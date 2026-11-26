package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Matériel gâté : déclaré au retour (état « endommagé » ou « vide / épuisé »), en scannant
 * le matériel, ou depuis le catalogue. Il reste dans la liste jusqu'au bouton « Réparé ».
 */
@Entity
@Table(name = "pannes")
public class Panne {

    public enum Origine { RETOUR, SCAN, CATALOGUE }

    public enum Statut { EN_PANNE, REPAREE, HORS_SERVICE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Origine origine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.EN_PANNE;

    @Column(name = "date_declaration", nullable = false)
    private LocalDateTime dateDeclaration = LocalDateTime.now();

    @Column(name = "declaree_par", nullable = false)
    private String declareePar;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_emprunt_id")
    private DetailEmprunt detailEmprunt;

    /** Délégué qui avait le matériel quand la panne a été constatée (retour). */
    @Column(name = "delegue_concerne")
    private String delegueConcerne;

    @Column(name = "date_reparation")
    private LocalDateTime dateReparation;

    @Column(name = "reparee_par")
    private String repareePar;

    @Column(name = "note_reparation", length = 500)
    private String noteReparation;

    /** Coût de la réparation en FCFA (facultatif). */
    @Column(name = "cout_reparation")
    private Integer coutReparation;

    public Panne() {
    }

    public Panne(Materiel materiel, String description, Origine origine, Statut statut, String declareePar) {
        this.materiel = materiel;
        this.description = description;
        this.origine = origine;
        this.statut = statut;
        this.declareePar = declareePar;
    }

    /** Fiche d'emprunt concernée (pour ouvrir le bon d'emprunt). */
    @JsonProperty("empruntId")
    public Long getEmpruntId() {
        return detailEmprunt != null ? detailEmprunt.getEmprunt().getId() : null;
    }

    @JsonProperty("detailEmpruntId")
    public Long getDetailEmpruntId() {
        return detailEmprunt != null ? detailEmprunt.getId() : null;
    }

    public Long getId() { return id; }
    public Materiel getMateriel() { return materiel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Origine getOrigine() { return origine; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public LocalDateTime getDateDeclaration() { return dateDeclaration; }
    public String getDeclareePar() { return declareePar; }
    public DetailEmprunt getDetailEmprunt() { return detailEmprunt; }
    public void setDetailEmprunt(DetailEmprunt detailEmprunt) { this.detailEmprunt = detailEmprunt; }
    public String getDelegueConcerne() { return delegueConcerne; }
    public void setDelegueConcerne(String delegueConcerne) { this.delegueConcerne = delegueConcerne; }
    public LocalDateTime getDateReparation() { return dateReparation; }
    public void setDateReparation(LocalDateTime dateReparation) { this.dateReparation = dateReparation; }
    public String getRepareePar() { return repareePar; }
    public void setRepareePar(String repareePar) { this.repareePar = repareePar; }
    public String getNoteReparation() { return noteReparation; }
    public void setNoteReparation(String noteReparation) { this.noteReparation = noteReparation; }
    public Integer getCoutReparation() { return coutReparation; }
    public void setCoutReparation(Integer coutReparation) { this.coutReparation = coutReparation; }
}
