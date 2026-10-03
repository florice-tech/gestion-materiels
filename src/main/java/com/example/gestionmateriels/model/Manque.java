package com.example.gestionmateriels.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Matériel qui manque : demandé par des délégués alors qu'il n'existe pas ou n'est pas
 * disponible, ou fourniture épuisée. Il reste dans la liste tant qu'il n'a pas été ajouté.
 * Chaque nouvelle demande du même matériel augmente le compteur au lieu de créer une ligne.
 */
@Entity
@Table(name = "manques")
public class Manque {

    public enum Statut { OUVERT, RESOLU, ABANDONNE }

    public enum Origine { DELEGUE, AGENT, STOCK_EPUISE, INDISPONIBLE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String designation;

    @ManyToOne
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @Column(name = "quantite_souhaitee", nullable = false)
    private Integer quantiteSouhaitee = 1;

    @Column(name = "nombre_demandes", nullable = false)
    private Integer nombreDemandes = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.OUVERT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origine origine;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_derniere_demande", nullable = false)
    private LocalDateTime dateDerniereDemande = LocalDateTime.now();

    @Column(name = "date_resolution")
    private LocalDateTime dateResolution;

    @Column(name = "resolu_par")
    private String resoluPar;

    @Column(name = "note_resolution", length = 500)
    private String noteResolution;

    public Long getId() { return id; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) { this.categorie = categorie; }
    public Integer getQuantiteSouhaitee() { return quantiteSouhaitee; }
    public void setQuantiteSouhaitee(Integer quantiteSouhaitee) { this.quantiteSouhaitee = quantiteSouhaitee; }
    public Integer getNombreDemandes() { return nombreDemandes; }
    public void setNombreDemandes(Integer nombreDemandes) { this.nombreDemandes = nombreDemandes; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public Origine getOrigine() { return origine; }
    public void setOrigine(Origine origine) { this.origine = origine; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public LocalDateTime getDateDerniereDemande() { return dateDerniereDemande; }
    public void setDateDerniereDemande(LocalDateTime d) { this.dateDerniereDemande = d; }
    public LocalDateTime getDateResolution() { return dateResolution; }
    public void setDateResolution(LocalDateTime dateResolution) { this.dateResolution = dateResolution; }
    public String getResoluPar() { return resoluPar; }
    public void setResoluPar(String resoluPar) { this.resoluPar = resoluPar; }
    public String getNoteResolution() { return noteResolution; }
    public void setNoteResolution(String noteResolution) { this.noteResolution = noteResolution; }
}
