package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.Categorie;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaterielRepository extends JpaRepository<Materiel, Long> {

    // Matériel par statut (ex : tout ce qui est DISPONIBLE ou A_VERIFIER)
    List<Materiel> findByStatut(StatutMateriel statut);

    // Matériel par catégorie (ex : AUDIOVISUEL)
    List<Materiel> findByCategorie(Categorie categorie);

    // Durable ou consommable
    List<Materiel> findByTypeGestion(TypeGestion typeGestion);

    // Retrouver un équipement par son code unique (scan / saisie)
    Optional<Materiel> findByCodeUnique(String codeUnique);
}