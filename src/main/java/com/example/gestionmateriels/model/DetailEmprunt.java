package com.example.gestionmateriels.model;

import jakarta.persistence.*;

/**
 * Ligne de détail d'un emprunt : quel matériel, et en quelle quantité,
 * a été inclus dans une fiche d'emprunt donnée.
 */
@Entity
@Table(name = "details_emprunt")
public class DetailEmprunt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'emprunt auquel appartient cette ligne
    @ManyToOne(optional = false)
    @JoinColumn(name = "emprunt_id", nullable = false)
    private Emprunt emprunt;

    // Le matériel emprunté
    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    // Nombre d'unités (1 pour un vidéoprojecteur, 3 pour des marqueurs, etc.)
    @Column(nullable = false)
    private Integer quantite = 1;

    // Constructeurs
    public DetailEmprunt() {
    }

    public DetailEmprunt(Emprunt emprunt, Materiel materiel, Integer quantite) {
        this.emprunt = emprunt;
        this.materiel = materiel;
        this.quantite = quantite;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Emprunt getEmprunt() {
        return emprunt;
    }

    public void setEmprunt(Emprunt emprunt) {
        this.emprunt = emprunt;
    }

    public Materiel getMateriel() {
        return materiel;
    }

    public void setMateriel(Materiel materiel) {
        this.materiel = materiel;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }
}