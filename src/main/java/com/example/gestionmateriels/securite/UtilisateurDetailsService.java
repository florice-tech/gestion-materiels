package com.example.gestionmateriels.securite;

import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.DelegueRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Charge un utilisateur par son identifiant : d'abord parmi les agents, puis parmi les délégués.
 * Les identifiants sont uniques sur l'ensemble des deux tables (vérifié à la création).
 */
@Service
public class UtilisateurDetailsService implements UserDetailsService {

    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;

    public UtilisateurDetailsService(AgentRepository agentRepository, DelegueRepository delegueRepository) {
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        String id = identifiant == null ? "" : identifiant.trim();
        return agentRepository.findByIdentifiantIgnoreCase(id)
                .map(UtilisateurConnecte::depuis)
                .or(() -> delegueRepository.findByIdentifiantIgnoreCase(id).map(UtilisateurConnecte::depuis))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu"));
    }
}
