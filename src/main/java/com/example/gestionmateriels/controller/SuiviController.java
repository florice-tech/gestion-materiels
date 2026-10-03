package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.service.SuiviService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parcours d'un matériel, occupation des créneaux, réglages utiles aux pages. */
@RestController
@RequestMapping("/api")
public class SuiviController {

    private final SuiviService suiviService;
    private final String urlPublique;

    public SuiviController(SuiviService suiviService, @Value("${app.url-publique:}") String urlPublique) {
        this.suiviService = suiviService;
        this.urlPublique = urlPublique;
    }

    // Agents : qui a eu ce matériel, quand, et comment il est passé de main en main
    @GetMapping("/materiels/{id}/parcours")
    public List<SuiviService.Etape> parcours(@PathVariable Long id) {
        return suiviService.parcours(id);
    }

    // Tous : créneaux occupés pour un jour (calendrier de réservation)
    @GetMapping("/disponibilites")
    public List<SuiviService.Occupation> disponibilites(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate jour) {
        return suiviService.disponibilites(jour);
    }

    // Tous : QR code de la porte d'une salle → matériel présent, réservations du jour
    @GetMapping("/salles/{id}/situation")
    public SuiviService.SituationSalle situationSalle(@PathVariable Long id) {
        return suiviService.situationSalle(id);
    }

    // Adresse à mettre dans les QR codes (sinon l'adresse affichée dans le navigateur)
    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("urlPublique", urlPublique == null ? "" : urlPublique.replaceAll("/+$", ""));
        return corps;
    }
}
