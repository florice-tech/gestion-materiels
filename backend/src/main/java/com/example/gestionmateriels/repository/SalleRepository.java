package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Salle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {

    Optional<Salle> findByNomIgnoreCase(String nom);

    List<Salle> findAllByOrderByNomAsc();
}
