package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterielRepository extends JpaRepository<Materiel, Long> {

    List<Materiel> findAllByOrderByDesignationAsc();

    boolean existsByCategorie(Categorie categorie);

    Optional<Materiel> findByCodeUniqueIgnoreCase(String codeUnique);

    long countByTypeGestion(TypeGestion typeGestion);

    long countByTypeGestionAndStatut(TypeGestion typeGestion, StatutMateriel statut);

    List<Materiel> findByTypeGestionOrderByDesignationAsc(TypeGestion typeGestion);
}
