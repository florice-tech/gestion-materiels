package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cycle de vie des emprunts :
 * - demande immédiate (délégué) -> validation / refus (agent) ou annulation (délégué) -> retour ;
 * - réservation à l'avance (délégué) -> retrait le jour venu (agent ou scan) -> retour ;
 * - le retour peut se faire au poste (agent, toute la fiche) ou article par article (scan, transfert).
 * Le matériel durable est bloqué dès qu'il est demandé ; une réservation ne le bloque qu'à son retrait,
 * mais personne d'autre ne peut l'emprunter sur le créneau réservé.
 */
@Service
@Transactional
public class EmpruntService {

    static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");
    static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EmpruntRepository empruntRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final MaterielRepository materielRepository;
    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;
    private final SalleRepository salleRepository;
    private final NotificationService notifications;
    private final ConfianceService confianceService;
    private final PanneService panneService;
    private final ManqueService manqueService;

    public EmpruntService(EmpruntRepository empruntRepository,
                          DetailEmpruntRepository detailEmpruntRepository,
                          MaterielRepository materielRepository,
                          AgentRepository agentRepository,
                          DelegueRepository delegueRepository,
                          SalleRepository salleRepository,
                          NotificationService notifications,
                          ConfianceService confianceService,
                          PanneService panneService,
                          ManqueService manqueService) {
        this.confianceService = confianceService;
        this.panneService = panneService;
        this.manqueService = manqueService;
        this.empruntRepository = empruntRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.materielRepository = materielRepository;
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
        this.salleRepository = salleRepository;
        this.notifications = notifications;
    }

    // =====================================================================
    // Demande immédiate (délégué)
    // =====================================================================

    public Emprunt demanderEmprunt(Long delegueId, String salle, LocalTime heureRetourPrevue,
                                   List<ArticleDemande> articles) {
        Delegue delegue = trouverDelegueActif(delegueId);
        Emprunt emprunt = new Emprunt(delegue, salleConnue(salle), heureRetourPrevue);
        emprunt.setStatutEmprunt(StatutEmprunt.EN_ATTENTE);
        emprunt.setMode(Emprunt.Mode.DEMANDE);

        for (ArticleDemande article : articlesValides(articles)) {
            Materiel materiel = trouverMateriel(article.materielId());
            int quantite = quantite(article, materiel);
            verifierCreneauLibre(materiel, LocalDate.now(), LocalTime.now(), heureRetourPrevue, null);
            bloquer(materiel, quantite);
            emprunt.getDetails().add(new DetailEmprunt(emprunt, materiel, quantite));
        }
        Emprunt enregistre = empruntRepository.save(emprunt);

        notifications.notifierAgents(Categorie.DEMANDE, "Nouvelle demande d'emprunt",
                delegue.getNom() + " (salle " + enregistre.getSalle() + ") demande : " + resume(enregistre) + ".",
                "emprunt.html");
        return enregistre;
    }

    // =====================================================================
    // Réservation à l'avance (délégué)
    // =====================================================================

