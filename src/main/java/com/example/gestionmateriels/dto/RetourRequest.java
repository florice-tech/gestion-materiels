package com.example.gestionmateriels.dto;

import java.util.List;

public class RetourRequest {
    private Long empruntId;
    private Long agentRetourId;
    private String observations;
    private List<DetailRetourRequest> details;

    public Long getEmpruntId() { return empruntId; }
    public void setEmpruntId(Long empruntId) { this.empruntId = empruntId; }

    public Long getAgentRetourId() { return agentRetourId; }
    public void setAgentRetourId(Long agentRetourId) { this.agentRetourId = agentRetourId; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<DetailRetourRequest> getDetails() { return details; }
    public void setDetails(List<DetailRetourRequest> details) { this.details = details; }
}