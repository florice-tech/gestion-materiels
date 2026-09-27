package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.service.ArticleEmprunteDTO;

import java.time.LocalTime;
import java.util.List;

/**
 * Corps JSON attendu quand un délégué fait une demande d'emprunt.
 */
public class DemandeEmpruntRequest {

    private Long delegueId;
    private String salle;
    private LocalTime heureRetourPrevue;
    private List<ArticleEmprunteDTO> articles;

    public Long getDelegueId() { return delegueId; }
    public void setDelegueId(Long delegueId) { this.delegueId = delegueId; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public LocalTime getHeureRetourPrevue() { return heureRetourPrevue; }
    public void setHeureRetourPrevue(LocalTime heureRetourPrevue) { this.heureRetourPrevue = heureRetourPrevue; }

    public List<ArticleEmprunteDTO> getArticles() { return articles; }
    public void setArticles(List<ArticleEmprunteDTO> articles) { this.articles = articles; }
}