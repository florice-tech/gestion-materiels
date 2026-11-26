package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.model.Panne;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.PanneService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Matériel gâté (agents) : déclarer, réparer, mettre hors service, vérifier. */
@RestController
@RequestMapping("/api")
public class PanneController {

    public record DeclarationRequest(Long materielId, String description) {
    }

    public record ReparationRequest(String note, Integer cout) {
    }

    private final PanneService panneService;

    public PanneController(PanneService panneService) {
        this.panneService = panneService;
    }

    @GetMapping("/pannes")
    public List<Panne> lister() {
        return panneService.lister();
    }

    @GetMapping("/materiels/{id}/pannes")
    public List<Panne> duMateriel(@PathVariable Long id) {
        return panneService.historiqueDuMateriel(id);
    }

    @PostMapping("/pannes")
    public ResponseEntity<Map<String, Object>> declarer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                        @RequestBody DeclarationRequest r) {
        Panne p = panneService.declarer(r.materielId(), r.description(), Panne.Origine.SCAN, moi.getNom());
        return Reponses.ok("« " + p.getMateriel().getDesignation() + " » est enregistré comme gâté.", "panneId", p.getId());
    }

    @PostMapping("/pannes/{id}/reparer")
    public ResponseEntity<Map<String, Object>> reparer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable Long id, @RequestBody(required = false) ReparationRequest r) {
        Panne p = panneService.reparer(id, r != null ? r.note() : null, r != null ? r.cout() : null, moi.getNom());
        return Reponses.ok("« " + p.getMateriel().getDesignation() + " » est réparé et de nouveau disponible.");
    }

    @PostMapping("/materiels/{id}/reparer")
    public ResponseEntity<Map<String, Object>> reparerMateriel(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                               @PathVariable Long id, @RequestBody(required = false) ReparationRequest r) {
        Panne p = panneService.reparerMateriel(id, r != null ? r.note() : null, r != null ? r.cout() : null, moi.getNom());
        return Reponses.ok("« " + p.getMateriel().getDesignation() + " » est réparé et de nouveau disponible.");
    }

    @PostMapping("/pannes/{id}/hors-service")
    public ResponseEntity<Map<String, Object>> horsService(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                           @PathVariable Long id, @RequestBody(required = false) ReparationRequest r) {
        Panne p = panneService.mettreHorsService(id, r != null ? r.note() : null, moi.getNom());
        return Reponses.ok("« " + p.getMateriel().getDesignation() + " » est mis hors service.");
    }

    @PostMapping("/materiels/{id}/verifie")
    public ResponseEntity<Map<String, Object>> verifie(@PathVariable Long id) {
        var m = panneService.verifierBonEtat(id);
        return Reponses.ok("« " + m.getDesignation() + " » est vérifié et de nouveau disponible.");
    }
}
