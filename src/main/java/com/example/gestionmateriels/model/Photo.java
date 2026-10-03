package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Photo stockée en base : soit une des 6 photos d'un matériel, soit la photo prise
 * par un délégué en rendant un matériel abîmé (rattachée à la ligne d'emprunt).
 * Le contenu n'est jamais renvoyé dans le JSON : il se lit par GET /api/photos/{id}.
 */
@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "materiel_id")
    private Materiel materiel;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_emprunt_id")
    private DetailEmprunt detailEmprunt;

    @Column(name = "type_mime", nullable = false, length = 40)
    private String typeMime;

    @Column(nullable = false)
    private Integer taille;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenu;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @Column(name = "ajoutee_par")
    private String ajouteePar;

    public Photo() {
    }

    public Photo(String typeMime, byte[] contenu, String ajouteePar) {
        this.typeMime = typeMime;
        this.contenu = contenu;
        this.taille = contenu.length;
        this.ajouteePar = ajouteePar;
        this.dateAjout = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Materiel getMateriel() { return materiel; }
    public void setMateriel(Materiel materiel) { this.materiel = materiel; }
    public DetailEmprunt getDetailEmprunt() { return detailEmprunt; }
    public void setDetailEmprunt(DetailEmprunt detailEmprunt) { this.detailEmprunt = detailEmprunt; }
    public String getTypeMime() { return typeMime; }
    public Integer getTaille() { return taille; }
    public byte[] getContenu() { return contenu; }
    public LocalDateTime getDateAjout() { return dateAjout; }
    public String getAjouteePar() { return ajouteePar; }
}
