package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.service.ArticleEmprunteDTO;
import com.example.gestionmateriels.service.EmpruntService;
import com.example.gestionmateriels.service.OperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste le cycle complet d'un emprunt sur une base H2 en mémoire,
 * à partir des données de démonstration insérées par DataInitializer.
 * Chaque test est annulé (rollback) à la fin, les tests sont donc indépendants.
 */
@SpringBootTest
@Transactional
class EmpruntServiceTests {

    @Autowired private EmpruntService empruntService;
    @Autowired private MaterielRepository materielRepository;
    @Autowired private DelegueRepository delegueRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private DetailEmpruntRepository detailEmpruntRepository;
    @Autowired private EmpruntRepository empruntRepository;

    private Delegue delegue;
    private Agent agent;
    private Materiel videoprojecteur;
    private Materiel marqueur;

    @BeforeEach
    void chargerDonnees() {
        delegue = delegueRepository.findByIdentifiant("pkodjo").orElseThrow();
        agent = agentRepository.findByIdentifiant("mdaniel").orElseThrow();
        videoprojecteur = materielRepository.findByCodeUnique("VP-01").orElseThrow();
        marqueur = materielRepository.findAll().stream()
                .filter(m -> m.getDesignation().equals("Marqueur Noir"))
                .findFirst().orElseThrow();
    }

    private Emprunt demander(int quantiteMarqueurs) {
        return empruntService.demanderEmprunt(delegue.getId(), "101", LocalTime.of(12, 0), List.of(
                new ArticleEmprunteDTO(videoprojecteur.getId(), 1),
                new ArticleEmprunteDTO(marqueur.getId(), quantiteMarqueurs)
        ));
    }

    @Test
    void cycleCompletDemandeValidationRetour() {
        int stockInitial = marqueur.getQuantiteStock();

        Emprunt emprunt = demander(3);
        assertEquals(Emprunt.StatutEmprunt.EN_ATTENTE, emprunt.getStatutEmprunt());
        assertEquals(Materiel.StatutMateriel.EMPRUNTE, videoprojecteur.getStatut());
        assertEquals(stockInitial - 3, marqueur.getQuantiteStock());
        assertEquals(1, empruntService.listerDemandesEnAttente().size());
        assertTrue(empruntService.listerEmpruntsActifs().isEmpty(), "une demande en attente n'est pas un emprunt actif");

        empruntService.validerEmprunt(emprunt.getId(), agent.getId());
        assertEquals(Emprunt.StatutEmprunt.EN_COURS, emprunt.getStatutEmprunt());
        assertEquals(1, empruntService.listerEmpruntsActifs().size());

        // Retour : un état pour le seul article durable (le vidéoprojecteur)
        List<DetailRetourRequest> etats = new ArrayList<>();
        for (DetailEmprunt detail : detailEmpruntRepository.findByEmpruntId(emprunt.getId())) {
            if (detail.getMateriel().getTypeGestion() == Materiel.TypeGestion.DURABLE) {
                DetailRetourRequest etat = new DetailRetourRequest();
                etat.setDetailId(detail.getId());
                etat.setEtatRetour(Emprunt.EtatRetour.ENDOMMAGE);
                etats.add(etat);
            }
        }
        empruntService.enregistrerRetour(emprunt.getId(), agent.getId(), "Lampe faible", etats);

        assertNotNull(emprunt.getDateRetour());
        assertEquals(Materiel.StatutMateriel.MAINTENANCE, videoprojecteur.getStatut());
        assertEquals(stockInitial - 3, marqueur.getQuantiteStock(), "les consommables ne reviennent pas en stock");
        assertTrue(empruntService.listerEmpruntsActifs().isEmpty());
        assertEquals(1, empruntService.listerEmpruntsDelegue(delegue.getId()).size());
    }

    @Test
    void retourRefuseSiEtatManquantPourUnDurable() {
        Emprunt emprunt = demander(1);
        empruntService.validerEmprunt(emprunt.getId(), agent.getId());

        OperationException e = assertThrows(OperationException.class,
                () -> empruntService.enregistrerRetour(emprunt.getId(), agent.getId(), null, List.of()));
        assertTrue(e.getMessage().contains("Vidéoprojecteur"));
    }

    @Test
    void refusDUneDemandeLibereLeMateriel() {
        int stockInitial = marqueur.getQuantiteStock();
        Emprunt emprunt = demander(5);
        Long id = emprunt.getId();

        empruntService.annulerDemande(id, null);

        assertEquals(Materiel.StatutMateriel.DISPONIBLE, videoprojecteur.getStatut());
        assertEquals(stockInitial, marqueur.getQuantiteStock());
        assertTrue(empruntRepository.findById(id).isEmpty());
        assertTrue(detailEmpruntRepository.findByEmpruntId(id).isEmpty());
    }

    @Test
    void unDelegueNePeutPasAnnulerLaDemandeDUnAutre() {
        Emprunt emprunt = demander(1);
        assertThrows(OperationException.class,
                () -> empruntService.annulerDemande(emprunt.getId(), delegue.getId() + 999));
    }

    @Test
    void uneDemandeValideeNePeutPlusEtreAnnulee() {
        Emprunt emprunt = demander(1);
        empruntService.validerEmprunt(emprunt.getId(), agent.getId());
        assertThrows(OperationException.class, () -> empruntService.annulerDemande(emprunt.getId(), null));
    }

    @Test
    void stockInsuffisantRefuse() {
        int stock = marqueur.getQuantiteStock();
        assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "101", null, List.of(new ArticleEmprunteDTO(marqueur.getId(), stock + 1))));
    }

    @Test
    void materielDurableDejaEmprunteRefuse() {
        demander(1);
        assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "102", null, List.of(new ArticleEmprunteDTO(videoprojecteur.getId(), 1))));
    }
}
