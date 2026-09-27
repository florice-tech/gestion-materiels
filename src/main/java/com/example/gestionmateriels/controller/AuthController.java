package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.InscriptionDelegueRequest;
import com.example.gestionmateriels.dto.LoginRequest;
import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.service.AuthService;
import com.example.gestionmateriels.service.OperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/agent/login
    @PostMapping("/agent/login")
    public ResponseEntity<Map<String, Object>> loginAgent(@RequestBody LoginRequest requete) {
        try {
            Agent agent = authService.connecterAgent(requete.getIdentifiant(), requete.getMotDePasse());

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("type", "AGENT");
            reponse.put("id", agent.getId());
            reponse.put("nom", agent.getNom());
            reponse.put("role", agent.getRole());
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    // POST /api/auth/delegue/login
    @PostMapping("/delegue/login")
    public ResponseEntity<Map<String, Object>> loginDelegue(@RequestBody LoginRequest requete) {
        try {
            Delegue delegue = authService.connecterDelegue(requete.getIdentifiant(), requete.getMotDePasse());

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("type", "DELEGUE");
            reponse.put("id", delegue.getId());
            reponse.put("nom", delegue.getNom());
            reponse.put("filiereNiveau", delegue.getFiliereNiveau());
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    // POST /api/auth/delegue/inscription
    @PostMapping("/delegue/inscription")
    public ResponseEntity<Map<String, Object>> inscrireDelegue(@RequestBody InscriptionDelegueRequest requete) {
        try {
            Delegue delegue = authService.inscrireDelegue(
                    requete.getNom(), requete.getFiliereNiveau(),
                    requete.getIdentifiant(), requete.getMotDePasse()
            );

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Compte créé avec succès. Vous pouvez maintenant vous connecter.");
            reponse.put("id", delegue.getId());
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> erreur(String message) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", false);
        reponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reponse);
    }
}