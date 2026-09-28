package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

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
}