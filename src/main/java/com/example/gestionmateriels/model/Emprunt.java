package com.example.gestionmateriels.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fiche d'emprunt / retour : journal d'audit des mouvements de matériel.
 * Garde l'agent qui a remis le matériel et celui qui l'a réceptionné.
 */
@Entity
@Table(name = "emprunts")
public class Emprunt {

    // ----- Enum -----
    // Note : VIDE_EPUICE conservé tel que demandé (probablement à corriger en VIDE_EPUISE)
    public enum EtatRetour {
        BON_ETAT, A_VERIFIER, ENDOMMAGE, VIDE_EPUICE
    }

    // ----- Attributs -----
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String delegueNom;

    @Column(nullable = false)
    private String filiereNiveau;

    @Column(nullable = false)
    private String salle;

    // Agent qui remet le matériel (obligatoire)
    @ManyToOne(optional = false)
    @JoinColumn(name = "agent_sortie_id", nullable = false)
    private Agent agentSortie;

    // Date de sortie : renseignée automatiquement à la création
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateSortie;

    private LocalTime heureRetourPrevue;

    // Agent qui réceptionne le retour (optionnel tant que le matériel n'est pas rendu)
    @ManyToOne
    @JoinColumn(name = "agent_retour_id")
    private Agent agentRetour;

    private LocalDateTime dateRetour;

    @Enumerated(EnumType.STRING)
    private EtatRetour etatRetour;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // Initialise dateSortie juste avant l'insertion en base
    @PrePersist
    protected void onCreate() {
        if (this.dateSortie == null) {
            this.dateSortie = LocalDateTime.now();
        }
    }

    // ----- Constructeurs -----
    public Emprunt() {
    }

    public Emprunt(String delegueNom, String filiereNiveau, String salle,
                   Agent agentSortie, LocalTime heureRetourPrevue) {
        this.delegueNom = delegueNom;
        this.filiereNiveau = filiereNiveau;
        this.salle = salle;
        this.agentSortie = agentSortie;
        this.heureRetourPrevue = heureRetourPrevue;
    }

    // ----- Getters et Setters -----
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDelegueNom() {
        return delegueNom;
    }

    public void setDelegueNom(String delegueNom) {
        this.delegueNom = delegueNom;
    }

    public String getFiliereNiveau() {
        return filiereNiveau;
    }

    public void setFiliereNiveau(String filiereNiveau) {
        this.filiereNiveau = filiereNiveau;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String salle) {
        this.salle = salle;
    }

    public Agent getAgentSortie() {
        return agentSortie;
    }

    public void setAgentSortie(Agent agentSortie) {
        this.agentSortie = agentSortie;
    }

    public LocalDateTime getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(LocalDateTime dateSortie) {
        this.dateSortie = dateSortie;
    }

    public LocalTime getHeureRetourPrevue() {
        return heureRetourPrevue;
    }

    public void setHeureRetourPrevue(LocalTime heureRetourPrevue) {
        this.heureRetourPrevue = heureRetourPrevue;
    }

    public Agent getAgentRetour() {
        return agentRetour;
    }

    public void setAgentRetour(Agent agentRetour) {
        this.agentRetour = agentRetour;
    }

    public LocalDateTime getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(LocalDateTime dateRetour) {
        this.dateRetour = dateRetour;
    }

    public EtatRetour getEtatRetour() {
        return etatRetour;
    }

    public void setEtatRetour(EtatRetour etatRetour) {
        this.etatRetour = etatRetour;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}