package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Materiel;

/**
 * Création / modification d'un matériel.
 * codeUnique : durable uniquement. quantiteStock : consommable, à la création uniquement.
 * seuilAlerte : consommable. Le type de gestion ne peut pas être changé après création.
 */
public record MaterielRequest(String designation, Long categorieId, Materiel.TypeGestion typeGestion,
                              String codeUnique, Integer quantiteStock, Integer seuilAlerte) {
}
