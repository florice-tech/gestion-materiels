package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.MaterielRequest;
import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import com.example.gestionmateriels.repository.PhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MaterielService {

    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final CategorieRepository categorieRepository;
    private final PhotoRepository photoRepository;

    public MaterielService(MaterielRepository materielRepository,
                           DetailEmpruntRepository detailEmpruntRepository,
                           CategorieRepository categorieRepository, PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.categorieRepository = categorieRepository;
    }

    @Transactional(readOnly = true)
    public List<Materiel> lister() {
        return materielRepository.findAllByOrderByDesignationAsc();
    }

    public Materiel creerMateriel(MaterielRequest requete) {
        if (requete.typeGestion() == null) {
            throw new OperationException("Le type de gestion (durable/consommable) est obligatoire.");
        }

        Materiel materiel = new Materiel();
        materiel.setTypeGestion(requete.typeGestion());
        materiel.setStatut(Materiel.StatutMateriel.DISPONIBLE);
        appliquerChampsCommuns(materiel, requete);

        if (requete.typeGestion() == Materiel.TypeGestion.DURABLE) {
            materiel.setQuantiteStock(1);
        } else {
            int quantite = requete.quantiteStock() != null ? requete.quantiteStock() : 0;
            if (quantite < 0) {
                throw new OperationException("La quantité en stock ne peut pas être négative.");
            }
            materiel.setQuantiteStock(quantite);
        }
        return materielRepository.save(materiel);
    }

    /**
     * Modifie la désignation, la catégorie, le code (durable) ou le seuil d'alerte (consommable).
     * Le type de gestion et le stock ne changent pas ici (le stock passe par le réapprovisionnement).
     */
    public Materiel modifierMateriel(Long id, MaterielRequest requete) {
        Materiel materiel = trouver(id);
        if (requete.typeGestion() != null && requete.typeGestion() != materiel.getTypeGestion()) {
            throw new OperationException("Le type de gestion d'un matériel ne peut pas être modifié.");
        }
        appliquerChampsCommuns(materiel, requete);
        return materiel;
    }

    private void appliquerChampsCommuns(Materiel materiel, MaterielRequest requete) {
        materiel.setDesignation(Verifications.obligatoire(requete.designation(), "La désignation est obligatoire."));

        if (requete.categorieId() == null) {
            throw new OperationException("La catégorie est obligatoire.");
        }
        Categorie categorie = categorieRepository.findById(requete.categorieId())
                .orElseThrow(() -> OperationException.introuvable(
                        "Catégorie introuvable (id=" + requete.categorieId() + ")."));
        materiel.setCategorie(categorie);

        // Chaque matériel (durable ou fourniture) porte un code : c'est lui qui est imprimé dans son QR code.
        String code = Verifications.facultatif(requete.codeUnique());
        if (code == null) {
            code = materiel.getCodeUnique() != null ? materiel.getCodeUnique() : genererCode(materiel.getDesignation());
        }
        if (!code.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,49}")) {
            throw new OperationException("Le code ne peut contenir que des lettres, des chiffres, des points et des tirets (50 caractères au plus).");
        }
        String codeFinal = code;
        materielRepository.findByCodeUniqueIgnoreCase(codeFinal)
                .filter(autre -> !autre.getId().equals(materiel.getId()))
                .ifPresent(autre -> {
                    throw new OperationException("Ce code existe déjà : " + codeFinal + " (" + autre.getDesignation() + ").");
                });
        materiel.setCodeUnique(codeFinal);

        if (materiel.getTypeGestion() == Materiel.TypeGestion.CONSOMMABLE && requete.seuilAlerte() != null) {
            if (requete.seuilAlerte() < 0) {
                throw new OperationException("Le seuil d'alerte ne peut pas être négatif.");
            }
            materiel.setSeuilAlerte(requete.seuilAlerte());
        }
    }

    /**
     * Donne un code aux matériels qui n'en ont pas encore (fournitures créées avant les QR codes).
     * Appelé au démarrage ; renvoie le nombre de codes attribués.
     */
    public int attribuerCodesManquants() {
        int n = 0;
        for (Materiel m : materielRepository.findAllByOrderByDesignationAsc()) {
            if (m.getCodeUnique() == null || m.getCodeUnique().isBlank()) {
                m.setCodeUnique(genererCode(m.getDesignation()));
                materielRepository.saveAndFlush(m);
                n++;
            }
        }
        return n;
    }

    /** « Craie blanche (boîte) » → CRA-01, puis CRA-02... ; « Vidéoprojecteur » → VID-01. */
    String genererCode(String designation) {
        String prefixe = prefixe(designation);
        int numero = 1;
        while (materielRepository.findByCodeUniqueIgnoreCase(String.format("%s-%02d", prefixe, numero)).isPresent()) {
            numero++;
        }
        return String.format("%s-%02d", prefixe, numero);
    }

    static String prefixe(String designation) {
        String texte = Normalizer.normalize(designation == null ? "" : designation, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim();
        if (texte.isEmpty()) {
            return "MAT";
        }
        String mot = texte.split(" ")[0];
        return mot.length() < 2 ? "MAT" : mot.substring(0, Math.min(3, mot.length()));
    }

    public void supprimerMateriel(Long id) {
        Materiel materiel = trouver(id);
        if (materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
            throw new OperationException(
                    "Impossible de supprimer \"" + materiel.getDesignation() + "\" : il est actuellement emprunté.");
        }
        if (detailEmpruntRepository.existsByMaterielId(id)) {
            throw new OperationException(
                    "Impossible de supprimer \"" + materiel.getDesignation()
                            + "\" : il figure dans l'historique des emprunts. Passez-le plutôt au statut Hors service.");
        }
        photoRepository.deleteAll(photoRepository.findByMaterielId(id));
        photoRepository.flush();
        materielRepository.delete(materiel);
    }

    /**
     * Change manuellement le statut d'un matériel durable (ex : sortir de MAINTENANCE, passer en HS).
     * EMPRUNTE ne peut être ni posé ni retiré à la main : il vient uniquement du cycle d'emprunt.
     */
    public Materiel changerStatut(Long id, Materiel.StatutMateriel nouveauStatut) {
        if (nouveauStatut == null) {
            throw new OperationException("Le nouveau statut est obligatoire.");
        }
        Materiel materiel = trouver(id);
        if (nouveauStatut == Materiel.StatutMateriel.EMPRUNTE) {
            throw new OperationException("Le statut Emprunté ne peut être défini que par un emprunt réel.");
        }
        if (materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
            throw new OperationException("Ce matériel est emprunté : son statut changera à son retour.");
        }
        materiel.setStatut(nouveauStatut);
        return materiel;
    }

    /** Ajoute du stock à un consommable. */
    public Materiel reapprovisionner(Long id, Integer quantiteAjoutee) {
        if (quantiteAjoutee == null || quantiteAjoutee <= 0) {
            throw new OperationException("La quantité à ajouter doit être supérieure à zéro.");
        }
        Materiel materiel = trouver(id);
        if (materiel.getTypeGestion() != Materiel.TypeGestion.CONSOMMABLE) {
            throw new OperationException("Seuls les consommables peuvent être réapprovisionnés.");
        }
        materiel.setQuantiteStock(materiel.getQuantiteStock() + quantiteAjoutee);
        return materiel;
    }

    private Materiel trouver(Long id) {
        return materielRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Matériel introuvable (id=" + id + ")."));
    }
}
