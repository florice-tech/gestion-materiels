package com.example.gestionmateriels.dto;

/** Création d'un compte délégué (par lui-même ou par un administrateur). */
public record InscriptionDelegueRequest(String nom, String filiereNiveau, String matricule, String identifiant,
                                        String motDePasse, String telephone) {
}
