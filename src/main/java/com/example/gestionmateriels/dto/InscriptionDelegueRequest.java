package com.example.gestionmateriels.dto;

public class InscriptionDelegueRequest {
    private String nom;
    private String filiereNiveau;
    private String identifiant;
    private String motDePasse;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getFiliereNiveau() { return filiereNiveau; }
    public void setFiliereNiveau(String filiereNiveau) { this.filiereNiveau = filiereNiveau; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
}