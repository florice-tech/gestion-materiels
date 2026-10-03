package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Manque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManqueRepository extends JpaRepository<Manque, Long> {

    List<Manque> findAllByOrderByDateDerniereDemandeDesc();

    List<Manque> findByStatut(Manque.Statut statut);

    Optional<Manque> findFirstByStatutAndMaterielId(Manque.Statut statut, Long materielId);

    long countByStatut(Manque.Statut statut);
}
