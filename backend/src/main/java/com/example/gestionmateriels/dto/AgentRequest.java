package com.example.gestionmateriels.dto;

/**
 * Création ou modification d'un agent par un administrateur.
 * Le mot de passe n'est utilisé qu'à la création.
 */
public record AgentRequest(String nom, String role, String identifiant, String motDePasse, Boolean administrateur) {
}
