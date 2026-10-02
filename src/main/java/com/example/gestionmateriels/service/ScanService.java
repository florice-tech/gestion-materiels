package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Scan du QR code collé sur un matériel durable. Selon la situation du délégué qui scanne :
 * - matériel libre : il le récupère (l'emprunt commence aussitôt) ;
 * - il a une demande en attente ou une réservation du jour pour ce matériel : il la retire ;
 * - il l'a déjà : il le rend en indiquant son état ;
 * - un autre délégué l'a : il demande à le reprendre, l'autre accepte ou refuse (transfert).
 */
@Service
@Transactional
public class ScanService {

    public enum Action {
        RECUPERER, RETIRER_DEMANDE, RENDRE, DEMANDER_TRANSFERT, TRANSFERT_EN_ATTENTE, INDISPONIBLE, CONSULTER
    }

    public record Detenteur(Long delegueId, String nom, String filiereNiveau, String salle,
                            LocalDateTime depuis, LocalDateTime echeance, boolean enRetard) {
    }

    public record Situation(Materiel materiel, Action action, String message, Detenteur detenteur,
                            Long empruntId, Long transfertId) {
    }

    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final EmpruntRepository empruntRepository;
    private final TransfertRepository transfertRepository;
    private final EmpruntService empruntService;
    private final NotificationService notifications;

    public ScanService(MaterielRepository materielRepository, DetailEmpruntRepository detailEmpruntRepository,
                       EmpruntRepository empruntRepository, TransfertRepository transfertRepository,
                       EmpruntService empruntService, NotificationService notifications) {
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.empruntRepository = empruntRepository;
        this.transfertRepository = transfertRepository;
        this.empruntService = empruntService;
        this.notifications = notifications;
    }

    // =====================================================================
    // Situation après un scan
    // =====================================================================

    @Transactional(readOnly = true)
    public Situation situation(String code, UtilisateurConnecte moi) {
        Materiel materiel = parCode(code);
        Optional<DetailEmprunt> enMain = ligneEnMain(materiel);
        Detenteur detenteur = enMain.map(ScanService::detenteur).orElse(null);

        if (moi.estAgent()) {
            String message = detenteur != null
                    ? "Actuellement chez " + detenteur.nom() + " (salle " + detenteur.salle() + ")."
                    : libelleStatut(materiel);
            return new Situation(materiel, Action.CONSULTER, message, detenteur, null, null);
        }

        // Il l'a déjà : il peut le rendre
        if (enMain.isPresent() && enMain.get().getEmprunt().getDelegue().getId().equals(moi.getId())) {
            return new Situation(materiel, Action.RENDRE, "Vous avez ce matériel. Rendez-le en indiquant son état.",
                    detenteur, enMain.get().getEmprunt().getId(), null);
        }

        // Un autre délégué l'a : transfert
        if (enMain.isPresent()) {
            Optional<Transfert> enAttente = transfertRepository.findFirstByMaterielIdAndDemandeurIdAndStatut(
                    materiel.getId(), moi.getId(), Transfert.Statut.EN_ATTENTE);
            if (enAttente.isPresent()) {
                return new Situation(materiel, Action.TRANSFERT_EN_ATTENTE,
                        "Votre demande a été envoyée à " + detenteur.nom() + ". Vous serez prévenu dès qu'il répond.",
                        detenteur, null, enAttente.get().getId());
            }
            return new Situation(materiel, Action.DEMANDER_TRANSFERT,
                    detenteur.nom() + " a ce matériel. Vous pouvez lui demander de vous le passer.",
                    detenteur, null, null);
        }

        // Demande en attente ou réservation du jour à son nom : il la retire
        Optional<Emprunt> aRetirer = ficheARetirer(materiel, moi.getId());
        if (aRetirer.isPresent()) {
            Emprunt e = aRetirer.get();
            String quoi = e.getStatutEmprunt() == StatutEmprunt.RESERVEE ? "réservation" : "demande";
            return new Situation(materiel, Action.RETIRER_DEMANDE,
                    "Vous avez une " + quoi + " pour ce matériel : confirmez pour le retirer.",
                    null, e.getId(), null);
        }

        // Bloqué par la demande d'un autre délégué
        if (materiel.getStatut() == Materiel.StatutMateriel.EMPRUNTE) {
            return new Situation(materiel, Action.INDISPONIBLE,
                    "Ce matériel est réservé par une demande en attente d'un autre délégué.", null, null, null);
        }
        if (materiel.getStatut() != Materiel.StatutMateriel.DISPONIBLE) {
            return new Situation(materiel, Action.INDISPONIBLE, libelleStatut(materiel), null, null, null);
        }
        return new Situation(materiel, Action.RECUPERER,
                "Matériel disponible. Indiquez la salle et l'heure de retour pour le récupérer.", null, null, null);
    }

    // =====================================================================
    // Actions
    // =====================================================================

    /** Récupérer un matériel libre : l'emprunt commence immédiatement. */
    public Emprunt recuperer(String code, Long delegueId, String salle, LocalTime heureRetourPrevue) {
        Materiel materiel = parCode(code);
        Delegue delegue = empruntService.trouverDelegueActif(delegueId);
        if (heureRetourPrevue == null) {
            throw new OperationException("Indiquez l'heure à laquelle vous rendrez le matériel.");
        }
        if (!heureRetourPrevue.isAfter(LocalTime.now())) {
            throw new OperationException("L'heure de retour doit être plus tard dans la journée.");
        }
        Optional<Emprunt> aRetirer = ficheARetirer(materiel, delegueId);
        if (aRetirer.isPresent()) {
            return empruntService.retirerDemandeParScan(aRetirer.get().getId(), delegueId);
        }
        if (ligneEnMain(materiel).isPresent()) {
            throw new OperationException("Ce matériel est déjà emprunté.");
        }
        empruntService.verifierCreneauLibre(materiel, LocalDate.now(), LocalTime.now(), heureRetourPrevue, null);
        empruntService.bloquer(materiel, 1);

        Emprunt emprunt = new Emprunt(delegue, empruntService.salleConnue(salle), heureRetourPrevue);
        emprunt.setMode(Emprunt.Mode.SCAN);
        emprunt.setStatutEmprunt(StatutEmprunt.EN_COURS);
        emprunt.setDateSortie(LocalDateTime.now());
        emprunt.getDetails().add(new DetailEmprunt(emprunt, materiel, 1));
        Emprunt enregistre = empruntRepository.save(emprunt);

        notifications.notifierAgents(Categorie.INFO, "Matériel récupéré par scan",
                delegue.getNom() + " a récupéré " + materiel.getDesignation() + " (" + materiel.getCodeUnique()
                        + ") pour la salle " + enregistre.getSalle() + ", retour prévu à "
                        + heureRetourPrevue.format(EmpruntService.HEURE) + ".", "retour.html");
        return enregistre;
    }

    /** Retirer sa demande en attente ou sa réservation du jour en scannant le matériel. */
    public Emprunt retirer(String code, Long delegueId) {
        Materiel materiel = parCode(code);
        Emprunt fiche = ficheARetirer(materiel, delegueId)
                .orElseThrow(() -> new OperationException("Vous n'avez pas de demande en attente pour ce matériel."));
        return empruntService.retirerDemandeParScan(fiche.getId(), delegueId);
    }

    /** Rendre un matériel en le scannant : bon état, ou problème signalé (le matériel passe « à vérifier »). */
    public Emprunt rendre(String code, Long delegueId, boolean probleme, String remarque) {
        Materiel materiel = parCode(code);
        DetailEmprunt ligne = ligneEnMain(materiel)
                .filter(l -> l.getEmprunt().getDelegue().getId().equals(delegueId))
                .orElseThrow(() -> new OperationException("Vous n'avez pas ce matériel en votre possession."));
        if (probleme && Verifications.facultatif(remarque) == null) {
            throw new OperationException("Décrivez le problème rencontré.");
        }
        empruntService.rendreLigne(ligne, probleme ? Emprunt.EtatRetour.A_VERIFIER : Emprunt.EtatRetour.BON_ETAT,
                Emprunt.Mode.SCAN, remarque);
        annulerTransfertsEnAttente(ligne, "le matériel a été rendu");
        Emprunt emprunt = ligne.getEmprunt();
        empruntService.cloturerSiTermine(emprunt);

        notifications.notifierAgents(Categorie.RETOUR,
                probleme ? "Matériel rendu avec un problème" : "Matériel rendu par scan",
                emprunt.getDelegue().getNom() + " a rendu " + materiel.getDesignation() + " (" + materiel.getCodeUnique()
                        + ")" + (probleme ? " : " + remarque.trim() : " en bon état") + ".",
                probleme ? "materiel.html" : "historique.html");
        return emprunt;
    }

    // =====================================================================
    // Transferts entre délégués
    // =====================================================================

    public Transfert demanderTransfert(String code, Long delegueId, String salle, LocalTime heureRetourPrevue) {
        Materiel materiel = parCode(code);
        Delegue demandeur = empruntService.trouverDelegueActif(delegueId);
        DetailEmprunt ligne = ligneEnMain(materiel)
                .orElseThrow(() -> new OperationException("Ce matériel n'est plus emprunté : vous pouvez le récupérer directement."));
        Delegue detenteur = ligne.getEmprunt().getDelegue();
        if (detenteur.getId().equals(delegueId)) {
            throw new OperationException("Vous avez déjà ce matériel.");
        }
        if (transfertRepository.findFirstByMaterielIdAndDemandeurIdAndStatut(
                materiel.getId(), delegueId, Transfert.Statut.EN_ATTENTE).isPresent()) {
            throw new OperationException("Vous avez déjà demandé ce matériel ; attendez la réponse.");
        }
        if (heureRetourPrevue != null) {
            // On ignore la fiche de celui qui a le matériel : c'est justement lui qui va le passer
            empruntService.verifierCreneauLibre(materiel, LocalDate.now(), LocalTime.now(), heureRetourPrevue,
                    ligne.getEmprunt().getId());
        }
        Transfert transfert = transfertRepository.save(new Transfert(materiel, ligne, demandeur, detenteur,
                empruntService.salleConnue(salle), heureRetourPrevue));

        notifications.notifierDelegue(detenteur, Categorie.TRANSFERT, "Demande de transfert",
                demandeur.getNom() + " (" + demandeur.getFiliereNiveau() + ") souhaite récupérer "
                        + materiel.getDesignation() + " que vous avez. Acceptez si vous avez terminé.",
                "mes-emprunts.html");
        return transfert;
    }

    /** Celui qui a le matériel accepte : sa ligne est clôturée, une nouvelle fiche commence pour le demandeur. */
    public Transfert accepterTransfert(Long transfertId, Long delegueId) {
        Transfert t = transfertEnAttente(transfertId);
        if (!t.getDetenteur().getId().equals(delegueId)) {
            throw new OperationException("Seul le délégué qui a le matériel peut accepter ce transfert.");
        }
        DetailEmprunt source = t.getDetailSource();
        if (!source.estDurableEnMain()) {
            t.setStatut(Transfert.Statut.ANNULE);
            t.setDateReponse(LocalDateTime.now());
            throw new OperationException("Ce matériel n'est plus en votre possession.");
        }

        empruntService.rendreLigne(source, null, Emprunt.Mode.TRANSFERT, "Passé à " + t.getDemandeur().getNom());
        empruntService.cloturerSiTermine(source.getEmprunt());

        Emprunt nouvelle = new Emprunt(t.getDemandeur(), t.getSalle(), t.getHeureRetourPrevue());
        nouvelle.setMode(Emprunt.Mode.TRANSFERT);
        nouvelle.setStatutEmprunt(StatutEmprunt.EN_COURS);
        nouvelle.setDateSortie(LocalDateTime.now());
        nouvelle.setEmpruntOrigine(source.getEmprunt());
        nouvelle.getDetails().add(new DetailEmprunt(nouvelle, t.getMateriel(), 1));
        empruntRepository.save(nouvelle);

        t.setStatut(Transfert.Statut.ACCEPTE);
        t.setDateReponse(LocalDateTime.now());
        t.setEmpruntCree(nouvelle);
        // Les autres demandes pour ce même matériel n'ont plus d'objet
        for (Transfert autre : transfertRepository.findByDetailSourceIdAndStatut(source.getId(), Transfert.Statut.EN_ATTENTE)) {
            if (!autre.getId().equals(t.getId())) {
                autre.setStatut(Transfert.Statut.ANNULE);
                autre.setDateReponse(LocalDateTime.now());
                notifications.notifierDelegue(autre.getDemandeur(), Categorie.REFUS, "Transfert impossible",
                        t.getMateriel().getDesignation() + " a été confié à " + t.getDemandeur().getNom() + ".",
                        "mes-emprunts.html");
            }
        }

        notifications.notifierDelegue(t.getDemandeur(), Categorie.VALIDATION, "Transfert accepté",
                t.getDetenteur().getNom() + " vous passe " + t.getMateriel().getDesignation()
                        + ". Il est maintenant sous votre responsabilité.", "mes-emprunts.html");
        notifications.notifierAgents(Categorie.TRANSFERT, "Matériel transféré",
                t.getMateriel().getDesignation() + " (" + t.getMateriel().getCodeUnique() + ") est passé de "
                        + t.getDetenteur().getNom() + " à " + t.getDemandeur().getNom() + " (salle " + t.getSalle() + ").",
                "materiel.html");
        return t;
    }

    public Transfert refuserTransfert(Long transfertId, Long delegueId) {
        Transfert t = transfertEnAttente(transfertId);
        if (!t.getDetenteur().getId().equals(delegueId)) {
            throw new OperationException("Seul le délégué qui a le matériel peut refuser ce transfert.");
        }
        t.setStatut(Transfert.Statut.REFUSE);
        t.setDateReponse(LocalDateTime.now());
        notifications.notifierDelegue(t.getDemandeur(), Categorie.REFUS, "Transfert refusé",
                t.getDetenteur().getNom() + " utilise encore " + t.getMateriel().getDesignation() + ".",
                "mes-emprunts.html");
        return t;
    }

    public Transfert annulerTransfert(Long transfertId, Long delegueId) {
        Transfert t = transfertEnAttente(transfertId);
        if (!t.getDemandeur().getId().equals(delegueId)) {
            throw new OperationException("Seul le délégué qui a fait la demande peut l'annuler.");
        }
        t.setStatut(Transfert.Statut.ANNULE);
        t.setDateReponse(LocalDateTime.now());
        return t;
    }

    @Transactional(readOnly = true)
    public List<Transfert> transfertsRecus(Long delegueId) {
        return transfertRepository.findByDetenteurIdOrderByDateDemandeDesc(delegueId);
    }

    @Transactional(readOnly = true)
    public List<Transfert> transfertsEnvoyes(Long delegueId) {
        return transfertRepository.findByDemandeurIdOrderByDateDemandeDesc(delegueId);
    }

    // =====================================================================
    // Outils
    // =====================================================================

    Materiel parCode(String code) {
        String c = Verifications.obligatoire(code, "Code du matériel manquant.");
        return materielRepository.findByCodeUniqueIgnoreCase(c)
                .orElseThrow(() -> OperationException.introuvable("Aucun matériel ne porte le code « " + c + " »."));
    }

    private Optional<DetailEmprunt> ligneEnMain(Materiel materiel) {
        return detailEmpruntRepository.lignesEnMain(materiel.getId()).stream().findFirst();
    }

    /** Demande en attente, ou réservation du jour (déjà commencée ou dans les 30 minutes), du délégué pour ce matériel. */
    private Optional<Emprunt> ficheARetirer(Materiel materiel, Long delegueId) {
        LocalDate aujourdHui = LocalDate.now();
        LocalTime bientot = LocalTime.now().plusMinutes(30);
        return detailEmpruntRepository.lignesParStatut(materiel.getId(),
                        List.of(StatutEmprunt.EN_ATTENTE, StatutEmprunt.RESERVEE)).stream()
                .map(DetailEmprunt::getEmprunt)
                .filter(e -> e.getDelegue().getId().equals(delegueId))
                .filter(e -> e.getStatutEmprunt() == StatutEmprunt.EN_ATTENTE
                        || (aujourdHui.equals(e.getDateReservation()) && !e.getHeureDebut().isAfter(bientot)))
                .findFirst();
    }

    private void annulerTransfertsEnAttente(DetailEmprunt ligne, String raison) {
        for (Transfert t : transfertRepository.findByDetailSourceIdAndStatut(ligne.getId(), Transfert.Statut.EN_ATTENTE)) {
            t.setStatut(Transfert.Statut.ANNULE);
            t.setDateReponse(LocalDateTime.now());
            notifications.notifierDelegue(t.getDemandeur(), Categorie.INFO, "Matériel disponible",
                    t.getMateriel().getDesignation() + " est de nouveau libre (" + raison + ") : scannez-le pour le récupérer.",
                    "mes-emprunts.html");
        }
    }

    private Transfert transfertEnAttente(Long id) {
        Transfert t = transfertRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Demande de transfert introuvable."));
        if (t.getStatut() != Transfert.Statut.EN_ATTENTE) {
            throw new OperationException("Cette demande de transfert a déjà reçu une réponse.");
        }
        return t;
    }

    private static Detenteur detenteur(DetailEmprunt ligne) {
        Emprunt e = ligne.getEmprunt();
        return new Detenteur(e.getDelegue().getId(), e.getDelegue().getNom(), e.getDelegue().getFiliereNiveau(),
                e.getSalle(), e.getDateSortie(), e.getEcheance(), e.isEnRetard());
    }

    private static String libelleStatut(Materiel m) {
        return switch (m.getStatut()) {
            case DISPONIBLE -> "Matériel disponible.";
            case EMPRUNTE -> "Matériel réservé par une demande en attente.";
            case A_VERIFIER -> "Matériel à vérifier par un agent avant d'être prêté à nouveau.";
            case MAINTENANCE -> "Matériel en maintenance.";
            case HS -> "Matériel hors service.";
        };
    }
}
