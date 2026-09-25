package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    // Retrouver un agent par son nom
    Optional<Agent> findByNom(String nom);
}