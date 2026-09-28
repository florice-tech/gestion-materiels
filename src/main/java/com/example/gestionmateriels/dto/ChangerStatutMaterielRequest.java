package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Materiel;

public class ChangerStatutMaterielRequest {
    private Materiel.StatutMateriel statut;

    public Materiel.StatutMateriel getStatut() { return statut; }
    public void setStatut(Materiel.StatutMateriel statut) { this.statut = statut; }
}
