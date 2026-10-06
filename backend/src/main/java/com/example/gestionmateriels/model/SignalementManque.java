package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Une demande d'un matériel manquant : qui, quand, pour quelle salle. */
@Entity
@Table(name = "signalements_manque")
public class SignalementManque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "manque_id", nullable = false)
    private Manque manque;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegue_id")
    private Delegue delegue;

    @Column(nullable = false)
    private String auteur;

    private String salle;

    @Column(length = 500)
    private String commentaire;

    @Column(name = "date_signalement", nullable = false)
    private LocalDateTime dateSignalement = LocalDateTime.now();

    public SignalementManque() {
    }

    public SignalementManque(Manque manque, Delegue delegue, String auteur, String salle, String commentaire) {
        this.manque = manque;
        this.delegue = delegue;
        this.auteur = auteur;
        this.salle = salle;
        this.commentaire = commentaire;
    }

    public Long getId() { return id; }
    public Manque getManque() { return manque; }
    public Delegue getDelegue() { return delegue; }
    public String getAuteur() { return auteur; }
    public String getSalle() { return salle; }
    public String getCommentaire() { return commentaire; }
    public LocalDateTime getDateSignalement() { return dateSignalement; }
}
