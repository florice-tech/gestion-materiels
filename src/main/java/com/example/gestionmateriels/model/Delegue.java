package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Compte d'un délégué : peut se connecter et déclarer lui-même une demande d'emprunt.
 * La demande doit ensuite être validée et le matériel remis physiquement par un agent.
 */
@Entity
@Table(name = "delegues")
public class Delegue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String filiereNiveau;

    @Column(nullable = false, unique = true)
    private String identifiant;

    // Haché avec BCrypt, jamais renvoyé dans les réponses JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // Numéro WhatsApp (facultatif), au format international sans « + » : 22890123456
    @Column(length = 30)
    private String telephone;

    // Un agent a levé le blocage des réservations : les retards antérieurs ne bloquent plus
    @Column(name = "reservations_debloquees_le")
    private LocalDateTime reservationsDebloqueesLe;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    public Delegue() {
    }

    public Delegue(String nom, String filiereNiveau, String identifiant, String motDePasse) {
        this.nom = nom;
        this.filiereNiveau = filiereNiveau;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getFiliereNiveau() { return filiereNiveau; }
    public void setFiliereNiveau(String filiereNiveau) { this.filiereNiveau = filiereNiveau; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public LocalDateTime getReservationsDebloqueesLe() { return reservationsDebloqueesLe; }
    public void setReservationsDebloqueesLe(LocalDateTime le) { this.reservationsDebloqueesLe = le; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
