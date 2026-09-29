package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cycle de vie des emprunts :
 * demande (délégué) -> validation / refus (agent) ou annulation (délégué) -> retour (agent).
 * Le matériel est réservé dès la demande, pour qu'il ne puisse pas être demandé deux fois.
 */
@Service
@Transactional
public class EmpruntService {

    private final EmpruntRepository empruntRepository;
    private final MaterielRepository materielRepository;
    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;
    private final SalleRepository salleRepository;

    public EmpruntService(EmpruntRepository empruntRepository,
                          MaterielRepository materielRepository,
                          AgentRepository agentRepository,
                          DelegueRepository delegueRepository,
                          SalleRepository salleRepository) {
        this.empruntRepository = empruntRepository;
        this.materielRepository = materielRepository;
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
        this.salleRepository = salleRepository;
    }

    // ---------- Demande (délégué) ----------

    public Emprunt demanderEmprunt(Long delegueId, String salle, LocalTime heureRetourPrevue,
                                   List<ArticleDemande> articles) {
        Delegue delegue = delegueRepository.findById(delegueId)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable (id=" + delegueId + ")."));
        if (!delegue.isActif()) {
            throw new OperationException("Votre compte est désactivé.");
        }

        String nomSalle = Verifications.obligatoire(salle, "La salle est obligatoire.");
        Salle salleConnue = salleRepository.findByNomIgnoreCase(nomSalle)
                .orElseThrow(() -> new OperationException("Salle inconnue : " + nomSalle + "."));

        if (articles == null || articles.isEmpty()) {
            throw new OperationException("Aucun matériel sélectionné pour cette demande.");
        }

        Emprunt emprunt = new Emprunt(delegue, salleConnue.getNom(), heureRetourPrevue);
        emprunt.setStatutEmprunt(StatutEmprunt.EN_ATTENTE);

        Set<Long> dejaVus = new HashSet<>();
        for (ArticleDemande article : articles) {
            if (article == null || article.materielId() == null) {
                throw new OperationException("Un matériel de la demande n'a pas d'identifiant valide.");
            }
            if (!dejaVus.add(article.materielId())) {
                throw new OperationException("Le même matériel apparaît deux fois dans la demande.");
            }

            Materiel materiel = trouverMateriel(article.materielId());
            int quantite = article.quantite() != null ? article.quantite() : 1;
            if (quantite <= 0) {
                throw new OperationException(
                        "La quantité demandée pour \"" + materiel.getDesignation() + "\" doit être supérieure à zéro.");
            }

            reserver(materiel, quantite);
            emprunt.getDetails().add(new DetailEmprunt(emprunt, materiel, quantite));
        }

        return empruntRepository.save(emprunt);
    }

    /** Le délégué annule sa propre demande tant qu'elle n'a pas été validée. */
    public Emprunt annulerDemande(Long empruntId, Long delegueId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        if (!emprunt.getDelegue().getId().equals(delegueId)) {
            throw new OperationException("Cette demande ne vous appartient pas.");
        }
        exigerStatut(emprunt, StatutEmprunt.EN_ATTENTE, "Seule une demande en attente peut être annulée.");

        libererMateriel(emprunt);
        emprunt.setStatutEmprunt(StatutEmprunt.ANNULEE);
        emprunt.setDateTraitement(LocalDateTime.now());
        return emprunt;
    }

    // ---------- Traitement (agent) ----------

    /** L'agent remet le matériel au délégué : l'emprunt commence. */
    public Emprunt validerEmprunt(Long empruntId, Long agentId) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        exigerStatut(emprunt, StatutEmprunt.EN_ATTENTE, "Cette demande a déjà été traitée.");

        emprunt.setAgentSortie(trouverAgent(agentId));
        emprunt.setDateSortie(LocalDateTime.now());
        emprunt.setStatutEmprunt(StatutEmprunt.EN_COURS);
        return emprunt;
    }

    /** L'agent refuse la demande : le matériel réservé est libéré. */
    public Emprunt refuserDemande(Long empruntId, Long agentId, String motif) {
        Emprunt emprunt = trouverEmprunt(empruntId);
        exigerStatut(emprunt, StatutEmprunt.EN_ATTENTE, "Seule une demande en attente peut être refusée.");

        libererMateriel(emprunt);
        emprunt.setStatutEmprunt(StatutEmprunt.REFUSEE);
        emprunt.setAgentRefus(trouverAgent(agentId));
        emprunt.setMotifRefus(Verifications.facultatif(motif));
        emprunt.setDateTraitement(LocalDateTime.now());
        return emprunt;
    }

    /**
     * Enregistre le retour, avec un état déclaré pour chaque article durable.
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
            Materiel materiel = detail.getMateriel();
            if (materiel.getTypeGestion() != Materiel.TypeGestion.DURABLE) {
                continue;
            }
            Emprunt.EtatRetour etat = etatParLigne.get(detail.getId());
            if (etat == null) {
                throw new OperationException(
                        "L'état de retour de \"" + materiel.getDesignation() + "\" est manquant.");
            }
            detail.setEtatRetour(etat);
            materiel.setStatut(switch (etat) {
                case BON_ETAT -> Materiel.StatutMateriel.DISPONIBLE;
                case A_VERIFIER -> Materiel.StatutMateriel.A_VERIFIER;
                case ENDOMMAGE -> Materiel.StatutMateriel.MAINTENANCE;
                case VIDE_EPUISE -> Materiel.StatutMateriel.HS;
            });
        }
        if (!lignesDeLaFiche.containsAll(etatParLigne.keySet())) {
            throw new OperationException("Une ligne de retour ne correspond pas à cet emprunt.");
        }

        emprunt.setAgentRetour(agent);
        emprunt.setDateRetour(LocalDateTime.now());
        emprunt.setObservations(Verifications.facultatif(observations));
        emprunt.setStatutEmprunt(StatutEmprunt.RETOURNE);
        return emprunt;
    }

    // ---------- Listes ----------

    @Transactional(readOnly = true)
    public List<Emprunt> listerDemandesEnAttente() {
        return empruntRepository.findByStatutEmpruntOrderByDateDemandeAsc(StatutEmprunt.EN_ATTENTE);
    }

    /** Emprunts en cours (matériel remis, pas encore rendu), les retards en premier. */
    @Transactional(readOnly = true)
    public List<Emprunt> listerEmpruntsEnCours() {
        List<Emprunt> enCours = empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt.EN_COURS);
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

    // ---------- Outils ----------

    /** Réserve le matériel au moment de la demande (durable : EMPRUNTE ; consommable : stock décrémenté). */
    private void reserver(Materiel materiel, int quantite) {
        if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
            if (quantite != 1) {
                throw new OperationException(
                        "\"" + materiel.getDesignation() + "\" est un matériel unique : quantité 1 seulement.");
            }
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
        }
    }

    /** Annule la réservation d'une demande refusée ou annulée. */
    private void libererMateriel(Emprunt emprunt) {
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

    private void exigerStatut(Emprunt emprunt, StatutEmprunt attendu, String message) {
        if (emprunt.getStatutEmprunt() != attendu) {
            throw new OperationException(message);
        }
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
