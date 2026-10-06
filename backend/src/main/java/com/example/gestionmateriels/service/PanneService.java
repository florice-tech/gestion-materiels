package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.repository.MaterielRepository;
import com.example.gestionmateriels.repository.PanneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Matériel gâté. Une panne naît :
 * - au retour au poste, quand l'agent choisit « endommagé » (en panne) ou « vide / épuisé » (hors service) ;
 * - quand un agent scanne le matériel et le déclare gâté ;
 * - quand un agent passe le matériel en maintenance ou hors service depuis le catalogue.
 * Elle reste ouverte jusqu'au bouton « Réparé », qui remet le matériel en service.
 */
@Service
@Transactional
public class PanneService {

    static final List<Panne.Statut> OUVERTES = List.of(Panne.Statut.EN_PANNE, Panne.Statut.HORS_SERVICE);

    private final PanneRepository panneRepository;
    private final MaterielRepository materielRepository;
    private final NotificationService notifications;

    public PanneService(PanneRepository panneRepository, MaterielRepository materielRepository,
                        NotificationService notifications) {
        this.panneRepository = panneRepository;
        this.materielRepository = materielRepository;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<Panne> lister() {
        return panneRepository.findAllByOrderByDateDeclarationDesc();
    }

    @Transactional(readOnly = true)
    public List<Panne> historiqueDuMateriel(Long materielId) {
        return panneRepository.findByMaterielIdOrderByDateDeclarationDesc(materielId);
    }

    /** Un agent déclare gâté un matériel (scan ou bouton) : il passe en maintenance. */
    public Panne declarer(Long materielId, String description, Panne.Origine origine, String agent) {
        Materiel materiel = trouverDurable(materielId);
        String texte = Verifications.obligatoire(description, "Décrivez ce qui ne va pas sur le matériel.");
        if (materiel.getStatut() == StatutMateriel.EMPRUNTE) {
            throw new OperationException("Ce matériel est emprunté ou réservé : il pourra être déclaré gâté à son retour.");
        }
        if (panneOuverte(materiel).isPresent()) {
            throw new OperationException("« " + materiel.getDesignation() + " » est déjà dans la liste du matériel gâté.");
        }
        Panne panne = panneRepository.save(new Panne(materiel, longueurMax(texte), origine, Panne.Statut.EN_PANNE, agent));
        materiel.setStatut(StatutMateriel.MAINTENANCE);
        notifications.notifierAgents(Categorie.INFO, "Matériel gâté",
                agent + " a déclaré gâté " + materiel.getDesignation() + " (" + materiel.getCodeUnique() + ") : " + texte,
                "pannes.html");
        return panne;
    }

    /** Retour au poste avec l'état « endommagé » ou « vide / épuisé ». */
    void depuisRetour(DetailEmprunt ligne, Emprunt.EtatRetour etat, String observations, String agent) {
        if (etat != Emprunt.EtatRetour.ENDOMMAGE && etat != Emprunt.EtatRetour.VIDE_EPUISE) {
            return;
        }
        Materiel materiel = ligne.getMateriel();
        if (panneOuverte(materiel).isPresent()) {
            return;
        }
        String description = Verifications.facultatif(observations);
        if (description == null) {
            description = Verifications.facultatif(ligne.getRemarqueRetour());
        }
        if (description == null) {
            description = etat == Emprunt.EtatRetour.ENDOMMAGE ? "Rendu endommagé" : "Rendu vide ou épuisé";
        }
        Panne panne = new Panne(materiel, longueurMax(description), Panne.Origine.RETOUR,
                etat == Emprunt.EtatRetour.ENDOMMAGE ? Panne.Statut.EN_PANNE : Panne.Statut.HORS_SERVICE, agent);
        panne.setDetailEmprunt(ligne);
        panne.setDelegueConcerne(ligne.getEmprunt().getDelegue().getNom());
        panneRepository.save(panne);
    }

    /** Bouton « Réparé » : le matériel redevient disponible. */
    public Panne reparer(Long panneId, String note, Integer cout, String agent) {
        Panne panne = trouverOuverte(panneId);
        if (cout != null && cout < 0) {
            throw new OperationException("Le coût de réparation ne peut pas être négatif.");
        }
        panne.setStatut(Panne.Statut.REPAREE);
        panne.setDateReparation(LocalDateTime.now());
        panne.setRepareePar(agent);
        panne.setNoteReparation(Verifications.facultatif(note));
        panne.setCoutReparation(cout);
        Materiel materiel = panne.getMateriel();
        if (materiel.getStatut() != StatutMateriel.EMPRUNTE) {
            materiel.setStatut(StatutMateriel.DISPONIBLE);
        }
        return panne;
    }

    /** Réparer depuis le scan d'un matériel en maintenance ou hors service. */
    public Panne reparerMateriel(Long materielId, String note, Integer cout, String agent) {
        Materiel materiel = trouverDurable(materielId);
        Panne panne = panneOuverte(materiel).orElseGet(() -> {
            if (materiel.getStatut() != StatutMateriel.MAINTENANCE && materiel.getStatut() != StatutMateriel.HS) {
                throw new OperationException("Ce matériel n'est pas déclaré gâté.");
            }
            // Panne antérieure au suivi : on la crée pour garder la trace de la réparation
            return panneRepository.save(new Panne(materiel, "Panne non décrite", Panne.Origine.CATALOGUE,
                    materiel.getStatut() == StatutMateriel.HS ? Panne.Statut.HORS_SERVICE : Panne.Statut.EN_PANNE, agent));
        });
        return reparer(panne.getId(), note, cout, agent);
    }

    /** Irréparable : le matériel passe hors service et reste dans la liste. */
    public Panne mettreHorsService(Long panneId, String note, String agent) {
        Panne panne = trouverOuverte(panneId);
        panne.setStatut(Panne.Statut.HORS_SERVICE);
        if (Verifications.facultatif(note) != null) {
            panne.setNoteReparation(note.trim());
        }
        panne.getMateriel().setStatut(StatutMateriel.HS);
        return panne;
    }

    /** Matériel « à vérifier » contrôlé par un agent : il est en bon état. */
    public Materiel verifierBonEtat(Long materielId) {
        Materiel materiel = trouverDurable(materielId);
        if (materiel.getStatut() != StatutMateriel.A_VERIFIER) {
            throw new OperationException("Ce matériel n'est pas en attente de vérification.");
        }
        materiel.setStatut(StatutMateriel.DISPONIBLE);
        return materiel;
    }

    /** Le statut a été changé à la main dans le catalogue : la liste du matériel gâté suit. */
    void surChangementStatut(Materiel materiel, StatutMateriel ancien, StatutMateriel nouveau, String agent) {
        boolean gateAvant = ancien == StatutMateriel.MAINTENANCE || ancien == StatutMateriel.HS;
        boolean gateApres = nouveau == StatutMateriel.MAINTENANCE || nouveau == StatutMateriel.HS;
        Optional<Panne> ouverte = panneOuverte(materiel);
        if (gateApres) {
            Panne.Statut statut = nouveau == StatutMateriel.HS ? Panne.Statut.HORS_SERVICE : Panne.Statut.EN_PANNE;
            if (ouverte.isPresent()) {
                ouverte.get().setStatut(statut);
            } else {
                panneRepository.save(new Panne(materiel, "Déclaré depuis le catalogue", Panne.Origine.CATALOGUE, statut, agent));
            }
        } else if (gateAvant && ouverte.isPresent()) {
            Panne panne = ouverte.get();
            panne.setStatut(Panne.Statut.REPAREE);
            panne.setDateReparation(LocalDateTime.now());
            panne.setRepareePar(agent);
            if (panne.getNoteReparation() == null) {
                panne.setNoteReparation("Remis en service depuis le catalogue");
            }
        }
    }

    @Transactional(readOnly = true)
    public long nombreOuvertes() {
        return panneRepository.countByStatut(Panne.Statut.EN_PANNE) + panneRepository.countByStatut(Panne.Statut.HORS_SERVICE);
    }

    private Optional<Panne> panneOuverte(Materiel materiel) {
        return panneRepository.findFirstByMaterielIdAndStatutInOrderByDateDeclarationDesc(materiel.getId(), OUVERTES);
    }

    private Panne trouverOuverte(Long id) {
        Panne panne = panneRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Panne introuvable."));
        if (!OUVERTES.contains(panne.getStatut())) {
            throw new OperationException("Ce matériel a déjà été réparé.");
        }
        return panne;
    }

    private Materiel trouverDurable(Long materielId) {
        Materiel materiel = materielRepository.findById(materielId)
                .orElseThrow(() -> OperationException.introuvable("Matériel introuvable."));
        if (materiel.getTypeGestion() != Materiel.TypeGestion.DURABLE) {
            throw new OperationException("Une fourniture ne se répare pas : réapprovisionnez-la si elle manque.");
        }
        return materiel;
    }

    private static String longueurMax(String texte) {
        return texte.length() > 500 ? texte.substring(0, 500) : texte;
    }
}
