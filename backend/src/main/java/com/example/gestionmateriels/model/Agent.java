package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Agent de surveillance (ou remplaçant) qui remet et réceptionne le matériel.
 * Un agent "administrateur" peut en plus gérer les comptes.
 */
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    // Intitulé affiché (ex : "Surveillant", "Surveillant général")
    @Column(nullable = false)
    private String role = "Surveillant";

    // Identifiant de connexion (ex : "mdaniel"), unique parmi agents ET délégués
    @Column(nullable = false, unique = true)
    private String identifiant;

    // Haché avec BCrypt, jamais renvoyé dans les réponses JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String motDePasse;

    // Donne accès à la gestion des comptes
    @Column(nullable = false)
    private boolean administrateur = false;

    // Un compte désactivé ne peut plus se connecter (l'historique est conservé)
    @Column(nullable = false)
    private boolean actif = true;

    public Agent() {
    }

    public Agent(String nom, String role, String identifiant, String motDePasse, boolean administrateur) {
        this.nom = nom;
        this.role = role;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
        this.administrateur = administrateur;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public boolean isAdministrateur() { return administrateur; }
    public void setAdministrateur(boolean administrateur) { this.administrateur = administrateur; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}
