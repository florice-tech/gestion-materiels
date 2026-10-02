package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.ScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Transferts de matériel entre délégués (côté délégué). */
@RestController
@RequestMapping("/api/transferts")
public class TransfertController {

    private final ScanService scanService;

    public TransfertController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping
    public Map<String, Object> mesTransferts(@AuthenticationPrincipal UtilisateurConnecte moi) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", true);
        corps.put("recus", scanService.transfertsRecus(moi.getId()));
        corps.put("envoyes", scanService.transfertsEnvoyes(moi.getId()));
        return corps;
    }

    @PostMapping("/{id}/accepter")
    public ResponseEntity<Map<String, Object>> accepter(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        var t = scanService.accepterTransfert(id, moi.getId());
        return Reponses.ok("Transfert accepté : " + t.getMateriel().getDesignation() + " est maintenant sous la responsabilité de "
                + t.getDemandeur().getNom() + ".");
    }

    @PostMapping("/{id}/refuser")
    public ResponseEntity<Map<String, Object>> refuser(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        scanService.refuserTransfert(id, moi.getId());
        return Reponses.ok("Transfert refusé.");
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Map<String, Object>> annuler(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        scanService.annulerTransfert(id, moi.getId());
        return Reponses.ok("Demande de transfert annulée.");
    }
}
