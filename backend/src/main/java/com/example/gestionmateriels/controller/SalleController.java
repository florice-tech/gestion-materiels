package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.NomRequest;
import com.example.gestionmateriels.model.Salle;
import com.example.gestionmateriels.service.SalleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salles")
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @GetMapping
    public List<Salle> lister() {
        return salleService.lister();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody NomRequest requete) {
        return Reponses.ok("Salle ajoutée.", "salle", salleService.creer(requete.nom()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> renommer(@PathVariable Long id, @RequestBody NomRequest requete) {
        return Reponses.ok("Salle renommée.", "salle", salleService.renommer(id, requete.nom()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        salleService.supprimer(id);
        return Reponses.ok("Salle supprimée.");
    }
}
