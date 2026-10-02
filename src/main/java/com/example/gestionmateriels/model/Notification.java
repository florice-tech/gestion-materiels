package com.example.gestionmateriels.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Notification affichée dans l'application (cloche de l'en-tête) pour un agent ou un délégué.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    public enum Destinataire { AGENT, DELEGUE }

    /** Sert à choisir l'icône et la couleur dans l'interface. */
    public enum Categorie { DEMANDE, VALIDATION, REFUS, TRANSFERT, RETOUR, RETARD, RESERVATION, INFO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Destinataire destinataireType;

    @Column(nullable = false)
    private Long destinataireId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categorie categorie;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    // Page à ouvrir quand on clique sur la notification (ex : "emprunt.html")
    private String lien;

    @Column(nullable = false)
    private boolean lue = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public Notification() {
    }

    public Notification(Destinataire destinataireType, Long destinataireId, Categorie categorie,
                        String titre, String message, String lien) {
        this.destinataireType = destinataireType;
        this.destinataireId = destinataireId;
        this.categorie = categorie;
        this.titre = titre;
        this.message = message;
        this.lien = lien;
    }

    public Long getId() { return id; }
    public Destinataire getDestinataireType() { return destinataireType; }
    public Long getDestinataireId() { return destinataireId; }
    public Categorie getCategorie() { return categorie; }
    public String getTitre() { return titre; }
    public String getMessage() { return message; }
    public String getLien() { return lien; }
    public boolean isLue() { return lue; }
    public void setLue(boolean lue) { this.lue = lue; }
    public LocalDateTime getDateCreation() { return dateCreation; }
}
