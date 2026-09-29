package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

/**
 * Compte d'un délégué : peut se connecter et déclarer lui-même une demande d'emprunt.
 * La demande devra ensuite être validée et remise physiquement par un agent.
 */
@Entity
@Table(name = "delegues")
public class Delegue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String filiereNiveau;

    @Column(nullable = false, unique = true)
    private String identifiant;

    // Jamais renvoyé dans les réponses JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String motDePasse;

    public Delegue() {
    }

    public Delegue(String nom, String filiereNiveau, String identifiant, String motDePasse) {
        this.nom = nom;
        this.filiereNiveau = filiereNiveau;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
    }

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

    public String getFiliereNiveau() {
        return filiereNiveau;
    }

    public void setFiliereNiveau(String filiereNiveau) {
        this.filiereNiveau = filiereNiveau;
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