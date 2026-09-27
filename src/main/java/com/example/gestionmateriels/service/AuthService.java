package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.DelegueRepository;
import org.springframework.stereotype.Service;

/**
 * Vérifie les identifiants de connexion des agents et des délégués,
 * et gère l'inscription des délégués.
 */
@Service
public class AuthService {

    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;

    public AuthService(AgentRepository agentRepository, DelegueRepository delegueRepository) {
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
    }

    public Agent connecterAgent(String identifiant, String motDePasse) {
        Agent agent = agentRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new OperationException("Identifiant ou mot de passe incorrect."));

        if (!agent.getMotDePasse().equals(motDePasse)) {
            throw new OperationException("Identifiant ou mot de passe incorrect.");
        }
        return agent;
    }

    public Delegue connecterDelegue(String identifiant, String motDePasse) {
        Delegue delegue = delegueRepository.findByIdentifiant(identifiant)
                .orElseThrow(() -> new OperationException("Identifiant ou mot de passe incorrect."));

        if (!delegue.getMotDePasse().equals(motDePasse)) {
            throw new OperationException("Identifiant ou mot de passe incorrect.");
        }
        return delegue;
    }

    public Delegue inscrireDelegue(String nom, String filiereNiveau, String identifiant, String motDePasse) {
        if (nom == null || nom.isBlank()) {
            throw new OperationException("Le nom est obligatoire.");
        }
        if (filiereNiveau == null || filiereNiveau.isBlank()) {
            throw new OperationException("La filière/niveau est obligatoire.");
        }
        if (identifiant == null || identifiant.isBlank()) {
            throw new OperationException("L'identifiant est obligatoire.");
        }
        if (motDePasse == null || motDePasse.length() < 4) {
            throw new OperationException("Le mot de passe doit contenir au moins 4 caractères.");
        }
        if (delegueRepository.findByIdentifiant(identifiant).isPresent()) {
            throw new OperationException("Cet identifiant est déjà utilisé.");
        }

        return delegueRepository.save(new Delegue(nom, filiereNiveau, identifiant, motDePasse));
    }
}