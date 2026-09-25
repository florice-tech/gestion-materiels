package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Emprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpruntRepository extends JpaRepository<Emprunt, Long> {

    // Emprunts en cours : pas encore de date de retour
    List<Emprunt> findByDateRetourIsNullOrderByDateSortieDesc();

    // Historique complet, du plus récent au plus ancien
    List<Emprunt> findAllByOrderByDateSortieDesc();

    // Recherche par nom de délégué (sans tenir compte des majuscules)
    List<Emprunt> findByDelegueNomContainingIgnoreCase(String nom);

    // Emprunts remis par un agent donné (traçabilité)
    List<Emprunt> findByAgentSortieId(Long agentId);
}