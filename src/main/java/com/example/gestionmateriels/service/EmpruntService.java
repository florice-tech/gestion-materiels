package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

/**
 * Contient toute la logique métier des emprunts et retours de matériel.
 */
@Service
public class EmpruntService {

    private final EmpruntRepository empruntRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;
    private final MaterielRepository materielRepository;
    private final AgentRepository agentRepository;

    // Injection des repositories par constructeur
    public EmpruntService(EmpruntRepository empruntRepository,
                          DetailEmpruntRepository detailEmpruntRepository,
                          MaterielRepository materielRepository,
                          AgentRepository agentRepository) {
        this.empruntRepository = empruntRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
        this.materielRepository = materielRepository;
        this.agentRepository = agentRepository;
    }

    /**
     * Crée un nouvel emprunt : vérifie la disponibilité de chaque article,
     * met à jour les statuts/stocks, puis enregistre la fiche.
     * @Transactional garantit que si une vérification échoue, rien n'est enregistré.
     */
    @Transactional
    public Emprunt creerEmprunt(String delegueNom, String filiereNiveau, String salle,
                                Long agentSortieId, LocalTime heureRetourPrevue,
                                List<ArticleEmprunteDTO> articles) {

        if (articles == null || articles.isEmpty()) {
            throw new OperationException("Aucun matériel sélectionné pour cet emprunt.");
        }

        Agent agentSortie = agentRepository.findById(agentSortieId)
                .orElseThrow(() -> new OperationException("Agent introuvable (id=" + agentSortieId + ")."));

        Emprunt emprunt = new Emprunt(delegueNom, filiereNiveau, salle, agentSortie, heureRetourPrevue);
        empruntRepository.save(emprunt);

        for (ArticleEmprunteDTO article : articles) {
            Materiel materiel = materielRepository.findById(article.getMaterielId())
                    .orElseThrow(() -> new OperationException("Matériel introuvable (id=" + article.getMaterielId() + ")."));

            int quantiteDemandee = article.getQuantite() != null ? article.getQuantite() : 1;

            if (materiel.getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                // Matériel durable : on vérifie qu'il est disponible, puis on le marque "Emprunté"
                if (materiel.getStatut() != Materiel.StatutMateriel.DISPONIBLE) {
                    throw new OperationException(
                            "Le matériel \"" + materiel.getDesignation() + "\" n'est pas disponible actuellement.");
                }
                materiel.setStatut(Materiel.StatutMateriel.EMPRUNTE);
                materielRepository.save(materiel);

            } else {
                // Consommable : on vérifie le stock, puis on le décrémente
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
     * Enregistre le retour d'un emprunt : met à jour la fiche et le statut
     * de chaque matériel durable concerné, selon l'état déclaré au retour.
     */
    @Transactional
    public Emprunt enregistrerRetour(Long empruntId, Long agentRetourId,
                                     Emprunt.EtatRetour etatRetour, String observations) {

        Emprunt emprunt = empruntRepository.findById(empruntId)
                .orElseThrow(() -> new OperationException("Emprunt introuvable (id=" + empruntId + ")."));

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

        // Mise à jour du statut de chaque matériel durable emprunté dans cette fiche
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
            // Les consommables ont déjà été décomptés du stock à l'emprunt ; rien de plus à faire ici.
        }

        return emprunt;
    }

    /**
     * Liste les emprunts en cours (non encore clôturés), du plus récent au plus ancien.
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