package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.ManqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Matériel manquant : les délégués et les agents signalent, les agents règlent. */
@RestController
@RequestMapping("/api/manques")
public class ManqueController {

    public record SignalementRequest(Long materielId, String designation, Long categorieId, Integer quantite,
                                     String salle, String commentaire) {
    }

    public record NoteRequest(String note) {
    }

    private final ManqueService manqueService;

    public ManqueController(ManqueService manqueService) {
        this.manqueService = manqueService;
    }

    @GetMapping
    public List<ManqueService.Vue> lister() {
        return manqueService.lister();
    }

    @GetMapping("/mes-signalements")
    public List<ManqueService.MonSignalement> mesSignalements(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return manqueService.mesSignalements(moi.getId());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> signaler(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                        @RequestBody SignalementRequest r) {
        var resultat = manqueService.signaler(r.materielId(), r.designation(), r.categorieId(), r.quantite(),
                r.salle(), r.commentaire(), moi);
        return Reponses.ok(resultat.getValue(), "manqueId", resultat.getKey().getId());
    }

    @PostMapping("/{id}/resoudre")
    public ResponseEntity<Map<String, Object>> resoudre(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                        @PathVariable Long id, @RequestBody(required = false) NoteRequest r) {
        var m = manqueService.resoudre(id, r != null ? r.note() : null, moi.getNom());
        return Reponses.ok("« " + m.getDesignation() + " » est réglé. Les délégués qui l'avaient demandé sont prévenus.");
    }

    @PostMapping("/{id}/abandonner")
    public ResponseEntity<Map<String, Object>> abandonner(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                          @PathVariable Long id, @RequestBody(required = false) NoteRequest r) {
        var m = manqueService.abandonner(id, r != null ? r.note() : null, moi.getNom());
        return Reponses.ok("« " + m.getDesignation() + " » est retiré de la liste. Les délégués sont prévenus.");
    }

    @PostMapping("/{id}/rouvrir")
    public ResponseEntity<Map<String, Object>> rouvrir(@PathVariable Long id) {
        var m = manqueService.rouvrir(id);
        return Reponses.ok("« " + m.getDesignation() + " » est de nouveau dans le matériel manquant.");
    }
}
