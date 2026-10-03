package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ScanRequest;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.ScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Scan du QR code d'un matériel (le code est celui imprimé sur l'étiquette). */
@RestController
@RequestMapping("/api/scan")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping("/{code}")
    public ScanService.Situation situation(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable String code) {
        return scanService.situation(code, moi);
    }

    @PostMapping("/{code}/recuperer")
    public ResponseEntity<Map<String, Object>> recuperer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                         @PathVariable String code, @RequestBody ScanRequest r) {
        var emprunt = scanService.recuperer(code, moi.getId(), r.salle(), r.heureRetourPrevue());
        return Reponses.ok("C'est noté : le matériel est sous votre responsabilité.", "empruntId", emprunt.getId());
    }

    @PostMapping("/{code}/retirer")
    public ResponseEntity<Map<String, Object>> retirer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                       @PathVariable String code) {
        var emprunt = scanService.retirer(code, moi.getId());
        return Reponses.ok("Matériel retiré : l'emprunt a commencé.", "empruntId", emprunt.getId());
    }

    @PostMapping("/{code}/rendre")
    public ResponseEntity<Map<String, Object>> rendre(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                      @PathVariable String code, @RequestBody ScanRequest r) {
        scanService.rendre(code, moi.getId(), Boolean.TRUE.equals(r.probleme()), r.remarque(), r.photo());
        return Reponses.ok(Boolean.TRUE.equals(r.probleme())
                ? "Retour enregistré. Le problème a été signalé aux agents."
                : "Retour enregistré. Merci !");
    }

    @PostMapping("/{code}/transfert")
    public ResponseEntity<Map<String, Object>> transfert(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                         @PathVariable String code, @RequestBody ScanRequest r) {
        var t = scanService.demanderTransfert(code, moi.getId(), r.salle(), r.heureRetourPrevue());
        return Reponses.ok("Demande envoyée à " + t.getDetenteur().getNom() + ". Vous serez prévenu de sa réponse.",
                "transfertId", t.getId());
    }

    @PostMapping("/{code}/fourniture")
    public ResponseEntity<Map<String, Object>> fourniture(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                          @PathVariable String code, @RequestBody ScanRequest r) {
        var emprunt = scanService.demanderFourniture(code, moi.getId(), r.salle(), r.quantite());
        return Reponses.ok("Demande envoyée au poste de surveillance : vous y retirerez votre fourniture.",
                "empruntId", emprunt.getId());
    }
}
