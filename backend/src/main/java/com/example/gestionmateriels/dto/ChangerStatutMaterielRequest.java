package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Materiel;

public record ChangerStatutMaterielRequest(Materiel.StatutMateriel statut) {
}
