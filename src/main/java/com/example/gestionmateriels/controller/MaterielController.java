package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ChangerStatutMaterielRequest;
import com.example.gestionmateriels.dto.MaterielRequest;
import com.example.gestionmateriels.dto.ReapprovisionnerRequest;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.repository.MaterielRepository;
import com.example.gestionmateriels.service.MaterielService;
import com.example.gestionmateriels.service.OperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materiels")
public class MaterielController {

    private final MaterielRepository materielRepository;
    private final MaterielService materielService;

    public MaterielController(MaterielRepository materielRepository, MaterielService materielService) {
        this.materielRepository = materielRepository;
        this.materielService = materielService;
    }

    @GetMapping
    public List<Materiel> lister() {
        return materielRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody MaterielRequest requete) {
        try {
            Materiel materiel = materielService.creerMateriel(requete);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Matériel ajouté au catalogue.");
            reponse.put("materiel", materiel);
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        try {
            materielService.supprimerMateriel(id);
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Matériel supprimé du catalogue.");
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    // PATCH /api/materiels/{id}/statut -> ex: sortir de MAINTENANCE, passer en HS
    @PatchMapping("/{id}/statut")
    public ResponseEntity<Map<String, Object>> changerStatut(@PathVariable Long id,
                                                              @RequestBody ChangerStatutMaterielRequest requete) {
        try {
            Materiel materiel = materielService.changerStatut(id, requete.getStatut());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Statut mis à jour.");
            reponse.put("materiel", materiel);
            return ResponseEntity.ok(reponse);
        } catch (OperationException e) {
            return erreur(e.getMessage());
        }
    }

    // PATCH /api/materiels/{id}/stock -> ajoute de la quantité à un consommable
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> reapprovisionner(@PathVariable Long id,
                                                                 @RequestBody ReapprovisionnerRequest requete) {
        try {
            Materiel materiel = materielService.reapprovisionner(id, requete.getQuantite());
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Stock mis à jour.");
            reponse.put("materiel", materiel);
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