package com.example.gestionmateriels.model;

import jakarta.persistence.*;

/**
 * Salle de cours proposée dans le formulaire de demande.
 * L'emprunt garde le nom de la salle en texte : supprimer ou renommer une salle
 * ne modifie donc pas l'historique.
 */
@Entity
@Table(name = "salles")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    public Salle() {
    }

    public Salle(String nom) {
        this.nom = nom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}
