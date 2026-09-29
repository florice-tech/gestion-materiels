package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.NomRequest;
import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.service.CategorieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @GetMapping
    public List<Categorie> lister() {
        return categorieService.lister();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody NomRequest requete) {
        return Reponses.ok("Catégorie ajoutée.", "categorie", categorieService.creer(requete.nom()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> renommer(@PathVariable Long id, @RequestBody NomRequest requete) {
        return Reponses.ok("Catégorie renommée.", "categorie", categorieService.renommer(id, requete.nom()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        categorieService.supprimer(id);
        return Reponses.ok("Catégorie supprimée.");
    }
}
