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
    private final com.example.gestionmateriels.service.AffluenceService affluenceService;

    public StatistiqueController(StatistiqueService statistiqueService,
                                 com.example.gestionmateriels.service.AffluenceService affluenceService) {
        this.statistiqueService = statistiqueService;
        this.affluenceService = affluenceService;
    }

    /** Administrateur : heures chargées et équipements les plus tendus. */
    @GetMapping("/affluence")
    public com.example.gestionmateriels.service.AffluenceService.Affluence affluence(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "8") int semaines,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long categorieId) {
        return affluenceService.calculer(semaines, categorieId);
    }

    @GetMapping
    public StatistiqueService.TableauDeBord tableauDeBord(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return statistiqueService.tableauDeBord(moi != null && moi.isAdministrateur());
    }
}
