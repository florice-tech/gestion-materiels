package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ChangerStatutMaterielRequest;
import com.example.gestionmateriels.dto.MaterielRequest;
import com.example.gestionmateriels.dto.ReapprovisionnerRequest;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.service.MaterielService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Catalogue : lecture pour tous les connectés, modifications pour les agents (voir SecuriteConfig). */
@RestController
@RequestMapping("/api/materiels")
public class MaterielController {

    private final MaterielService materielService;

    public MaterielController(MaterielService materielService) {
        this.materielService = materielService;
    }

    @GetMapping
    public List<Materiel> lister() {
        return materielService.lister();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> creer(@RequestBody MaterielRequest requete) {
        return Reponses.ok("Matériel ajouté au catalogue.", "materiel", materielService.creerMateriel(requete));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable Long id, @RequestBody MaterielRequest requete) {
        return Reponses.ok("Matériel modifié.", "materiel", materielService.modifierMateriel(id, requete));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> supprimer(@PathVariable Long id) {
        materielService.supprimerMateriel(id);
        return Reponses.ok("Matériel supprimé du catalogue.");
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Map<String, Object>> changerStatut(@PathVariable Long id,
                                                              @RequestBody ChangerStatutMaterielRequest requete) {
        return Reponses.ok("Statut mis à jour.", "materiel", materielService.changerStatut(id, requete.statut()));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> reapprovisionner(@PathVariable Long id,
                                                                 @RequestBody ReapprovisionnerRequest requete) {
        return Reponses.ok("Stock mis à jour.", "materiel", materielService.reapprovisionner(id, requete.quantite()));
    }
}
