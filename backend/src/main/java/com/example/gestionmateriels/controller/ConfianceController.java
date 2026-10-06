package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.ConfianceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Score de confiance des délégués. */
@RestController
@RequestMapping("/api/confiance")
public class ConfianceController {

    private final ConfianceService confianceService;

    public ConfianceController(ConfianceService confianceService) {
        this.confianceService = confianceService;
    }

    /** Le délégué connecté voit son propre score. */
    @GetMapping("/moi")
    public ResponseEntity<?> moi(@AuthenticationPrincipal UtilisateurConnecte moi) {
        if (!moi.estDelegue()) {
            return Reponses.negatif("Réservé aux délégués.");
        }
        return ResponseEntity.ok(confianceService.evaluer(moi.getId()));
    }

    /** Agents : tous les délégués, les bloqués et les moins fiables d'abord. */
    @GetMapping
    public List<ConfianceService.Confiance> tous() {
        return confianceService.tous();
    }

    @PostMapping("/{delegueId}/debloquer")
    public ResponseEntity<Map<String, Object>> debloquer(@PathVariable Long delegueId) {
        var c = confianceService.debloquer(delegueId);
        return Reponses.ok("Réservations débloquées pour " + c.nom() + ".", "confiance", c);
    }
}
