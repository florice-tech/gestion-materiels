package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.EmpruntRequest;
import com.example.gestionmateriels.dto.RetourRequest;
import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.service.EmpruntService;
import com.example.gestionmateriels.service.OperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Expose les opérations d'emprunt et de retour de matériel via l'API REST.
 */
@RestController
@RequestMapping("/api/emprunts")
public class EmpruntController {

    private final EmpruntService empruntService;

    public EmpruntController(EmpruntService empruntService) {
        this.empruntService = empruntService;
    }

    /**
     * POST /api/emprunts  -> Créer un nouvel emprunt
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> creerEmprunt(@RequestBody EmpruntRequest requete) {
        try {
            Emprunt emprunt = empruntService.creerEmprunt(
                    requete.getDelegueNom(),
                    requete.getFiliereNiveau(),
                    requete.getSalle(),
                    requete.getAgentSortieId(),
                    requete.getHeureRetourPrevue(),
                    requete.getArticles()
            );

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Emprunt enregistré avec succès !");
            reponse.put("empruntId", emprunt.getId());
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    /**
     * POST /api/emprunts/retour  -> Enregistrer le retour d'un emprunt
     */
    @PostMapping("/retour")
    public ResponseEntity<Map<String, Object>> enregistrerRetour(@RequestBody RetourRequest requete) {
        try {
            empruntService.enregistrerRetour(
                    requete.getEmpruntId(),
                    requete.getAgentRetourId(),
                    requete.getEtatRetour(),
                    requete.getObservations()
            );

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Le retour du matériel a bien été enregistré.");
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    /**
     * GET /api/emprunts/actifs  -> Liste des emprunts en cours (non rendus)
     */
    @GetMapping("/actifs")
    public ResponseEntity<Map<String, Object>> listerEmpruntsActifs() {
        List<Emprunt> emprunts = empruntService.listerEmpruntsActifs();

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", emprunts);
        return ResponseEntity.ok(reponse);
    }

    /**
     * GET /api/emprunts/historique  -> Historique complet des emprunts
     */
    @GetMapping("/historique")
    public ResponseEntity<Map<String, Object>> listerHistorique() {
        List<Emprunt> emprunts = empruntService.listerHistorique();

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", emprunts);
        return ResponseEntity.ok(reponse);
    }

    // Construit une réponse d'erreur uniforme (HTTP 400)
    private ResponseEntity<Map<String, Object>> erreur(String message) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", false);
        reponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reponse);
    }
}