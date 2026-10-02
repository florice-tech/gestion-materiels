package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Transfert;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.DelegueRepository;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.TransfertRepository;
import com.example.gestionmateriels.repository.EmpruntRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Chiffres et listes du tableau de bord des agents (et de l'administrateur). */
@Service
@Transactional(readOnly = true)
public class StatistiqueService {

    public record Compteurs(long demandesEnAttente, long empruntsEnCours, long empruntsEnRetard,
                            long sortiesAujourdhui, long retoursAujourdhui, long reservationsAujourdhui,
                            long transfertsEnAttente, long materielsSortis) {
    }

    /** Un matériel durable actuellement entre les mains d'un délégué. */
    public record Sortie(Long materielId, String designation, String code, String delegue, String filiereNiveau,
                         String salle, LocalDateTime depuis, LocalTime retourPrevu, boolean enRetard,
                         Emprunt.Mode mode, Long empruntId) {
    }

    /** Une ligne du fil d'activité. type : DEMANDE, RESERVATION, SORTIE, SCAN, TRANSFERT, RETOUR, PROBLEME. */
    public record Evenement(String type, String texte, LocalDateTime quand, String lien) {
    }

    /** Chiffres réservés à l'administrateur. */
    public record Administration(long agentsActifs, long deleguesActifs, long deleguesInscrits,
                                 long materielsDurables, long fournitures, int tauxUtilisation) {
    }

    public record EtatParc(long total, long disponibles, long empruntes, long aVerifier,
                           long maintenance, long horsService) {
    }

    public record MaterielPopulaire(String designation, long nombreEmprunts, long quantiteTotale) {
    }

    public record ActiviteJour(LocalDate jour, long sorties) {
    }

    public record TableauDeBord(Compteurs compteurs, EtatParc parcDurable, List<Materiel> stocksBas,
                                List<Emprunt> retards, List<MaterielPopulaire> materielsPopulaires,
                                List<ActiviteJour> activite7Jours, List<Sortie> enCirculation,
                                List<Emprunt> demandesATraiter, List<Emprunt> reservationsDuJour,
                                List<Evenement> activiteRecente, Administration administration) {
    }

    private static final int JOURS_ACTIVITE = 7;

    private final EmpruntRepository empruntRepository;
    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final TransfertRepository transfertRepository;
    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;

    public StatistiqueService(EmpruntRepository empruntRepository, MaterielRepository materielRepository,
                              DetailEmpruntRepository detailEmpruntRepository, TransfertRepository transfertRepository,
                              AgentRepository agentRepository, DelegueRepository delegueRepository) {
        this.empruntRepository = empruntRepository;
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.transfertRepository = transfertRepository;
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
    }

    /** @param administrateur les chiffres d'administration ne sont calculés que pour un administrateur */
    public TableauDeBord tableauDeBord(boolean administrateur) {
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();

        List<Emprunt> enCours = empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt.EN_COURS);
        List<Emprunt> retards = enCours.stream().filter(Emprunt::isEnRetard).toList();

        List<Emprunt> sortiesRecentes = empruntRepository.findByDateSortieGreaterThanEqual(
                debutJournee.minusDays(JOURS_ACTIVITE - 1));

        List<DetailEmprunt> lignesEnMain = detailEmpruntRepository.toutesLignesEnMain();
        List<Sortie> enCirculation = lignesEnMain.stream().map(StatistiqueService::sortie).toList();
        List<Emprunt> demandes = empruntRepository.findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt.EN_ATTENTE);
        List<Emprunt> reservationsDuJour = empruntRepository
                .findByStatutEmpruntAndDateReservationOrderByHeureDebutAsc(StatutEmprunt.RESERVEE, LocalDate.now());

        Compteurs compteurs = new Compteurs(
                demandes.size(),
                enCours.size(),
                retards.size(),
                sortiesRecentes.stream().filter(e -> !e.getDateSortie().isBefore(debutJournee)).count(),
                empruntRepository.countByDateRetourGreaterThanEqual(debutJournee),
                reservationsDuJour.size(),
                transfertRepository.countByStatut(Transfert.Statut.EN_ATTENTE),
                enCirculation.size());

        EtatParc parc = new EtatParc(
                materielRepository.countByTypeGestion(TypeGestion.DURABLE),
                compterDurables(StatutMateriel.DISPONIBLE),
                compterDurables(StatutMateriel.EMPRUNTE),
                compterDurables(StatutMateriel.A_VERIFIER),
                compterDurables(StatutMateriel.MAINTENANCE),
                compterDurables(StatutMateriel.HS));

        List<Materiel> stocksBas = materielRepository
                .findByTypeGestionOrderByDesignationAsc(TypeGestion.CONSOMMABLE)
                .stream().filter(Materiel::isStockBas).toList();

        List<MaterielPopulaire> populaires = new ArrayList<>();
        for (Object[] ligne : detailEmpruntRepository.classementMateriels(
                List.of(StatutEmprunt.EN_COURS, StatutEmprunt.RETOURNE), PageRequest.of(0, 5))) {
            populaires.add(new MaterielPopulaire((String) ligne[0],
                    ((Number) ligne[1]).longValue(), ((Number) ligne[2]).longValue()));
        }

        // Sorties par jour sur les 7 derniers jours (jours sans sortie compris)
        Map<LocalDate, Long> parJour = new LinkedHashMap<>();
        for (int i = JOURS_ACTIVITE - 1; i >= 0; i--) {
            parJour.put(LocalDate.now().minusDays(i), 0L);
        }
        for (Emprunt e : sortiesRecentes) {
            parJour.computeIfPresent(e.getDateSortie().toLocalDate(), (jour, n) -> n + 1);
        }
        List<ActiviteJour> activite = parJour.entrySet().stream()
                .map(entree -> new ActiviteJour(entree.getKey(), entree.getValue()))
                .toList();

        Administration administration = null;
        if (administrateur) {
            long fournitures = materielRepository.countByTypeGestion(TypeGestion.CONSOMMABLE);
            int taux = parc.total() == 0 ? 0 : (int) Math.round(enCirculation.size() * 100.0 / parc.total());
            administration = new Administration(agentRepository.countByActifTrue(), delegueRepository.countByActifTrue(),
                    delegueRepository.count(), parc.total(), fournitures, taux);
        }

        return new TableauDeBord(compteurs, parc, stocksBas, retards, populaires, activite, enCirculation,
                demandes.stream().limit(5).toList(), reservationsDuJour,
                activiteRecente(debutJournee.minusDays(2)), administration);
    }

    /** Fil des derniers mouvements : demandes, réservations, sorties (poste, scan, transfert) et retours. */
    private List<Evenement> activiteRecente(LocalDateTime depuis) {
        List<Evenement> fil = new ArrayList<>();
        for (Emprunt e : empruntRepository.findByDateDemandeGreaterThanEqualOrderByDateDemandeDesc(depuis)) {
            String qui = e.getDelegue().getNom();
            if (e.getMode() == Emprunt.Mode.RESERVATION) {
                fil.add(new Evenement("RESERVATION", qui + " a réservé " + resume(e) + " pour le "
                        + e.getDateReservation().format(EmpruntService.JOUR) + " à "
                        + e.getHeureDebut().format(EmpruntService.HEURE) + ".", e.getDateDemande(), "emprunt.html"));
            } else if (e.getMode() == Emprunt.Mode.DEMANDE) {
                fil.add(new Evenement("DEMANDE", qui + " a demandé " + resume(e) + " (salle " + e.getSalle() + ").",
                        e.getDateDemande(), "emprunt.html"));
            }
        }
        for (Emprunt e : empruntRepository.findByDateSortieGreaterThanEqual(depuis)) {
            String qui = e.getDelegue().getNom();
            switch (e.getMode()) {
                case SCAN -> fil.add(new Evenement("SCAN", qui + " a récupéré " + resume(e) + " en scannant le QR code.",
                        e.getDateSortie(), "retour.html"));
                case TRANSFERT -> fil.add(new Evenement("TRANSFERT", (e.getTransmisPar() != null ? e.getTransmisPar() : "Un délégué")
                        + " a passé " + resume(e) + " à " + qui + ".", e.getDateSortie(), "retour.html"));
                default -> fil.add(new Evenement("SORTIE", resume(e) + " remis à " + qui
                        + (e.getAgentSortie() != null ? " par " + e.getAgentSortie().getNom() : "") + ".",
                        e.getDateSortie(), "retour.html"));
            }
        }
        for (DetailEmprunt d : detailEmpruntRepository.findByDateRetourGreaterThanEqualOrderByDateRetourDesc(depuis)) {
            if (d.getModeRetour() == Emprunt.Mode.TRANSFERT) {
                continue; // déjà raconté par la sortie du transfert
            }
            boolean probleme = d.getEtatRetour() != null && d.getEtatRetour() != Emprunt.EtatRetour.BON_ETAT;
            fil.add(new Evenement(probleme ? "PROBLEME" : "RETOUR", d.getEmprunt().getDelegue().getNom() + " a rendu "
                    + d.getMateriel().getDesignation() + (probleme ? " avec un problème signalé" : "")
                    + (d.getModeRetour() == Emprunt.Mode.SCAN ? " (scan)." : " au poste."),
                    d.getDateRetour(), probleme ? "fiche-materiel.html?id=" + d.getMateriel().getId() : "historique.html"));
        }
        fil.sort(Comparator.comparing(Evenement::quand).reversed());
        return fil.stream().limit(12).toList();
    }

    private static String resume(Emprunt e) {
        List<String> noms = e.getDetails().stream().map(d -> d.getMateriel().getDesignation()
                + (d.getMateriel().getTypeGestion() == TypeGestion.CONSOMMABLE ? " × " + d.getQuantite() : "")).toList();
        if (noms.isEmpty()) {
            return "du matériel";
        }
        return noms.size() <= 2 ? String.join(" et ", noms) : noms.get(0) + ", " + noms.get(1) + " et " + (noms.size() - 2) + (noms.size() == 3 ? " autre" : " autres");
    }

    private static Sortie sortie(DetailEmprunt d) {
        Emprunt e = d.getEmprunt();
        Materiel m = d.getMateriel();
        return new Sortie(m.getId(), m.getDesignation(), m.getCodeUnique(), e.getDelegue().getNom(),
                e.getDelegue().getFiliereNiveau(), e.getSalle(), e.getDateSortie(), e.getHeureRetourPrevue(),
                e.isEnRetard(), e.getMode(), e.getId());
    }

    private long compterDurables(StatutMateriel statut) {
        return materielRepository.countByTypeGestionAndStatut(TypeGestion.DURABLE, statut);
    }
}
