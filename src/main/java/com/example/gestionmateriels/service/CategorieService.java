package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.stereotype.Service;

@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final MaterielRepository materielRepository;

    public CategorieService(CategorieRepository categorieRepository, MaterielRepository materielRepository) {
        this.categorieRepository = categorieRepository;
        this.materielRepository = materielRepository;
    }

    public Categorie creerCategorie(String nom) {
        if (nom == null || nom.isBlank()) {
            throw new OperationException("Le nom de la catégorie est obligatoire.");
        }
        if (categorieRepository.findByNom(nom).isPresent()) {
            throw new OperationException("Cette catégorie existe déjà : " + nom);
        }
        return categorieRepository.save(new Categorie(nom));
    }

    public void supprimerCategorie(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new OperationException("Catégorie introuvable (id=" + id + ")."));

        boolean utilisee = !materielRepository.findByCategorie(categorie).isEmpty();
        if (utilisee) {
            throw new OperationException(
                    "Impossible de supprimer \"" + categorie.getNom() + "\" : des matériels utilisent encore cette catégorie.");
        }

        categorieRepository.delete(categorie);
    }
}