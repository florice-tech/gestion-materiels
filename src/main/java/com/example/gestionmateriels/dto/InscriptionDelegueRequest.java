package com.example.gestionmateriels.dto;

/** Création de son compte par un délégué. */
public record InscriptionDelegueRequest(String nom, String filiereNiveau, String identifiant, String motDePasse,
                                        String telephone) {
}
