package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Delegue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DelegueRepository extends JpaRepository<Delegue, Long> {
    Optional<Delegue> findByIdentifiant(String identifiant);
}