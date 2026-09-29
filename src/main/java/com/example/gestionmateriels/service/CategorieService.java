package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategorieService {

    private final CategorieRepository categorieRepository;
    private final MaterielRepository materielRepository;

    public CategorieService(CategorieRepository categorieRepository, MaterielRepository materielRepository) {
        this.categorieRepository = categorieRepository;
        this.materielRepository = materielRepository;
    }

    @Transactional(readOnly = true)
    public List<Categorie> lister() {
        return categorieRepository.findAllByOrderByNomAsc();
    }

    public Categorie creer(String nom) {
        String nomPropre = Verifications.obligatoire(nom, "Le nom de la catégorie est obligatoire.");
        verifierNomLibre(nomPropre, null);
        return categorieRepository.save(new Categorie(nomPropre));
    }

    public Categorie renommer(Long id, String nom) {
        Categorie categorie = trouver(id);
        String nomPropre = Verifications.obligatoire(nom, "Le nom de la catégorie est obligatoire.");
        verifierNomLibre(nomPropre, id);
        categorie.setNom(nomPropre);
        return categorie;
    }

    public void supprimer(Long id) {
        Categorie categorie = trouver(id);
        if (materielRepository.existsByCategorie(categorie)) {
            throw new OperationException("Impossible de supprimer \"" + categorie.getNom()
                    + "\" : des matériels utilisent encore cette catégorie.");
        }
        categorieRepository.delete(categorie);
    }

    private void verifierNomLibre(String nom, Long idActuel) {
        categorieRepository.findByNomIgnoreCase(nom)
                .filter(autre -> !autre.getId().equals(idActuel))
                .ifPresent(autre -> {
                    throw new OperationException("Cette catégorie existe déjà : " + nom);
                });
    }

    private Categorie trouver(Long id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Catégorie introuvable (id=" + id + ")."));
    }
}
