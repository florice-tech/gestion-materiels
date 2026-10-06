package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "details_emprunt")
public class DetailEmprunt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "emprunt_id", nullable = false)
    @JsonIgnore // évite la boucle infinie Emprunt -> details -> emprunt -> details...
    private Emprunt emprunt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materiel_id", nullable = false)
    private Materiel materiel;

    @Column(nullable = false)
    private Integer quantite = 1;

    // État de CE matériel précis au retour (chaque article peut avoir un état différent)
    @Enumerated(EnumType.STRING)
    private Emprunt.EtatRetour etatRetour;

    // Retour article par article : un délégué peut rendre un matériel en le scannant,
    // ou le passer à un autre délégué (transfert), sans attendre que toute la fiche soit rendue.
    private LocalDateTime dateRetour;

    @Enumerated(EnumType.STRING)
    private Emprunt.Mode modeRetour;

    @Column(columnDefinition = "TEXT")
    private String remarqueRetour;

    public DetailEmprunt() {
    }

    public DetailEmprunt(Emprunt emprunt, Materiel materiel, Integer quantite) {
        this.emprunt = emprunt;
        this.materiel = materiel;
        this.quantite = quantite;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Emprunt getEmprunt() { return emprunt; }
    public void setEmprunt(Emprunt emprunt) { this.emprunt = emprunt; }

    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public Emprunt.EtatRetour getEtatRetour() { return etatRetour; }
    public void setEtatRetour(Emprunt.EtatRetour etatRetour) { this.etatRetour = etatRetour; }

    public LocalDateTime getDateRetour() { return dateRetour; }
    public void setDateRetour(LocalDateTime dateRetour) { this.dateRetour = dateRetour; }

    public Emprunt.Mode getModeRetour() { return modeRetour; }
    public void setModeRetour(Emprunt.Mode modeRetour) { this.modeRetour = modeRetour; }

    public String getRemarqueRetour() { return remarqueRetour; }
    public void setRemarqueRetour(String remarqueRetour) { this.remarqueRetour = remarqueRetour; }

    /** Vrai si ce matériel durable a déjà été rendu (ou transféré). */
    @JsonProperty("rendu")
    public boolean isRendu() {
        return dateRetour != null;
    }

    /** Vrai pour un matériel durable encore entre les mains du délégué. */
    public boolean estDurableEnMain() {
        return materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE
                && dateRetour == null
                && emprunt.getStatutEmprunt() == Emprunt.StatutEmprunt.EN_COURS;
    }
}