package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.StatistiqueService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord des agents. */
@RestController
@RequestMapping("/api/statistiques")
public class StatistiqueController {

    private final StatistiqueService statistiqueService;

    public StatistiqueController(StatistiqueService statistiqueService) {
        this.statistiqueService = statistiqueService;
    }

    @GetMapping
    public StatistiqueService.TableauDeBord tableauDeBord(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return statistiqueService.tableauDeBord(moi != null && moi.isAdministrateur());
    }
}
