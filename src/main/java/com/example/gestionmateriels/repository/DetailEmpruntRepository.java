package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.DetailEmprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailEmpruntRepository extends JpaRepository<DetailEmprunt, Long> {

    // Toutes les lignes (matériels) d'un emprunt donné
    List<DetailEmprunt> findByEmpruntId(Long empruntId);

    // Historique d'un matériel : dans quels emprunts il est apparu
    List<DetailEmprunt> findByMaterielId(Long materielId);
}