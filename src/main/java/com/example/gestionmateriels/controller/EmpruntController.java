package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.AnnulationRequest;
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

@RestController
@RequestMapping("/api/emprunts")
public class EmpruntController {

    private final EmpruntService empruntService;

    public EmpruntController(EmpruntService empruntService) {
        this.empruntService = empruntService;
    }

    @PostMapping("/demande")
    public ResponseEntity<Map<String, Object>> demanderEmprunt(@RequestBody DemandeEmpruntRequest requete) {
        try {
            Emprunt emprunt = empruntService.demanderEmprunt(
                    requete.getDelegueId(), requete.getSalle(),
                    requete.getHeureRetourPrevue(), requete.getArticles()
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

    // POST /api/emprunts/{id}/refuser -> un agent refuse une demande en attente (le matériel est libéré)
    @PostMapping("/{id}/refuser")
    public ResponseEntity<Map<String, Object>> refuserDemande(@PathVariable Long id) {
        try {
            empruntService.annulerDemande(id, null);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Demande refusée, le matériel est de nouveau disponible.");
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    // POST /api/emprunts/{id}/annuler -> le délégué annule sa propre demande en attente
    @PostMapping("/{id}/annuler")
    public ResponseEntity<Map<String, Object>> annulerDemande(@PathVariable Long id,
                                                              @RequestBody AnnulationRequest requete) {
        try {
            if (requete.getDelegueId() == null) {
                throw new OperationException("Le délégué est obligatoire.");
            }
            empruntService.annulerDemande(id, requete.getDelegueId());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Votre demande a été annulée.");
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    @PostMapping("/retour")
    public ResponseEntity<Map<String, Object>> enregistrerRetour(@RequestBody RetourRequest requete) {
        try {
            empruntService.enregistrerRetour(
                    requete.getEmpruntId(), requete.getAgentRetourId(),
                    requete.getObservations(), requete.getDetails()
            );
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Le retour du matériel a bien été enregistré.");
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    @GetMapping("/en-attente")
    public ResponseEntity<Map<String, Object>> listerDemandesEnAttente() {
        List<Emprunt> demandes = empruntService.listerDemandesEnAttente();
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", demandes);
        return ResponseEntity.ok(reponse);
    }

    @GetMapping("/actifs")
    public ResponseEntity<Map<String, Object>> listerEmpruntsActifs() {
        List<Emprunt> emprunts = empruntService.listerEmpruntsActifs();
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", emprunts);
        return ResponseEntity.ok(reponse);
    }

    @GetMapping("/historique")
    public ResponseEntity<Map<String, Object>> listerHistorique() {
        List<Emprunt> emprunts = empruntService.listerHistorique();
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", true);
        reponse.put("data", emprunts);
        return ResponseEntity.ok(reponse);
    }

    // GET /api/emprunts/delegue/{id} -> demandes et emprunts d'un délégué ("Mes emprunts")
    @GetMapping("/delegue/{delegueId}")
    public ResponseEntity<Map<String, Object>> listerEmpruntsDelegue(@PathVariable Long delegueId) {
        List<Emprunt> emprunts = empruntService.listerEmpruntsDelegue(delegueId);
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