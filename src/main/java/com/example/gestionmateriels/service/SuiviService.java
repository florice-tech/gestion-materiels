package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.EmpruntRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Suivi du matériel : parcours d'un équipement (qui l'a eu, quand, comment)
 * et occupation des créneaux d'une journée (calendrier des réservations).
 */
@Service
@Transactional(readOnly = true)
public class SuiviService {

    /** Une étape du parcours d'un matériel. */
    public record Etape(Long empruntId, String delegue, String filiereNiveau, String salle,
                        Emprunt.StatutEmprunt statut, Emprunt.Mode modeSortie, String remisPar,
                        String transmisPar, LocalDateTime dateDemande, LocalDateTime dateSortie,
                        LocalDateTime dateRetour, Emprunt.Mode modeRetour, Emprunt.EtatRetour etatRetour,
                        String remarque, LocalDate dateReservation, LocalTime heureDebut, LocalTime heureFin,
                        boolean enRetard) {
    }

    public record Creneau(LocalTime debut, LocalTime fin, String delegue, String salle, String type) {
    }

    public record Occupation(Materiel materiel, List<Creneau> creneaux) {
    }

    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final EmpruntRepository empruntRepository;

    public SuiviService(MaterielRepository materielRepository, DetailEmpruntRepository detailEmpruntRepository,
                        EmpruntRepository empruntRepository) {
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.empruntRepository = empruntRepository;
    }

    public List<Etape> parcours(Long materielId) {
        materielRepository.findById(materielId)
                .orElseThrow(() -> OperationException.introuvable("Matériel introuvable (id=" + materielId + ")."));
        List<Etape> etapes = new ArrayList<>();
        for (DetailEmprunt d : detailEmpruntRepository.parcoursMateriel(materielId)) {
            Emprunt e = d.getEmprunt();
            LocalDateTime retour = d.getDateRetour() != null ? d.getDateRetour()
                    : (e.getStatutEmprunt() == StatutEmprunt.RETOURNE ? e.getDateRetour() : null);
            etapes.add(new Etape(e.getId(), e.getDelegue().getNom(), e.getDelegue().getFiliereNiveau(), e.getSalle(),
                    e.getStatutEmprunt(), e.getMode(), e.getAgentSortie() != null ? e.getAgentSortie().getNom() : null,
                    e.getTransmisPar(), e.getDateDemande(), e.getDateSortie(), retour, d.getModeRetour(),
                    d.getEtatRetour(), d.getRemarqueRetour() != null ? d.getRemarqueRetour() : e.getObservations(),
                    e.getDateReservation(), e.getHeureDebut(), e.getHeureRetourPrevue(),
                    e.isEnRetard() && !d.isRendu()));
        }
        return etapes;
    }

    /** Occupation de chaque matériel durable pour un jour : réservations, et emprunts en cours si c'est aujourd'hui. */
    public List<Occupation> disponibilites(LocalDate jour) {
        LocalDate date = jour != null ? jour : LocalDate.now();
        List<Emprunt> reservations = empruntRepository
                .findByStatutEmpruntAndDateReservationOrderByHeureDebutAsc(StatutEmprunt.RESERVEE, date);
        List<Emprunt> enCours = date.equals(LocalDate.now())
                ? empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt.EN_COURS)
                : List.of();
        List<Emprunt> enAttente = date.equals(LocalDate.now())
                ? empruntRepository.findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt.EN_ATTENTE)
                : List.of();

        List<Occupation> resultat = new ArrayList<>();
        for (Materiel m : materielRepository.findByTypeGestionOrderByDesignationAsc(Materiel.TypeGestion.DURABLE)) {
            List<Creneau> creneaux = new ArrayList<>();
            for (Emprunt e : reservations) {
                if (contient(e, m, false)) {
                    creneaux.add(new Creneau(e.getHeureDebut(), e.getHeureRetourPrevue(),
                            e.getDelegue().getNom(), e.getSalle(), "RESERVATION"));
                }
            }
            for (Emprunt e : enCours) {
                if (contient(e, m, true)) {
                    creneaux.add(new Creneau(e.getDateSortie().toLocalTime(), e.getHeureRetourPrevue(),
                            e.getDelegue().getNom(), e.getSalle(), e.isEnRetard() ? "RETARD" : "EMPRUNT"));
                }
            }
            for (Emprunt e : enAttente) {
                if (contient(e, m, false)) {
                    creneaux.add(new Creneau(e.getDateDemande().toLocalTime(), e.getHeureRetourPrevue(),
                            e.getDelegue().getNom(), e.getSalle(), "DEMANDE"));
                }
            }
            creneaux.sort(Comparator.comparing(Creneau::debut));
            resultat.add(new Occupation(m, creneaux));
        }
        return resultat;
    }

    private static boolean contient(Emprunt e, Materiel m, boolean seulementEnMain) {
        return e.getDetails().stream().anyMatch(d -> d.getMateriel().getId().equals(m.getId())
                && (!seulementEnMain || !d.isRendu()));
    }
}
