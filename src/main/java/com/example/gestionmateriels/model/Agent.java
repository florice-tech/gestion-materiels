package com.example.gestionmateriels.model;

import jakarta.persistence.*;

/**
 * Agent de surveillance (ou remplaçant) qui remet ou réceptionne le matériel.
 * Sert à la traçabilité : chaque emprunt/retour est lié à un agent.
 */
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Long id;

    @Column(nullable = false)
    private String nom;

    // Valeur par défaut : "Surveillant"
    @Column(nullable = false)
    private String role = "Surveillant";

    // Constructeur sans argument (obligatoire pour JPA)
    public Agent() {
    }

    // Constructeur avec arguments
    public Agent(String nom, String role) {
        this.nom = nom;
        this.role = role;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}