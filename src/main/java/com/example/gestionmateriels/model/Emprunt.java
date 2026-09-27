package com.example.gestionmateriels.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Fiche d'emprunt / retour : journal d'audit des mouvements de matériel.
 * Un délégué crée la demande (statut EN_ATTENTE), un agent la valide et remet
 * physiquement le matériel (statut EN_COURS), puis un agent enregistre le retour.
 */
@Entity
@Table(name = "emprunts")
public class Emprunt {

    // ----- Enums -----

    // Étape du cycle de vie de la demande, avant même le retour
    public enum StatutEmprunt {
        EN_ATTENTE, EN_COURS
    }

    public enum EtatRetour {
        BON_ETAT, A_VERIFIER, ENDOMMAGE, VIDE_EPUISE
    }

    // ----- Attributs -----
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le délégué qui a fait la demande (compte réel, plus un simple texte)
    @ManyToOne(optional = false)
    @JoinColumn(name = "delegue_id", nullable = false)
    private Delegue delegue;

    @Column(nullable = false)
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEmprunt statutEmprunt = StatutEmprunt.EN_ATTENTE;

    // Agent qui remet le matériel : optionnel tant que la demande n'est pas validée
    @ManyToOne
    @JoinColumn(name = "agent_sortie_id")
    private Agent agentSortie;

    // Date de la demande : renseignée automatiquement à la création
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

    // Constructeur utilisé par le délégué : pas encore d'agent, statut EN_ATTENTE par défaut
    public Emprunt(Delegue delegue, String salle, LocalTime heureRetourPrevue) {
        this.delegue = delegue;
        this.salle = salle;
        this.heureRetourPrevue = heureRetourPrevue;
    }

    // ----- Getters et Setters -----
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Delegue getDelegue() {
        return delegue;
    }

    public void setDelegue(Delegue delegue) {
        this.delegue = delegue;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String salle) {
        this.salle = salle;
    }

    public StatutEmprunt getStatutEmprunt() {
        return statutEmprunt;
    }

    public void setStatutEmprunt(StatutEmprunt statutEmprunt) {
        this.statutEmprunt = statutEmprunt;
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