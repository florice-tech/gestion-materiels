package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ActifRequest;
import com.example.gestionmateriels.dto.AgentRequest;
import com.example.gestionmateriels.dto.MotDePasseRequest;
import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.CompteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Gestion des comptes, réservée aux administrateurs (voir SecuriteConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CompteService compteService;

    public AdminController(CompteService compteService) {
        this.compteService = compteService;
    }

    // ---------- Agents ----------

    @GetMapping("/agents")
    public List<Agent> agents() {
        return compteService.listerAgents();
    }

    @PostMapping("/agents")
    public ResponseEntity<Map<String, Object>> creerAgent(@RequestBody AgentRequest requete) {
        return Reponses.ok("Compte agent créé.", "agent", compteService.creerAgent(requete));
    }

    @PutMapping("/agents/{id}")
    public ResponseEntity<Map<String, Object>> modifierAgent(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                             @PathVariable Long id,
                                                             @RequestBody AgentRequest requete) {
        return Reponses.ok("Compte agent modifié.", "agent", compteService.modifierAgent(id, requete, moi));
    }

    @PatchMapping("/agents/{id}/actif")
    public ResponseEntity<Map<String, Object>> activerAgent(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                            @PathVariable Long id,
                                                            @RequestBody ActifRequest requete) {
        Agent agent = compteService.changerActifAgent(id, requete.actif(), moi);
        return Reponses.ok(agent.isActif() ? "Compte réactivé." : "Compte désactivé.", "agent", agent);
    }

    @PostMapping("/agents/{id}/mot-de-passe")
    public ResponseEntity<Map<String, Object>> motDePasseAgent(@PathVariable Long id,
                                                               @RequestBody MotDePasseRequest requete) {
        compteService.reinitialiserMotDePasseAgent(id, requete.motDePasse());
        return Reponses.ok("Mot de passe réinitialisé.");
    }

    // ---------- Délégués ----------

    @GetMapping("/delegues")
    public List<Delegue> delegues() {
        return compteService.listerDelegues();
    }

    @PatchMapping("/delegues/{id}/actif")
    public ResponseEntity<Map<String, Object>> activerDelegue(@PathVariable Long id,
                                                              @RequestBody ActifRequest requete) {
        Delegue delegue = compteService.changerActifDelegue(id, requete.actif());
        return Reponses.ok(delegue.isActif() ? "Compte réactivé." : "Compte désactivé.", "delegue", delegue);
    }

    @PostMapping("/delegues/{id}/mot-de-passe")
    public ResponseEntity<Map<String, Object>> motDePasseDelegue(@PathVariable Long id,
                                                                 @RequestBody MotDePasseRequest requete) {
        compteService.reinitialiserMotDePasseDelegue(id, requete.motDePasse());
        return Reponses.ok("Mot de passe réinitialisé.");
    }
}
