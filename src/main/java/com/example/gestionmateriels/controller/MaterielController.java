package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expose le catalogue de matériel en lecture seule pour alimenter les formulaires.
 */
@RestController
@RequestMapping("/api/materiels")
public class MaterielController {

    private final MaterielRepository materielRepository;

    public MaterielController(MaterielRepository materielRepository) {
        this.materielRepository = materielRepository;
    }

    // GET /api/materiels -> liste complète du catalogue
    @GetMapping
    public List<Materiel> lister() {
        return materielRepository.findAll();
    }
}