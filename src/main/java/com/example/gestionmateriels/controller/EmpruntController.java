package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.DemandeEmpruntRequest;
import com.example.gestionmateriels.dto.RetourRequest;
import com.example.gestionmateriels.dto.ValidationRequest;
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
     * POST /api/emprunts/demande -> Le délégué crée une demande d'emprunt
     */
    @PostMapping("/demande")
    public ResponseEntity<Map<String, Object>> demanderEmprunt(@RequestBody DemandeEmpruntRequest requete) {
        try {
            Emprunt emprunt = empruntService.demanderEmprunt(
                    requete.getDelegueId(),
                    requete.getSalle(),
                    requete.getHeureRetourPrevue(),
                    requete.getArticles()
            );

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Demande envoyée ! Présentez-vous au poste de surveillance pour récupérer le matériel.");
            reponse.put("empruntId", emprunt.getId());
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    /**
     * POST /api/emprunts/valider -> Un agent valide une demande et remet le matériel
     */
    @PostMapping("/valider")
    public ResponseEntity<Map<String, Object>> validerEmprunt(@RequestBody ValidationRequest requete) {
        try {
            empruntService.validerEmprunt(requete.getEmpruntId(), requete.getAgentSortieId());

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Emprunt validé, matériel remis au délégué.");
            return ResponseEntity.ok(reponse);

        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    /**
     * POST /api/emprunts/retour -> Enregistrer le retour d'un emprunt
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
     * GET /api/emprunts/en-attente -> Demandes pas encore validées par un agent
     */
    @GetMapping("/en-attente")
    public ResponseEntity<Map<String, Object>> listerDemandesEnAttente() {
        List<Emprunt> demandes = empruntService.listerDemandesEnAttente();

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", demandes);
        return ResponseEntity.ok(reponse);
    }

    /**
     * GET /api/emprunts/actifs -> Emprunts non encore rendus (en attente + en cours)
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
     * GET /api/emprunts/historique -> Historique complet des emprunts
     */
    @GetMapping("/historique")
    public ResponseEntity<Map<String, Object>> listerHistorique() {
        List<Emprunt> emprunts = empruntService.listerHistorique();

        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", emprunts);
        return ResponseEntity.ok(reponse);
    }

    private ResponseEntity<Map<String, Object>> erreur(String message) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", false);
        reponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reponse);
    }
}