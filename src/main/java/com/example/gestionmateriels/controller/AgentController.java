package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.repository.AgentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expose la liste des agents en lecture seule (pour le menu "agent connecté").
 */
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentRepository agentRepository;

    public AgentController(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    // GET /api/agents -> liste complète des agents
    @GetMapping
    public List<Agent> lister() {
        return agentRepository.findAll();
    }
}