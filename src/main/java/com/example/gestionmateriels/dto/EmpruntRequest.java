package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.service.ArticleEmprunteDTO;

import java.time.LocalTime;
import java.util.List;

/**
 * Corps JSON attendu pour créer un emprunt.
 */
public class EmpruntRequest {

    private String delegueNom;
    private String filiereNiveau;
    private String salle;
    private Long agentSortieId;
    private LocalTime heureRetourPrevue;
    private List<ArticleEmprunteDTO> articles;

    // Getters et Setters
    public String getDelegueNom() { return delegueNom; }
    public void setDelegueNom(String delegueNom) { this.delegueNom = delegueNom; }

    public String getFiliereNiveau() { return filiereNiveau; }
    public void setFiliereNiveau(String filiereNiveau) { this.filiereNiveau = filiereNiveau; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public Long getAgentSortieId() { return agentSortieId; }
    public void setAgentSortieId(Long agentSortieId) { this.agentSortieId = agentSortieId; }

    public LocalTime getHeureRetourPrevue() { return heureRetourPrevue; }
    public void setHeureRetourPrevue(LocalTime heureRetourPrevue) { this.heureRetourPrevue = heureRetourPrevue; }

    public List<ArticleEmprunteDTO> getArticles() { return articles; }
    public void setArticles(List<ArticleEmprunteDTO> articles) { this.articles = articles; }
}