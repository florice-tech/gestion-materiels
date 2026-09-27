package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * Contient toute la logique métier des emprunts et retours de matériel.
 *
 * Cycle de vie d'un emprunt :
 * 1. Le délégué fait une demande (demanderEmprunt) -> statut EN_ATTENTE, matériel réservé
 * 2. Un agent valide et remet le matériel (validerEmprunt) -> statut EN_COURS
 * 3. Un agent enregistre le retour (enregistrerRetour) -> dateRetour renseignée
 */
@Service
public class EmpruntService {

    private final EmpruntRepository empruntRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final MaterielRepository materielRepository;
    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;

    public EmpruntService(EmpruntRepository empruntRepository,
                          DetailEmpruntRepository detailEmpruntRepository,
                          MaterielRepository materielRepository,
                          AgentRepository agentRepository,
                          DelegueRepository delegueRepository) {
        this.empruntRepository = empruntRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.materielRepository = materielRepository;
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
    }

    /**
     * ÉTAPE 1 : le délégué fait sa demande en ligne.
     * Vérifie la disponibilité de chaque article et réserve immédiatement le matériel
     * (pour éviter que deux délégués demandent le même objet en même temps),
     * mais aucun agent n'est encore impliqué. Statut : EN_ATTENTE.
     */
    @Transactional
    public Emprunt demanderEmprunt(Long delegueId, String salle, LocalTime heureRetourPrevue,
                                   List<ArticleEmprunteDTO> articles) {

        if (articles == null || articles.isEmpty()) {
            throw new OperationException("Aucun matériel sélectionné pour cette demande.");
        }

        Delegue delegue = delegueRepository.findById(delegueId)
                .orElseThrow(() -> new OperationException("Délégué introuvable (id=" + delegueId + ")."));

        Emprunt emprunt = new Emprunt(delegue, salle, heureRetourPrevue);
        emprunt.setStatutEmprunt(Emprunt.StatutEmprunt.EN_ATTENTE);
        empruntRepository.save(emprunt);

        for (ArticleEmprunteDTO article : articles) {
            Materiel materiel = materielRepository.findById(article.getMaterielId())
                    .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + article.getMaterielId() + ")."));

            int quantiteDemandee = article.getQuantite() != null ? article.getQuantite() : 1;

            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                if (materiel.getStatut() != Materiel.StatutMateriel.DISPONIBLE) {
                    throw new OperationException(
                            "Le matériel \"" + materiel.getDesignation() + "\" n'est pas disponible actuellement.");
                }
                // Réservé dès la demande, pour éviter les doublons entre délégués
                materiel.setStatut(Materiel.StatutMateriel.EMPRUNTE);
                materielRepository.save(materiel);

            } else {
                if (materiel.getQuantiteStock() < quantiteDemandee) {
                    throw new OperationException(
                            "Stock insuffisant pour \"" + materiel.getDesignation() + "\" (disponible : "
                                    + materiel.getQuantiteStock() + ", demandé : " + quantiteDemandee + ").");
                }
                materiel.setQuantiteStock(materiel.getQuantiteStock() - quantiteDemandee);
                materielRepository.save(materiel);
            }

            DetailEmprunt detail = new DetailEmprunt(emprunt, materiel, quantiteDemandee);
            detailEmpruntRepository.save(detail);
        }

        return emprunt;
    }

    /**
     * ÉTAPE 2 : un agent valide la demande et remet physiquement le matériel.
     * Le matériel a déjà été réservé à l'étape 1, ici on renseigne juste qui a fait la remise.
     */
    @Transactional
    public Emprunt validerEmprunt(Long empruntId, Long agentSortieId) {

        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new OperationException("Demande introuvable (id=" + empruntId + ")."));

        if (emprunt.getStatutEmprunt() != Emprunt.StatutEmprunt.EN_ATTENTE) {
            throw new OperationException("Cette demande a déjà été traitée.");
        }

        Agent agentSortie = agentRepository.findById(agentSortieId)
                .orElseThrow(() -> new OperationException("Agent introuvable (id=" + agentSortieId + ")."));

        emprunt.setAgentSortie(agentSortie);
        emprunt.setStatutEmprunt(Emprunt.StatutEmprunt.EN_COURS);
        empruntRepository.save(emprunt);

        return emprunt;
    }

    /**
     * ÉTAPE 3 : enregistre le retour d'un emprunt validé, avec son état.
     */
    @Transactional
    public Emprunt enregistrerRetour(Long empruntId, Long agentRetourId,
                                     Emprunt.EtatRetour etatRetour, String observations) {

        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new OperationException("Emprunt introuvable (id=" + empruntId + ")."));

        if (emprunt.getStatutEmprunt() != Emprunt.StatutEmprunt.EN_COURS) {
            throw new OperationException("Cet emprunt n'est pas encore en cours (demande non validée ou déjà clôturée).");
        }
        if (emprunt.getDateRetour() != null) {
            throw new OperationException("Cet emprunt a déjà été clôturé.");
        }

        Agent agentRetour = agentRepository.findById(agentRetourId)
                .orElseThrow(() -> new OperationException("Agent introuvable (id=" + agentRetourId + ")."));

        emprunt.setAgentRetour(agentRetour);
        emprunt.setDateRetour(java.time.LocalDateTime.now());
        emprunt.setEtatRetour(etatRetour);
        emprunt.setObservations(observations);
        empruntRepository.save(emprunt);

        List<DetailEmprunt> details = detailEmpruntRepository.findByEmpruntId(empruntId);
        for (DetailEmprunt detail : details) {
            Materiel materiel = detail.getMateriel();

            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                Materiel.StatutMateriel nouveauStatut = switch (etatRetour) {
                    case BON_ETAT -> Materiel.StatutMateriel.DISPONIBLE;
                    case A_VERIFIER -> Materiel.StatutMateriel.A_VERIFIER;
                    case ENDOMMAGE -> Materiel.StatutMateriel.MAINTENANCE;
                    case VIDE_EPUICE -> Materiel.StatutMateriel.HS;
                };
                materiel.setStatut(nouveauStatut);
                materielRepository.save(materiel);
            }
        }

        return emprunt;
    }

    /**
     * Liste les demandes en attente de validation par un agent.
     */
    public List<Emprunt> listerDemandesEnAttente() {
        return empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(Emprunt.StatutEmprunt.EN_ATTENTE);
    }

    /**
     * Liste les emprunts en cours (validés, non encore rendus).
     */
    public List<Emprunt> listerEmpruntsActifs() {
        return empruntRepository.findByDateRetourIsNullOrderByDateSortieDesc();
    }

    /**
     * Liste l'historique complet des emprunts, du plus récent au plus ancien.
     */
    public List<Emprunt> listerHistorique() {
        return empruntRepository.findAllByOrderByDateSortieDesc();
    }
}