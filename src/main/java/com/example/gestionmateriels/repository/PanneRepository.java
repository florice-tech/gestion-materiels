package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Panne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanneRepository extends JpaRepository<Panne, Long> {

    List<Panne> findAllByOrderByDateDeclarationDesc();

    List<Panne> findByMaterielIdOrderByDateDeclarationDesc(Long materielId);

    Optional<Panne> findFirstByMaterielIdAndStatutInOrderByDateDeclarationDesc(Long materielId, List<Panne.Statut> statuts);

    long countByStatut(Panne.Statut statut);
}