    public Emprunt reserver(Long delegueId, String salle, LocalDate jour, LocalTime debut, LocalTime fin,
                            List<ArticleDemande> articles) {
        Delegue delegue = trouverDelegueActif(delegueId);
        if (jour == null || debut == null || fin == null) {
            throw new OperationException("Indiquez le jour, l'heure de début et l'heure de fin de la réservation.");
        }
        if (!fin.isAfter(debut)) {
            throw new OperationException("L'heure de fin doit être après l'heure de début.");
        }
        if (jour.isBefore(LocalDate.now()) || jour.atTime(fin).isBefore(LocalDateTime.now())) {
            throw new OperationException("Ce créneau est déjà passé.");
        }
        confianceService.verifierReservation(delegue, jour);

        Emprunt emprunt = new Emprunt(delegue, salleConnue(salle), fin);
        emprunt.setStatutEmprunt(StatutEmprunt.RESERVEE);
        emprunt.setMode(Emprunt.Mode.RESERVATION);
        emprunt.setDateReservation(jour);
        emprunt.setHeureDebut(debut);

        for (ArticleDemande article : articlesValides(articles)) {
            Materiel materiel = trouverMateriel(article.materielId());
            int quantite = quantite(article, materiel);
            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                if (materiel.getStatut() == Materiel.StatutMateriel.HS
                        || materiel.getStatut() == Materiel.StatutMateriel.MAINTENANCE) {
                    throw new OperationException("\"" + materiel.getDesignation() + "\" est hors service ou en maintenance.");
                }
                verifierCreneauLibre(materiel, jour, debut, fin, null);
            } else if (materiel.getQuantiteStock() < quantite) {
                throw new OperationException("Stock insuffisant pour \"" + materiel.getDesignation()
                        + "\" (disponible : " + materiel.getQuantiteStock() + ").");
            }
            emprunt.getDetails().add(new DetailEmprunt(emprunt, materiel, quantite));
        }
        Emprunt enregistre = empruntRepository.save(emprunt);

