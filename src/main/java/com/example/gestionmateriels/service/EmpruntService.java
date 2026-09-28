package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

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

    @Transactional
    public Emprunt demanderEmprunt(Long delegueId, String salle, LocalTime heureRetourPrevue,
                                   List<ArticleEmprunteDTO> articles) {

        if (delegueId == null) {
            throw new OperationException("Le délégué est obligatoire.");
        }
        if (salle == null || salle.isBlank()) {
            throw new OperationException("La salle est obligatoire.");
        }
        if (articles == null || articles.isEmpty()) {
            throw new OperationException("Aucun matériel sélectionné pour cette demande.");
        }

        Delegue delegue = delegueRepository.findById(delegueId)
                .orElseThrow(() -> new OperationException("Délégué introuvable (id=" + delegueId + ")."));

        Emprunt emprunt = new Emprunt(delegue, salle, heureRetourPrevue);
        emprunt.setStatutEmprunt(Emprunt.StatutEmprunt.EN_ATTENTE);
        empruntRepository.save(emprunt);

        for (ArticleEmprunteDTO article : articles) {
            if (article.getMaterielId() == null) {
                throw new OperationException("Un matériel de la demande n'a pas d'identifiant valide.");
            }

            Materiel materiel = materielRepository.findById(article.getMaterielId())
                    .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + article.getMaterielId() + ")."));

            int quantiteDemandee = article.getQuantite() != null ? article.getQuantite() : 1;
            if (quantiteDemandee <= 0) {
                throw new OperationException(
                        "La quantité demandée pour \"" + materiel.getDesignation() + "\" doit être supérieure à zéro.");
            }

            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                if (materiel.getStatut() != Materiel.StatutMateriel.DISPONIBLE) {
                    throw new OperationException(
                            "Le matériel \"" + materiel.getDesignation() + "\" n'est pas disponible actuellement.");
                }
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

    @Transactional
    public Emprunt validerEmprunt(Long empruntId, Long agentSortieId) {
        if (empruntId == null) {
            throw new OperationException("L'identifiant de la demande est obligatoire.");
        }
        if (agentSortieId == null) {
            throw new OperationException("L'agent qui remet le matériel est obligatoire.");
        }

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
     * Enregistre le retour, avec un état déclaré séparément pour chaque article durable de la fiche.
     * Les consommables n'exigent pas d'état (ils ne sont jamais remis en stock).
     */
    @Transactional
    public Emprunt enregistrerRetour(Long empruntId, Long agentRetourId, String observations,
                                     List<DetailRetourRequest> etatsDetails) {

        if (empruntId == null) {
            throw new OperationException("L'identifiant de l'emprunt est obligatoire.");
        }
        if (agentRetourId == null) {
            throw new OperationException("L'agent qui réceptionne est obligatoire.");
        }
        if (etatsDetails == null) {
            throw new OperationException("Aucun état n'a été renseigné pour les matériels retournés.");
        }

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

        List<DetailEmprunt> details = detailEmpruntRepository.findByEmpruntId(empruntId);

        // Vérifie qu'un état a été fourni pour chaque article DURABLE de la fiche
        for (DetailEmprunt detail : details) {
            if (detail.getMateriel().getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                boolean present = etatsDetails.stream()
                        .anyMatch(d -> detail.getId().equals(d.getDetailId()));
                if (!present) {
                    throw new OperationException(
                            "L'état de retour de \"" + detail.getMateriel().getDesignation() + "\" est manquant.");
                }
            }
        }

        for (DetailRetourRequest dto : etatsDetails) {
            if (dto.getDetailId() == null || dto.getEtatRetour() == null) {
                throw new OperationException("Chaque ligne de retour doit indiquer le matériel et son état.");
            }

            DetailEmprunt detail = detailEmpruntRepository.findById(dto.getDetailId())
                    .orElseThrow(() -> new OperationException("Ligne d'emprunt introuvable (id=" + dto.getDetailId() + ")."));

            if (!detail.getEmprunt().getId().equals(empruntId)) {
                throw new OperationException("Cette ligne ne correspond pas à cet emprunt.");
            }

            detail.setEtatRetour(dto.getEtatRetour());
            detailEmpruntRepository.save(detail);

            Materiel materiel = detail.getMateriel();
            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                Materiel.StatutMateriel nouveauStatut = switch (dto.getEtatRetour()) {
                    case BON_ETAT -> Materiel.StatutMateriel.DISPONIBLE;
                    case A_VERIFIER -> Materiel.StatutMateriel.A_VERIFIER;
                    case ENDOMMAGE -> Materiel.StatutMateriel.MAINTENANCE;
                    case VIDE_EPUISE -> Materiel.StatutMateriel.HS;
                };
                materiel.setStatut(nouveauStatut);
                materielRepository.save(materiel);
            }
        }

        emprunt.setAgentRetour(agentRetour);
        emprunt.setDateRetour(java.time.LocalDateTime.now());
        emprunt.setObservations(observations);
        empruntRepository.save(emprunt);

        return emprunt;
    }

    public List<Emprunt> listerDemandesEnAttente() {
        return empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(Emprunt.StatutEmprunt.EN_ATTENTE);
    }

    public List<Emprunt> listerEmpruntsActifs() {
        return empruntRepository.findByDateRetourIsNullOrderByDateSortieDesc();
    }

    public List<Emprunt> listerHistorique() {
        return empruntRepository.findAllByOrderByDateSortieDesc();
    }
}