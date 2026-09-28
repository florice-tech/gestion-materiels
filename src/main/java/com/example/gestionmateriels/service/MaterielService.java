package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.MaterielRequest;
import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.stereotype.Service;

@Service
public class MaterielService {

    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final CategorieRepository categorieRepository;

    public MaterielService(MaterielRepository materielRepository,
                           DetailEmpruntRepository detailEmpruntRepository,
                           CategorieRepository categorieRepository) {
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.categorieRepository = categorieRepository;
    }

    public Materiel creerMateriel(MaterielRequest requete) {
        if (requete.getDesignation() == null || requete.getDesignation().isBlank()) {
            throw new OperationException("La désignation est obligatoire.");
        }
        if (requete.getCategorieId() == null) {
            throw new OperationException("La catégorie est obligatoire.");
        }
        if (requete.getTypeGestion() == null) {
            throw new OperationException("Le type de gestion (durable/consommable) est obligatoire.");
        }

        Categorie categorie = categorieRepository.findById(requete.getCategorieId())
                .orElseThrow(() -> new OperationException("Catégorie introuvable (id=" + requete.getCategorieId() + ")."));

        Materiel materiel = new Materiel();
        materiel.setDesignation(requete.getDesignation());
        materiel.setCategorie(categorie);
        materiel.setTypeGestion(requete.getTypeGestion());
        materiel.setStatut(Materiel.StatutMateriel.DISPONIBLE);

        if (requete.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
            if (requete.getCodeUnique() == null || requete.getCodeUnique().isBlank()) {
                throw new OperationException("Le code unique est obligatoire pour un matériel durable.");
            }
            if (materielRepository.findByCodeUnique(requete.getCodeUnique()).isPresent()) {
                throw new OperationException("Ce code unique existe déjà : " + requete.getCodeUnique());
            }
            materiel.setCodeUnique(requete.getCodeUnique());
            materiel.setQuantiteStock(1);
        } else {
            materiel.setCodeUnique(null);
            int quantite = (requete.getQuantiteStock() != null) ? requete.getQuantiteStock() : 0;
            if (quantite < 0) {
                throw new OperationException("La quantité en stock ne peut pas être négative.");
            }
            materiel.setQuantiteStock(quantite);
        }

        return materielRepository.save(materiel);
    }

    public void supprimerMateriel(Long id) {
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + id + ")."));

        if (materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
            throw new OperationException(
                    "Impossible de supprimer \"" + materiel.getDesignation() + "\" : il est actuellement emprunté.");
        }

        boolean aDejaEteEmprunte = !detailEmpruntRepository.findByMaterielId(id).isEmpty();
        if (aDejaEteEmprunte) {
            throw new OperationException(
                    "Impossible de supprimer \"" + materiel.getDesignation()
                            + "\" : il possède un historique d'emprunts. Vous pouvez le passer au statut HS à la place.");
        }

        materielRepository.delete(materiel);
    }

    /**
     * Change manuellement le statut d'un matériel (ex: sortir de MAINTENANCE, passer en HS).
     * Le statut EMPRUNTE ne peut jamais être forcé manuellement : il ne vient que du cycle d'emprunt réel.
     */
    public Materiel changerStatut(Long id, Materiel.StatutMateriel nouveauStatut) {
        if (nouveauStatut == null) {
            throw new OperationException("Le nouveau statut est obligatoire.");
        }
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + id + ")."));

        if (nouveauStatut == Materiel.StatutMateriel.EMPRUNTE) {
            throw new OperationException("Le statut EMPRUNTE ne peut être défini que par un emprunt réel.");
        }

        materiel.setStatut(nouveauStatut);
        return materielRepository.save(materiel);
    }

    /**
     * Ajoute du stock à un consommable (réapprovisionnement).
     */
    public Materiel reapprovisionner(Long id, Integer quantiteAjoutee) {
        if (quantiteAjoutee == null || quantiteAjoutee <= 0) {
            throw new OperationException("La quantité à ajouter doit être supérieure à zéro.");
        }
        Materiel materiel = materielRepository.findById(id)
                .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + id + ")."));

        if (materiel.getTypeGestion() != Materiel.TypeGestion.CONSOMMABLE) {
            throw new OperationException("Seuls les consommables peuvent être réapprovisionnés.");
        }

        materiel.setQuantiteStock(materiel.getQuantiteStock() + quantiteAjoutee);
        return materielRepository.save(materiel);
    }
}