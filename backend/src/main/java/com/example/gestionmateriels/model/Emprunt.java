package com.example.gestionmateriels.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fiche d'emprunt. Cycle de vie :
 * EN_ATTENTE (demande du délégué) -> EN_COURS (matériel remis) -> RETOURNE,
 * ou EN_ATTENTE -> REFUSEE (par un agent) / ANNULEE (par le délégué).
 * Une réservation à l'avance commence en RESERVEE et passe EN_COURS le jour où le matériel est retiré.
 * Un scan de QR code crée directement une fiche EN_COURS.
 * Les fiches ne sont jamais supprimées : elles restent dans l'historique.
 */
@Entity
@Table(name = "emprunts")
public class Emprunt {

    public enum StatutEmprunt {
        RESERVEE, EN_ATTENTE, EN_COURS, RETOURNE, REFUSEE, ANNULEE
    }

    /**
     * Comment le matériel est sorti ou revenu :
     * DEMANDE (demande en ligne remise par un agent), SCAN (QR code scanné par le délégué),
     * TRANSFERT (passé d'un délégué à un autre), RESERVATION (réservé à l'avance), AGENT (retour au poste).
     */
    public enum Mode {
        DEMANDE, SCAN, TRANSFERT, RESERVATION, AGENT
    }

    public enum EtatRetour {
        BON_ETAT, A_VERIFIER, ENDOMMAGE, VIDE_EPUISE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "delegue_id", nullable = false)
    private Delegue delegue;

    @Column(nullable = false)
    private String salle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutEmprunt statutEmprunt = StatutEmprunt.EN_ATTENTE;

    // Date de la demande par le délégué
    @Column(nullable = false, updatable = false)
    private LocalDateTime dateDemande;

    // Agent qui a remis le matériel, et quand (validation de la demande)
    @ManyToOne
    @JoinColumn(name = "agent_sortie_id")
    private Agent agentSortie;

    private LocalDateTime dateSortie;

    // Heure à laquelle le matériel doit être rendu, le jour de la sortie
    private LocalTime heureRetourPrevue;

    // Agent qui a réceptionné le matériel, et quand
    @ManyToOne
    @JoinColumn(name = "agent_retour_id")
    private Agent agentRetour;

    private LocalDateTime dateRetour;

    @Column(columnDefinition = "TEXT")
    private String observations;

    // Refus (par un agent) ou annulation (par le délégué)
    @ManyToOne
    @JoinColumn(name = "agent_refus_id")
    private Agent agentRefus;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    private LocalDateTime dateTraitement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Mode mode = Mode.DEMANDE;

    // Réservation à l'avance : jour et heure de début (l'heure de fin est heureRetourPrevue)
    private LocalDate dateReservation;

    private LocalTime heureDebut;

    // Le délégué et les agents ont déjà reçu la notification de retard
    @JsonIgnore
    @Column(nullable = false)
    private boolean rappelRetardEnvoye = false;

    // Pour un transfert : la fiche du délégué qui avait le matériel avant
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "emprunt_origine_id")
    private Emprunt empruntOrigine;

    // Matériels de la fiche, chacun avec son propre état de retour
    @OneToMany(mappedBy = "emprunt", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<DetailEmprunt> details = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (dateDemande == null) {
            dateDemande = LocalDateTime.now();
        }
    }

    public Emprunt() {
    }

    public Emprunt(Delegue delegue, String salle, LocalTime heureRetourPrevue) {
        this.delegue = delegue;
        this.salle = salle;
        this.heureRetourPrevue = heureRetourPrevue;
    }

    /** Date et heure limites de retour (le jour de la sortie ou de la réservation, à l'heure prévue), ou null. */
    @JsonProperty("echeance")
    public LocalDateTime getEcheance() {
        if (heureRetourPrevue == null) {
            return null;
        }
        if (dateSortie != null) {
            return dateSortie.toLocalDate().atTime(heureRetourPrevue);
        }
        return dateReservation != null ? dateReservation.atTime(heureRetourPrevue) : null;
    }

    /** Nom du délégué qui a transmis le matériel (transfert), sinon null. */
    @JsonProperty("transmisPar")
    public String getTransmisPar() {
        return empruntOrigine != null ? empruntOrigine.getDelegue().getNom() : null;
    }

    /** Vrai si le matériel est sorti et que l'échéance de retour est dépassée. */
    @JsonProperty("enRetard")
    public boolean isEnRetard() {
        LocalDateTime echeance = getEcheance();
        return statutEmprunt == StatutEmprunt.EN_COURS
                && echeance != null
                && LocalDateTime.now().isAfter(echeance);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Delegue getDelegue() { return delegue; }
    public void setDelegue(Delegue delegue) { this.delegue = delegue; }

    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }

    public StatutEmprunt getStatutEmprunt() { return statutEmprunt; }
    public void setStatutEmprunt(StatutEmprunt statutEmprunt) { this.statutEmprunt = statutEmprunt; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public Agent getAgentSortie() { return agentSortie; }
    public void setAgentSortie(Agent agentSortie) { this.agentSortie = agentSortie; }

    public LocalDateTime getDateSortie() { return dateSortie; }
    public void setDateSortie(LocalDateTime dateSortie) { this.dateSortie = dateSortie; }

    public LocalTime getHeureRetourPrevue() { return heureRetourPrevue; }
    public void setHeureRetourPrevue(LocalTime heureRetourPrevue) { this.heureRetourPrevue = heureRetourPrevue; }

    public Agent getAgentRetour() { return agentRetour; }
    public void setAgentRetour(Agent agentRetour) { this.agentRetour = agentRetour; }

    public LocalDateTime getDateRetour() { return dateRetour; }
    public void setDateRetour(LocalDateTime dateRetour) { this.dateRetour = dateRetour; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Agent getAgentRefus() { return agentRefus; }
    public void setAgentRefus(Agent agentRefus) { this.agentRefus = agentRefus; }

    public String getMotifRefus() { return motifRefus; }
    public void setMotifRefus(String motifRefus) { this.motifRefus = motifRefus; }

    public LocalDateTime getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(LocalDateTime dateTraitement) { this.dateTraitement = dateTraitement; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }

    public LocalDate getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDate dateReservation) { this.dateReservation = dateReservation; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public boolean isRappelRetardEnvoye() { return rappelRetardEnvoye; }
    public void setRappelRetardEnvoye(boolean rappelRetardEnvoye) { this.rappelRetardEnvoye = rappelRetardEnvoye; }

    public Emprunt getEmpruntOrigine() { return empruntOrigine; }
    public void setEmpruntOrigine(Emprunt empruntOrigine) { this.empruntOrigine = empruntOrigine; }

    public List<DetailEmprunt> getDetails() { return details; }
    public void setDetails(List<DetailEmprunt> details) { this.details = details; }
}
