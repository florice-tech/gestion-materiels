package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Demande de passage d'un matériel d'un délégué à un autre.
 * Le nouveau délégué scanne le QR code d'un matériel déjà emprunté ; celui qui l'a reçoit
 * une notification et accepte (le matériel change de main) ou refuse.
 */
@Entity
@Table(name = "transferts")
public class Transfert {

    public enum Statut { EN_ATTENTE, ACCEPTE, REFUSE, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    // Ligne d'emprunt du délégué qui a le matériel au moment de la demande
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "detail_source_id", nullable = false)
    private DetailEmprunt detailSource;

    @ManyToOne(optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Delegue demandeur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "detenteur_id", nullable = false)
    private Delegue detenteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(nullable = false)
    private String salle;

    private LocalTime heureRetourPrevue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    private LocalDateTime dateReponse;

    // Fiche créée pour le nouveau délégué quand le transfert est accepté
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "emprunt_cree_id")
    private Emprunt empruntCree;

    @PrePersist
    protected void onCreate() {
        if (dateDemande == null) {
            dateDemande = LocalDateTime.now();
        }
    }

    public Transfert() {
    }

    public Transfert(Materiel materiel, DetailEmprunt detailSource, Delegue demandeur, Delegue detenteur,
                     String salle, LocalTime heureRetourPrevue) {
        this.materiel = materiel;
        this.detailSource = detailSource;
        this.demandeur = demandeur;
        this.detenteur = detenteur;
        this.salle = salle;
        this.heureRetourPrevue = heureRetourPrevue;
    }

    @JsonProperty("empruntCreeId")
    public Long getEmpruntCreeId() {
        return empruntCree != null ? empruntCree.getId() : null;
    }

    public Long getId() { return id; }
    public Materiel getMateriel() { return materiel; }
    public DetailEmprunt getDetailSource() { return detailSource; }
    public Delegue getDemandeur() { return demandeur; }
    public Delegue getDetenteur() { return detenteur; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public String getSalle() { return salle; }
    public LocalTime getHeureRetourPrevue() { return heureRetourPrevue; }
    public LocalDateTime getDateDemande() { return dateDemande; }
    public LocalDateTime getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDateTime dateReponse) { this.dateReponse = dateReponse; }
    public Emprunt getEmpruntCree() { return empruntCree; }
    public void setEmpruntCree(Emprunt empruntCree) { this.empruntCree = empruntCree; }
}