        notifications.notifierAgents(Categorie.RESERVATION, "Nouvelle réservation",
                delegue.getNom() + " a réservé pour le " + jour.format(JOUR) + " de " + debut.format(HEURE)
                        + " à " + fin.format(HEURE) + " (salle " + enregistre.getSalle() + ") : " + resume(enregistre) + ".",
                "emprunt.html");
        return enregistre;
    }

    /**
     * Retire une réservation le jour prévu : le matériel est bloqué et l'emprunt commence.
     * Appelé par un agent (agentId) ou par le délégué lui-même en scannant (delegueId).
     */
    public Emprunt retirerReservation(Long empruntId, Long agentId, Long delegueId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        exigerStatut(emprunt, StatutEmprunt.RESERVEE, "Cette réservation n'est plus active.");
        if (delegueId != null && !emprunt.getDelegue().getId().equals(delegueId)) {
            throw new OperationException("Cette réservation ne vous appartient pas.");
        }
        if (emprunt.getDateReservation().isAfter(LocalDate.now())) {
            throw new OperationException("Cette réservation est prévue pour le "
                    + emprunt.getDateReservation().format(JOUR) + " : le matériel ne peut pas encore être retiré.");
        }
        for (DetailEmprunt detail : emprunt.getDetails()) {
            bloquer(detail.getMateriel(), detail.getQuantite());
        }
        demarrer(emprunt, agentId != null ? trouverAgent(agentId) : null);
        return emprunt;
    }

    // =====================================================================
    // Annulation, validation, refus
    // =====================================================================

    /** Le délégué annule sa demande ou sa réservation tant que le matériel n'a pas été remis. */
    public Emprunt annulerDemande(Long empruntId, Long delegueId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (!emprunt.getDelegue().getId().equals(delegueId)) {
            throw new OperationException("Cette demande ne vous appartient pas.");
        }
        if (!estEnAttenteOuReservee(emprunt)) {
            throw new OperationException("Seule une demande en attente ou une réservation peut être annulée.");
        }
        libererSiBloque(emprunt);
        emprunt.setStatutEmprunt(StatutEmprunt.ANNULEE);
        emprunt.setDateTraitement(LocalDateTime.now());
        return emprunt;
    }

    /** L'agent remet le matériel d'une demande (ou d'une réservation du jour) : l'emprunt commence. */
    public Emprunt validerEmprunt(Long empruntId, Long agentId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (emprunt.getStatutEmprunt() == StatutEmprunt.RESERVEE) {
            return retirerReservation(empruntId, agentId, null);
        }
        exigerStatut(emprunt, StatutEmprunt.EN_ATTENTE, "Cette demande a déjà été traitée.");
        demarrer(emprunt, trouverAgent(agentId));
        return emprunt;
    }

    /** Le délégué récupère lui-même sa demande en attente en scannant le matériel. */
    public Emprunt retirerDemandeParScan(Long empruntId, Long delegueId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (!emprunt.getDelegue().getId().equals(delegueId)) {
            throw new OperationException("Cette demande ne vous appartient pas.");
        }
        if (emprunt.getStatutEmprunt() == StatutEmprunt.RESERVEE) {
            return retirerReservation(empruntId, null, delegueId);
        }
        exigerStatut(emprunt, StatutEmprunt.EN_ATTENTE, "Cette demande a déjà été traitée.");
        demarrer(emprunt, null);
        return emprunt;
    }

    /** L'agent refuse la demande ou la réservation : le matériel bloqué est libéré. */
    public Emprunt refuserDemande(Long empruntId, Long agentId, String motif) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (!estEnAttenteOuReservee(emprunt)) {
            throw new OperationException("Seule une demande en attente ou une réservation peut être refusée.");
        }
        libererSiBloque(emprunt);
        emprunt.setStatutEmprunt(StatutEmprunt.REFUSEE);
        emprunt.setAgentRefus(trouverAgent(agentId));
        emprunt.setMotifRefus(Verifications.facultatif(motif));
        emprunt.setDateTraitement(LocalDateTime.now());

        notifications.notifierDelegue(emprunt.getDelegue(), Categorie.REFUS,
                emprunt.getMode() == Emprunt.Mode.RESERVATION ? "Réservation refusée" : "Demande refusée",
                "Votre " + (emprunt.getMode() == Emprunt.Mode.RESERVATION ? "réservation" : "demande")
                        + " (" + resume(emprunt) + ") a été refusée"
                        + (emprunt.getMotifRefus() != null ? " : " + emprunt.getMotifRefus() : ".") ,
                "mes-emprunts.html");
        return emprunt;
    }

    // =====================================================================
    // Retours
    // =====================================================================

    /**
     * Retour au poste : un état pour chaque article durable encore en main.
     * Les consommables ne reviennent pas en stock : aucun état n'est demandé pour eux.
     */
    public Emprunt enregistrerRetour(Long empruntId, Long agentId, String observations,
                                     List<DetailRetourRequest> etats) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        exigerStatut(emprunt, StatutEmprunt.EN_COURS,
                "Cet emprunt n'est pas en cours (demande non validée ou matériel déjà rendu).");
        Agent agent = trouverAgent(agentId);

        Map<Long, Emprunt.EtatRetour> etatParLigne = new HashMap<>();
        if (etats != null) {
            for (DetailRetourRequest etat : etats) {
                if (etat == null || etat.detailId() == null || etat.etatRetour() == null) {
                    throw new OperationException("Chaque ligne de retour doit indiquer le matériel et son état.");
                }
                etatParLigne.put(etat.detailId(), etat.etatRetour());
            }
        }

        Set<Long> lignesDeLaFiche = new HashSet<>();
        for (DetailEmprunt detail : emprunt.getDetails()) {
            lignesDeLaFiche.add(detail.getId());
            if (detail.getMateriel().getTypeGestion() != Materiel.TypeGestion.DURABLE || detail.isRendu()) {
                continue;
            }
            Emprunt.EtatRetour etat = etatParLigne.get(detail.getId());
            if (etat == null) {
                throw new OperationException(
                        "L'état de retour de \"" + detail.getMateriel().getDesignation() + "\" est manquant.");
            }
            rendreLigne(detail, etat, Emprunt.Mode.AGENT, null);
            // Endommagé ou vide / épuisé : le matériel passe dans « Matériel gâté »
            panneService.depuisRetour(detail, etat, observations, agent.getNom());
        }
        if (!lignesDeLaFiche.containsAll(etatParLigne.keySet())) {
            throw new OperationException("Une ligne de retour ne correspond pas à cet emprunt.");
        }

        emprunt.setAgentRetour(agent);
        emprunt.setObservations(Verifications.facultatif(observations));
        cloturer(emprunt);
        return emprunt;
    }

    /**
     * Rend un seul article (scan du délégué ou transfert). La fiche est clôturée
     * quand plus aucun matériel durable n'est en main.
     */
    void rendreLigne(DetailEmprunt detail, Emprunt.EtatRetour etat, Emprunt.Mode mode, String remarque) {
        detail.setDateRetour(LocalDateTime.now());
        detail.setModeRetour(mode);
        detail.setEtatRetour(etat);
        detail.setRemarqueRetour(Verifications.facultatif(remarque));
        if (mode != Emprunt.Mode.TRANSFERT && etat != null) {
            detail.getMateriel().setStatut(switch (etat) {
                case BON_ETAT -> Materiel.StatutMateriel.DISPONIBLE;
                case A_VERIFIER -> Materiel.StatutMateriel.A_VERIFIER;
                case ENDOMMAGE -> Materiel.StatutMateriel.MAINTENANCE;
                case VIDE_EPUISE -> Materiel.StatutMateriel.HS;
            });
        }
    }

    /** Clôture la fiche si tous ses durables sont rendus. */
    void cloturerSiTermine(Emprunt emprunt) {
        boolean resteEnMain = emprunt.getDetails().stream()
                .anyMatch(d -> d.getMateriel().getTypeGestion() == Materiel.TypeGestion.DURABLE && !d.isRendu());
        if (!resteEnMain) {
            cloturer(emprunt);
        }
    }

    private void cloturer(Emprunt emprunt) {
        emprunt.setDateRetour(LocalDateTime.now());
        emprunt.setStatutEmprunt(StatutEmprunt.RETOURNE);
    }

    // =====================================================================
    // Listes
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Emprunt> listerDemandesEnAttente() {
        return empruntRepository.findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt.EN_ATTENTE);
    }

    @Transactional(readOnly = true)
    public List<Emprunt> listerReservationsAVenir() {
        return empruntRepository.findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt.RESERVEE).stream()
                .sorted(Comparator.comparing(Emprunt::getDateReservation).thenComparing(Emprunt::getHeureDebut))
                .toList();
    }

    /** Emprunts en cours (matériel remis, pas encore rendu), les retards en premier. */
    @Transactional(readOnly = true)
    public List<Emprunt> listerEmpruntsEnCours() {
        List<Emprunt> enCours = new ArrayList<>(
                empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt.EN_COURS));
        enCours.sort((a, b) -> Boolean.compare(b.isEnRetard(), a.isEnRetard()));
        return enCours;
    }

    @Transactional(readOnly = true)
    public List<Emprunt> listerHistorique() {
        return empruntRepository.findAllByOrderByDateDemandeDesc();
    }

    @Transactional(readOnly = true)
    public List<Emprunt> listerEmpruntsDelegue(Long delegueId) {
        return empruntRepository.findByDelegueIdOrderByDateDemandeDesc(delegueId);
    }

    /** Une fiche, pour le bon d'emprunt : visible par les agents et par le délégué concerné. */
    @Transactional(readOnly = true)
    public Emprunt consulter(Long empruntId, Long delegueId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (delegueId != null && !emprunt.getDelegue().getId().equals(delegueId)) {
            throw OperationException.introuvable("Emprunt introuvable (id=" + empruntId + ").");
        }
        return emprunt;
    }

    // =====================================================================
    // Disponibilité des créneaux
    // =====================================================================

    /**
     * Empêche d'emprunter ou de réserver un matériel durable sur un créneau déjà réservé par une autre fiche.
     * Une heure de fin absente signifie « jusqu'à la fin de la journée ».
     */
    void verifierCreneauLibre(Materiel materiel, LocalDate jour, LocalTime debut, LocalTime fin, Long empruntIgnore) {
        if (materiel.getTypeGestion() != Materiel.TypeGestion.DURABLE) {
            return;
        }
        LocalTime finEffective = fin != null ? fin : LocalTime.MAX;
        for (DetailEmprunt ligne : detailEmpruntRepository.lignesParStatut(materiel.getId(), List.of(StatutEmprunt.RESERVEE))) {
            Emprunt autre = ligne.getEmprunt();
            if (autre.getId().equals(empruntIgnore) || !jour.equals(autre.getDateReservation())) {
                continue;
            }
            LocalTime autreFin = autre.getHeureRetourPrevue() != null ? autre.getHeureRetourPrevue() : LocalTime.MAX;
            boolean chevauche = autre.getHeureDebut().isBefore(finEffective) && debut.isBefore(autreFin);
            if (chevauche) {
                throw new OperationException("\"" + materiel.getDesignation() + "\" est réservé le "
                        + jour.format(JOUR) + " de " + autre.getHeureDebut().format(HEURE) + " à "
                        + autreFin.format(HEURE) + " par " + autre.getDelegue().getNom()
                        + ". Choisissez un autre créneau ou un autre matériel.");
            }
        }
        // Réservation pour aujourd'hui sur un matériel déjà sorti : il doit être rendu avant le début
        if (jour.equals(LocalDate.now()) && materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
            for (DetailEmprunt ligne : detailEmpruntRepository.lignesParStatut(materiel.getId(),
                    List.of(StatutEmprunt.EN_COURS, StatutEmprunt.EN_ATTENTE))) {
                Emprunt autre = ligne.getEmprunt();
                if (autre.getId().equals(empruntIgnore) || ligne.isRendu()) {
                    continue;
                }
                LocalTime autreFin = autre.getHeureRetourPrevue() != null ? autre.getHeureRetourPrevue() : LocalTime.MAX;
                if (debut.isBefore(autreFin)) {
                    throw new OperationException("\"" + materiel.getDesignation() + "\" est emprunté par "
                            + autre.getDelegue().getNom() + " jusqu'à " + autreFin.format(HEURE) + ".");
                }
            }
        }
    }

    // =====================================================================
    // Outils
    // =====================================================================

    private void demarrer(Emprunt emprunt, Agent agent) {
        emprunt.setAgentSortie(agent);
        emprunt.setDateSortie(LocalDateTime.now());
        emprunt.setStatutEmprunt(StatutEmprunt.EN_COURS);
        if (agent != null) {
            notifications.notifierDelegue(emprunt.getDelegue(), Categorie.VALIDATION, "Matériel remis",
                    agent.getNom() + " vous a remis : " + resume(emprunt)
                            + (emprunt.getHeureRetourPrevue() != null
                            ? ". À rendre avant " + emprunt.getHeureRetourPrevue().format(HEURE) + "." : "."),
                    "mes-emprunts.html");
        } else {
            notifications.notifierAgents(Categorie.INFO, "Matériel retiré par scan",
                    emprunt.getDelegue().getNom() + " a retiré " + resume(emprunt) + " (salle " + emprunt.getSalle() + ").",
                    "retour.html");
        }
    }

    /** Bloque le matériel (durable : EMPRUNTE ; consommable : stock décrémenté). */
    void bloquer(Materiel materiel, int quantite) {
        if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
            if (materiel.getStatut() != Materiel.StatutMateriel.DISPONIBLE) {
                throw new OperationException(
                        "Le matériel \"" + materiel.getDesignation() + "\" n'est pas disponible actuellement.");
            }
            materiel.setStatut(Materiel.StatutMateriel.EMPRUNTE);
        } else {
            if (materiel.getQuantiteStock() < quantite) {
                throw new OperationException(
                        "Stock insuffisant pour \"" + materiel.getDesignation() + "\" (disponible : "
                                + materiel.getQuantiteStock() + ", demandé : " + quantite + ").");
            }
            materiel.setQuantiteStock(materiel.getQuantiteStock() - quantite);
            if (materiel.getQuantiteStock() == 0) {
                manqueService.stockEpuise(materiel);
            }
        }
    }

    /** Libère le matériel d'une demande refusée ou annulée (une réservation n'a rien bloqué). */
    private void libererSiBloque(Emprunt emprunt) {
        if (emprunt.getStatutEmprunt() != StatutEmprunt.EN_ATTENTE) {
            return;
        }
        for (DetailEmprunt detail : emprunt.getDetails()) {
            Materiel materiel = detail.getMateriel();
            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                if (materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
                    materiel.setStatut(Materiel.StatutMateriel.DISPONIBLE);
                }
            } else {
                materiel.setQuantiteStock(materiel.getQuantiteStock() + detail.getQuantite());
            }
        }
    }

    private List<ArticleDemande> articlesValides(List<ArticleDemande> articles) {
        if (articles == null || articles.isEmpty()) {
            throw new OperationException("Aucun matériel sélectionné pour cette demande.");
        }
        Set<Long> dejaVus = new HashSet<>();
        for (ArticleDemande article : articles) {
            if (article == null || article.materielId() == null) {
                throw new OperationException("Un matériel de la demande n'a pas d'identifiant valide.");
            }
            if (!dejaVus.add(article.materielId())) {
                throw new OperationException("Le même matériel apparaît deux fois dans la demande.");
            }
        }
        return articles;
    }

    private int quantite(ArticleDemande article, Materiel materiel) {
        int quantite = article.quantite() != null ? article.quantite() : 1;
        if (quantite <= 0) {
            throw new OperationException(
                    "La quantité demandée pour \"" + materiel.getDesignation() + "\" doit être supérieure à zéro.");
        }
        if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE && quantite != 1) {
            throw new OperationException(
                    "\"" + materiel.getDesignation() + "\" est un matériel unique : quantité 1 seulement.");
        }
        return quantite;
    }

    String salleConnue(String salle) {
        String nomSalle = Verifications.obligatoire(salle, "La salle est obligatoire.");
        return salleRepository.findByNomIgnoreCase(nomSalle)
                .orElseThrow(() -> new OperationException("Salle inconnue : " + nomSalle + "."))
                .getNom();
    }

    static String resume(Emprunt emprunt) {
        return emprunt.getDetails().stream()
                .map(d -> d.getMateriel().getDesignation()
                        + (d.getMateriel().getTypeGestion() == Materiel.TypeGestion.CONSOMMABLE ? " ×" + d.getQuantite() : ""))
                .collect(Collectors.joining(", "));
    }

    private static boolean estEnAttenteOuReservee(Emprunt emprunt) {
        return emprunt.getStatutEmprunt() == StatutEmprunt.EN_ATTENTE
                || emprunt.getStatutEmprunt() == StatutEmprunt.RESERVEE;
    }

    private void exigerStatut(Emprunt emprunt, StatutEmprunt attendu, String message) {
        if (emprunt.getStatutEmprunt() != attendu) {
            throw new OperationException(message);
        }
    }

    Delegue trouverDelegueActif(Long delegueId) {
        Delegue delegue = delegueRepository.findById(delegueId)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable (id=" + delegueId + ")."));
        if (!delegue.isActif()) {
            throw new OperationException("Votre compte est désactivé.");
        }
        return delegue;
    }

    private Emprunt trouverEmprunt(Long id) {
        if (id == null) {
            throw new OperationException("L'identifiant de l'emprunt est obligatoire.");
        }
        return empruntRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Emprunt introuvable (id=" + id + ")."));
    }

    private Materiel trouverMateriel(Long id) {
        return materielRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Matériel introuvable (id=" + id + ")."));
    }

    private Agent trouverAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Agent introuvable (id=" + id + ")."));
        if (!agent.isActif()) {
            throw new OperationException("Ce compte agent est désactivé.");
        }
        return agent;
    }
}
