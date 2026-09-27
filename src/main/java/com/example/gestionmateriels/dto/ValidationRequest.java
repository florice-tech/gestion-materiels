package com.example.gestionmateriels.dto;

/**
 * Corps JSON attendu quand un agent valide une demande d'emprunt en attente.
 */
public class ValidationRequest {

    private Long empruntId;
    private Long agentSortieId;

    public Long getEmpruntId() { return empruntId; }
    public void setEmpruntId(Long empruntId) { this.empruntId = empruntId; }

    public Long getAgentSortieId() { return agentSortieId; }
    public void setAgentSortieId(Long agentSortieId) { this.agentSortieId = agentSortieId; }
}