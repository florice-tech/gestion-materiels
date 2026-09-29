package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Agent de surveillance (ou remplaçant) qui remet ou réceptionne le matériel.
 * Sert à la traçabilité : chaque emprunt/retour est lié à un agent.
 * Chaque agent dispose d'un compte (identifiant + mot de passe) pour se connecter.
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

    // Identifiant de connexion (ex: "mdaniel"), distinct du nom affiché
    @Column(nullable = false, unique = true)
    private String identifiant;

    // Mot de passe (en clair pour l'instant, à hacher plus tard si besoin de plus de sécurité)
    // Jamais renvoyé dans les réponses JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String motDePasse;

    // Constructeur sans argument (obligatoire pour JPA)
    public Agent() {
    }

    // Constructeur avec arguments
    public Agent(String nom, String role, String identifiant, String motDePasse) {
        this.nom = nom;
        this.role = role;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
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

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}