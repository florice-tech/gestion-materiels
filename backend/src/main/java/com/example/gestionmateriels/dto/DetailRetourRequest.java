package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Emprunt;

/** État d'un article durable au retour. */
public record DetailRetourRequest(Long detailId, Emprunt.EtatRetour etatRetour) {
}
