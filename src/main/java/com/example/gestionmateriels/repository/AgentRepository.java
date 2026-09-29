package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByIdentifiantIgnoreCase(String identifiant);

    boolean existsByIdentifiantIgnoreCase(String identifiant);

    List<Agent> findAllByOrderByNomAsc();

    // Nombre d'administrateurs encore actifs (on ne doit jamais tomber à zéro)
    long countByAdministrateurTrueAndActifTrue();
}
