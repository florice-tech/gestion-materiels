package com.example.gestionmateriels.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emprunts")
public class Emprunt {

    public enum StatutEmprunt {
        EN_ATTENTE, EN_COURS
    }

    public enum EtatRetour {
        BON_ETAT, A_VERIFIER, ENDOMMAGE, VIDE_EPUISE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delegue_id", nullable = false)
    private Delegue delegue;

    @Column(nullable = false)
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEmprunt statutEmprunt = StatutEmprunt.EN_ATTENTE;

    @ManyToOne
    @JoinColumn(name = "agent_sortie_id")
    private Agent agentSortie;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateSortie;

    private LocalTime heureRetourPrevue;

    @ManyToOne
    @JoinColumn(name = "agent_retour_id")
    private Agent agentRetour;

    private LocalDateTime dateRetour;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // Liste des matériels empruntés, chacun avec son propre état de retour
    @OneToMany(mappedBy = "emprunt", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DetailEmprunt> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.dateSortie == null) {
            this.dateSortie = LocalDateTime.now();
        }
    }

    public Emprunt() {
    }

    public Emprunt(Delegue delegue, String salle, LocalTime heureRetourPrevue) {
        this.delegue = delegue;
        this.salle = salle;
        this.heureRetourPrevue = heureRetourPrevue;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Delegue getDelegue() { return delegue; }
    public void setDelegue(Delegue delegue) { this.delegue = delegue; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public StatutEmprunt getStatutEmprunt() { return statutEmprunt; }
    public void setStatutEmprunt(StatutEmprunt statutEmprunt) { this.statutEmprunt = statutEmprunt; }

    public Agent getAgentSortie() { return agentSortie; }
    public void setAgentSortie(Agent agentSortie) { this.agentSortie = agentSortie; }

    public LocalDateTime getDateSortie() { return dateSortie; }
    public void setDateSortie(LocalDateTime dateSortie) { this.dateSortie = dateSortie; }

    public LocalTime getHeureRetourPrevue() { return heureRetourPrevue; }
    public void setHeureRetourPrevue(LocalTime heureRetourPrevue) { this.heureRetourPrevue = heureRetourPrevue; }

    public Agent getAgentRetour() { return agentRetour; }
    public void setAgentRetour(Agent agentRetour) { this.agentRetour = agentRetour; }

    public LocalDateTime getDateRetour() { return dateRetour; }
    public void setDateRetour(LocalDateTime dateRetour) { this.dateRetour = dateRetour; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<DetailEmprunt> getDetails() { return details; }
    public void setDetails(List<DetailEmprunt> details) { this.details = details; }
}