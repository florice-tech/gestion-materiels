package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Map<String, Object> lister(@AuthenticationPrincipal UtilisateurConnecte moi) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", true);
        corps.put("nonLues", notificationService.nombreNonLues(moi));
        corps.put("data", notificationService.mesNotifications(moi));
        return corps;
    }

    @PostMapping("/{id}/lue")
    public ResponseEntity<Map<String, Object>> lue(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        notificationService.marquerLue(id, moi);
        return Reponses.ok("Notification lue.");
    }

    @PostMapping("/tout-lu")
    public ResponseEntity<Map<String, Object>> toutLu(@AuthenticationPrincipal UtilisateurConnecte moi) {
        notificationService.toutMarquerLu(moi);
        return Reponses.ok("Toutes les notifications sont lues.");
    }
}
