package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Emprunt;

public class DetailRetourRequest {
    private Long detailId;
    private Emprunt.EtatRetour etatRetour;

    public Long getDetailId() { return detailId; }
    public void setDetailId(Long detailId) { this.detailId = detailId; }

    public Emprunt.EtatRetour getEtatRetour() { return etatRetour; }
    public void setEtatRetour(Emprunt.EtatRetour etatRetour) { this.etatRetour = etatRetour; }
}