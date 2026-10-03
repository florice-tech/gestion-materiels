package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.repository.DelegueRepository;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.EmpruntRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Score de confiance d'un délégué, calculé sur ses retours des 90 derniers jours :
 * part des retours à l'heure, moins 10 points par matériel rendu endommagé.
 * Un délégué fiable peut réserver plus loin à l'avance ; après 3 retards (ou avec un
 * matériel en retard en ce moment) ses réservations sont en pause jusqu'au déblocage par un agent.
 */
@Service
@Transactional(readOnly = true)
public class ConfianceService {

    public enum Niveau { NOUVEAU, FIABLE, BON, A_SURVEILLER }

    public record Confiance(Long delegueId, String nom, String filiereNiveau, String telephone, boolean actif,
                            Integer score, Niveau niveau, long retours, long aLHeure, long enRetard,
                            long endommages, boolean bloque, String motifBlocage,
                            int delaiReservationJours, LocalDateTime debloqueLe) {
    }

    static final int JOURS_PRIS_EN_COMPTE = 90;
    static final int RETARDS_AVANT_BLOCAGE = 3;
    static final int TOLERANCE_MINUTES = 10;

    private final DelegueRepository delegueRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final EmpruntRepository empruntRepository;

    public ConfianceService(DelegueRepository delegueRepository, DetailEmpruntRepository detailEmpruntRepository,
                            EmpruntRepository empruntRepository) {
        this.delegueRepository = delegueRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.empruntRepository = empruntRepository;
    }

    public Confiance evaluer(Long delegueId) {
        Delegue d = delegueRepository.findById(delegueId)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable."));
        return evaluer(d);
    }

    public List<Confiance> tous() {
        return delegueRepository.findAllByOrderByNomAsc().stream().map(this::evaluer)
                .sorted(Comparator.comparing((Confiance c) -> !c.bloque())
                        .thenComparing(c -> c.score() == null ? 101 : c.score()))
                .toList();
    }

    Confiance evaluer(Delegue delegue) {
        LocalDateTime depuis = LocalDateTime.now().minusDays(JOURS_PRIS_EN_COMPTE);
        List<DetailEmprunt> lignes = detailEmpruntRepository.lignesRenduesDuDelegue(delegue.getId(), depuis);

        long retours = lignes.size();
        long enRetard = lignes.stream().filter(ConfianceService::renduEnRetard).count();
        long endommages = lignes.stream().filter(l -> l.getEtatRetour() == Emprunt.EtatRetour.ENDOMMAGE).count();
        Integer score = null;
        if (retours > 0) {
            long brut = Math.round(100.0 * (retours - enRetard) / retours) - 10 * endommages;
            score = (int) Math.max(0, Math.min(100, brut));
        }

        // Retards qui comptent pour le blocage : ceux après le dernier déblocage par un agent
        LocalDateTime debloque = delegue.getReservationsDebloqueesLe();
        long retardsBloquants = lignes.stream()
                .filter(ConfianceService::renduEnRetard)
                .filter(l -> debloque == null || l.getDateRetour().isAfter(debloque))
                .count();
        boolean retardEnCours = empruntRepository.findByDelegueIdOrderByDateDemandeDesc(delegue.getId()).stream()
                .anyMatch(e -> e.getStatutEmprunt() == StatutEmprunt.EN_COURS && e.isEnRetard());

        String motif = null;
        if (retardEnCours) {
            motif = "Vous avez un matériel en retard : rapportez-le pour pouvoir réserver à nouveau.";
        } else if (retardsBloquants >= RETARDS_AVANT_BLOCAGE) {
            motif = retardsBloquants + " retours en retard ces derniers temps : passez au poste de surveillance pour débloquer vos réservations.";
        }

        Niveau niveau;
        if (score == null) niveau = Niveau.NOUVEAU;
        else if (score >= 90 && retours >= 3) niveau = Niveau.FIABLE;
        else if (score >= 70) niveau = Niveau.BON;
        else niveau = Niveau.A_SURVEILLER;

        int delai = switch (niveau) {
            case FIABLE -> 60;
            case NOUVEAU, BON -> 30;
            case A_SURVEILLER -> 7;
        };

        return new Confiance(delegue.getId(), delegue.getNom(), delegue.getFiliereNiveau(), delegue.getTelephone(),
                delegue.isActif(), score, niveau, retours, retours - enRetard, enRetard, endommages,
                motif != null, motif, delai, debloque);
    }

    /** Appelé avant chaque réservation. */
    public void verifierReservation(Delegue delegue, LocalDate jour) {
        Confiance c = evaluer(delegue);
        if (c.bloque()) {
            throw new OperationException("Réservations en pause. " + c.motifBlocage());
        }
        if (jour != null && jour.isAfter(LocalDate.now().plusDays(c.delaiReservationJours()))) {
            throw new OperationException("Vous pouvez réserver jusqu'à " + c.delaiReservationJours()
                    + " jours à l'avance" + (c.niveau() == Niveau.FIABLE ? "." : " (60 jours avec le badge « Fiable »)."));
        }
    }

    @Transactional
    public Confiance debloquer(Long delegueId) {
        Delegue d = delegueRepository.findById(delegueId)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable."));
        d.setReservationsDebloqueesLe(LocalDateTime.now());
        return evaluer(d);
    }

    static boolean renduEnRetard(DetailEmprunt ligne) {
        LocalDateTime echeance = ligne.getEmprunt().getEcheance();
        return echeance != null && ligne.getDateRetour() != null
                && ligne.getDateRetour().isAfter(echeance.plusMinutes(TOLERANCE_MINUTES));
    }
}
