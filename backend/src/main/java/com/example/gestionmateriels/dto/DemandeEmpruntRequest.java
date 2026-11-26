package com.example.gestionmateriels.dto;

import java.time.LocalTime;
import java.util.List;

/** Demande d'emprunt d'un délégué (le délégué est celui de la session). */
public record DemandeEmpruntRequest(String salle, LocalTime heureRetourPrevue, List<ArticleDemande> articles) {
}
