package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.CategorieRequest;
import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.service.CategorieService;
import com.example.gestionmateriels.service.OperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieRepository categorieRepository;
    private final CategorieService categorieService;

    public CategorieController(CategorieRepository categorieRepository, CategorieService categorieService) {
        this.categorieRepository = categorieRepository;
        this.categorieService = categorieService;
    }

    @GetMapping
    public List<Categorie> lister() {
        return categorieRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody CategorieRequest requete) {
        try {
            Categorie categorie = categorieService.creerCategorie(requete.getNom());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Catégorie ajoutée.");
            reponse.put("categorie", categorie);
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        try {
            categorieService.supprimerCategorie(id);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Catégorie supprimée.");
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> erreur(String message) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", false);
        reponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reponse);
    }
}