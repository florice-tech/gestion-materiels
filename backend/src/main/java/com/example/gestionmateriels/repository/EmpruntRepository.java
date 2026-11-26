package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmpruntRepository extends JpaRepository<Emprunt, Long> {

    // Historique complet, du plus récent au plus ancien
    List<Emprunt> findAllByOrderByDateDemandeDesc();

    // Demandes à traiter, la plus ancienne d'abord
    List<Emprunt> findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt statut);

    // Emprunts en cours, les plus anciens d'abord
    List<Emprunt> findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt statut);

    // Demandes et emprunts d'un délégué
    List<Emprunt> findByDelegueIdOrderByDateDemandeDesc(Long delegueId);

    long countByStatutEmprunt(StatutEmprunt statut);

    // Pour les statistiques : sorties effectives depuis une date
    List<Emprunt> findByDateSortieGreaterThanEqual(LocalDateTime depuis);

    long countByDateRetourGreaterThanEqual(LocalDateTime depuis);

    // Fil d'activité : demandes déposées depuis une date
    List<Emprunt> findByDateDemandeGreaterThanEqualOrderByDateDemandeDesc(LocalDateTime depuis);

    // Réservations d'un jour donné (calendrier, réservations du jour)
    List<Emprunt> findByStatutEmpruntAndDateReservationOrderByHeureDebutAsc(StatutEmprunt statut, LocalDate jour);

    // Réservations non retirées dont la date est passée
    List<Emprunt> findByStatutEmpruntAndDateReservationBefore(StatutEmprunt statut, LocalDate jour);

    // Emprunts en cours pas encore signalés en retard
    List<Emprunt> findByStatutEmpruntAndRappelRetardEnvoyeFalse(StatutEmprunt statut);
}
