package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Materiel;

/**
 * Corps JSON attendu pour créer un nouveau matériel dans le catalogue.
 */
public class MaterielRequest {

    private String designation;
    private Materiel.Categorie categorie;
    private Materiel.TypeGestion typeGestion;
    private String codeUnique;       // Uniquement pour le matériel durable
    private Integer quantiteStock;   // Uniquement pour les consommables (1 par défaut pour le durable)

    // Getters et Setters
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Materiel.Categorie getCategorie() { return categorie; }
    public void setCategorie(Materiel.Categorie categorie) { this.categorie = categorie; }

    public Materiel.TypeGestion getTypeGestion() { return typeGestion; }
    public void setTypeGestion(Materiel.TypeGestion typeGestion) { this.typeGestion = typeGestion; }

    public String getCodeUnique() { return codeUnique; }
    public void setCodeUnique(String codeUnique) { this.codeUnique = codeUnique; }

    public Integer getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(Integer quantiteStock) { this.quantiteStock = quantiteStock; }
}