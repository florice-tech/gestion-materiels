package com.example.gestionmateriels.dto;

import com.example.gestionmateriels.model.Emprunt;

/**
 * Corps JSON attendu pour enregistrer un retour.
 */
public class RetourRequest {

    private Long empruntId;
    private Long agentRetourId;
    private Emprunt.EtatRetour etatRetour;
    private String observations;

    // Getters et Setters
    public Long getEmpruntId() { return empruntId; }
    public void setEmpruntId(Long empruntId) { this.empruntId = empruntId; }

    public Long getAgentRetourId() { return agentRetourId; }
    public void setAgentRetourId(Long agentRetourId) { this.agentRetourId = agentRetourId; }

    public Emprunt.EtatRetour getEtatRetour() { return etatRetour; }
    public void setEtatRetour(Emprunt.EtatRetour etatRetour) { this.etatRetour = etatRetour; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}