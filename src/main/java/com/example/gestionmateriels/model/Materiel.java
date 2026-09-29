package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Matériel du catalogue : durable (vidéoprojecteur, micro... suivi à l'unité)
 * ou consommable (marqueurs... suivi par quantité, non restitué).
 */
@Entity
@Table(name = "materiel")
public class Materiel {

    public enum TypeGestion {
        DURABLE, CONSOMMABLE
    }

    public enum StatutMateriel {
        DISPONIBLE, EMPRUNTE, A_VERIFIER, MAINTENANCE, HS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String designation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeGestion typeGestion;

    // Uniquement pour le matériel durable (unique s'il est renseigné)
    @Column(unique = true)
    private String codeUnique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutMateriel statut = StatutMateriel.DISPONIBLE;

    // Stock restant pour un consommable (toujours 1 pour un durable)
    @Column(nullable = false)
    private Integer quantiteStock = 1;

    // Consommables : une alerte "stock bas" s'affiche quand le stock atteint ce seuil
    @Column(nullable = false)
    private Integer seuilAlerte = 5;

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

    /** Vrai pour un consommable dont le stock est au niveau du seuil d'alerte ou en dessous. */
    @JsonProperty("stockBas")
    public boolean isStockBas() {
        return typeGestion == TypeGestion.CONSOMMABLE
                && quantiteStock != null && seuilAlerte != null
                && quantiteStock <= seuilAlerte;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }

    public TypeGestion getTypeGestion() { return typeGestion; }
    public void setTypeGestion(TypeGestion typeGestion) { this.typeGestion = typeGestion; }

    public String getCodeUnique() { return codeUnique; }
    public void setCodeUnique(String codeUnique) { this.codeUnique = codeUnique; }

    public StatutMateriel getStatut() { return statut; }
    public void setStatut(StatutMateriel statut) { this.statut = statut; }

    public Integer getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(Integer quantiteStock) { this.quantiteStock = quantiteStock; }

    public Integer getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(Integer seuilAlerte) { this.seuilAlerte = seuilAlerte; }
}
