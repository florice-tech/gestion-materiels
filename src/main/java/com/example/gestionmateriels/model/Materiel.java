package com.example.gestionmateriels.model;

import jakarta.persistence.*;

/**
 * Matériel du catalogue : durable (vidéoprojecteur, micro...) ou consommable (marqueurs...).
 */
@Entity
@Table(name = "materiel")
public class Materiel {

    // ----- Enums -----
    public enum Categorie {
        AUDIOVISUEL, ACCESSOIRES, CONNECTIQUE, FOURNITURES
    }

    public enum TypeGestion {
        DURABLE, CONSOMMABLE
    }

    public enum StatutMateriel {
        DISPONIBLE, EMPRUNTE, A_VERIFIER, MAINTENANCE, HS
    }

    // ----- Attributs -----
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ex : Vidéoprojecteur, Micro, Marqueur Noir
    @Column(nullable = false)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeGestion typeGestion;

    // Optionnel : uniquement pour le matériel durable (unique s'il est renseigné)
    @Column(unique = true)
    private String codeUnique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMateriel statut = StatutMateriel.DISPONIBLE;

    // Utilisé surtout pour les consommables
    private Integer quantiteStock = 1;

    // ----- Constructeurs -----
    public Materiel() {
    }

    public Materiel(String designation, Categorie categorie, TypeGestion typeGestion,
                    String codeUnique, StatutMateriel statut, Integer quantiteStock) {
        this.designation = designation;
        this.categorie = categorie;
        this.typeGestion = typeGestion;
        this.codeUnique = codeUnique;
        this.statut = statut;
        this.quantiteStock = quantiteStock;
    }

    // ----- Getters et Setters -----
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public TypeGestion getTypeGestion() {
        return typeGestion;
    }

    public void setTypeGestion(TypeGestion typeGestion) {
        this.typeGestion = typeGestion;
    }

    public String getCodeUnique() {
        return codeUnique;
    }

    public void setCodeUnique(String codeUnique) {
        this.codeUnique = codeUnique;
    }

    public StatutMateriel getStatut() {
        return statut;
    }

    public void setStatut(StatutMateriel statut) {
        this.statut = statut;
    }

    public Integer getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(Integer quantiteStock) {
        this.quantiteStock = quantiteStock;
    }
}