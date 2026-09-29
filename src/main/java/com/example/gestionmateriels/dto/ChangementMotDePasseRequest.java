package com.example.gestionmateriels.dto;

/** Changement de son propre mot de passe (l'ancien est exigé). */
public record ChangementMotDePasseRequest(String ancienMotDePasse, String nouveauMotDePasse) {
}
