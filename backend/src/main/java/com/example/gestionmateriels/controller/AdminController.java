package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ActifRequest;
import com.example.gestionmateriels.dto.AgentRequest;
import com.example.gestionmateriels.dto.IdentifiantRequest;
import com.example.gestionmateriels.dto.InscriptionDelegueRequest;
import com.example.gestionmateriels.dto.MatriculeRequest;
import com.example.gestionmateriels.dto.MotDePasseRequest;
import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.CompteService;
import com.example.gestionmateriels.service.PresenceService;
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
    private final PresenceService presenceService;

    public AdminController(CompteService compteService, PresenceService presenceService) {
        this.compteService = compteService;
        this.presenceService = presenceService;
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

    @PatchMapping("/agents/{id}/identifiant")
    public ResponseEntity<Map<String, Object>> identifiantAgent(@PathVariable Long id,
                                                                @RequestBody IdentifiantRequest requete) {
        return Reponses.ok("Identifiant modifié.", "agent", compteService.changerIdentifiantAgent(id, requete.identifiant()));
    }

    // ---------- Présences en ligne ----------

    @GetMapping("/presences")
    public Map<String, Object> presences() {
        return Map.of("success", true, "data", presenceService.enLigne());
    }

    // ---------- Délégués ----------

    @PostMapping("/delegues")
    public ResponseEntity<Map<String, Object>> creerDelegue(@RequestBody InscriptionDelegueRequest requete) {
        Delegue d = compteService.inscrireDelegue(requete.nom(), requete.filiereNiveau(), requete.matricule(),
                requete.identifiant(), requete.motDePasse(), requete.telephone());
        return Reponses.ok("Compte délégué créé.", "delegue", d);
    }

    @PatchMapping("/delegues/{id}/identifiant")
    public ResponseEntity<Map<String, Object>> identifiantDelegue(@PathVariable Long id,
                                                                  @RequestBody IdentifiantRequest requete) {
        return Reponses.ok("Identifiant modifié.", "delegue", compteService.changerIdentifiantDelegue(id, requete.identifiant()));
    }

    @PatchMapping("/delegues/{id}/matricule")
    public ResponseEntity<Map<String, Object>> matriculeDelegue(@PathVariable Long id,
                                                                @RequestBody MatriculeRequest requete) {
        return Reponses.ok("Matricule enregistré.", "delegue", compteService.changerMatricule(id, requete.matricule()));
    }

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

    @PatchMapping("/delegues/{id}/telephone")
    public ResponseEntity<Map<String, Object>> telephoneDelegue(@PathVariable Long id,
                                                                @RequestBody AuthController.TelephoneRequest requete) {
        Delegue d = compteService.changerTelephone(id, requete.telephone());
        return Reponses.ok("Numéro enregistré.", "delegue", d);
    }

    @PostMapping("/delegues/{id}/mot-de-passe")
    public ResponseEntity<Map<String, Object>> motDePasseDelegue(@PathVariable Long id,
                                                                 @RequestBody MotDePasseRequest requete) {
        compteService.reinitialiserMotDePasseDelegue(id, requete.motDePasse());
        return Reponses.ok("Mot de passe réinitialisé.");
    }
}
