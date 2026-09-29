package com.example.gestionmateriels.dto;

/**
 * Corps JSON envoyé par un délégué qui annule sa propre demande en attente.
 */
public class AnnulationRequest {

    private Long delegueId;

    public Long getDelegueId() { return delegueId; }
    public void setDelegueId(Long delegueId) { this.delegueId = delegueId; }
}
