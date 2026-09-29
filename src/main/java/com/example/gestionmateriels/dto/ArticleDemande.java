package com.example.gestionmateriels.dto;

/** Une ligne de demande : un matériel et la quantité voulue (1 pour un durable). */
public record ArticleDemande(Long materielId, Integer quantite) {
}
