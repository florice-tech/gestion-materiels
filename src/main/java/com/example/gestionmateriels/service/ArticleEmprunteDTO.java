package com.example.gestionmateriels.service;

/**
 * Représente une ligne saisie dans le formulaire d'emprunt :
 * un matériel choisi et la quantité demandée.
 */
public class ArticleEmprunteDTO {

    private Long materielId;
    private Integer quantite;

    public ArticleEmprunteDTO() {
    }

    public ArticleEmprunteDTO(Long materielId, Integer quantite) {
        this.materielId = materielId;
        this.quantite = quantite;
    }

    public Long getMaterielId() {
        return materielId;
    }

    public void setMaterielId(Long materielId) {
        this.materielId = materielId;
    }

    public Integer getQuantite() {
        return quantite;
    }

    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }
}