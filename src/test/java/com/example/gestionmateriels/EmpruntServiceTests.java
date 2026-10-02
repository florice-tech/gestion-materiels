package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.service.EmpruntService;
import com.example.gestionmateriels.service.OperationException;
import com.example.gestionmateriels.service.StatistiqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cycle complet d'un emprunt sur une base H2 en mémoire, à partir des données de démonstration.
 * Chaque test est annulé (rollback) à la fin : les tests sont indépendants.
 */
@SpringBootTest
@Transactional
class EmpruntServiceTests {

    @Autowired private EmpruntService empruntService;
    @Autowired private StatistiqueService statistiqueService;
    @Autowired private MaterielRepository materielRepository;
    @Autowired private DelegueRepository delegueRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private EmpruntRepository empruntRepository;

    private Delegue delegue;
    private Agent agent;
    private Materiel videoprojecteur;
    private Materiel marqueur;

    @BeforeEach
    void chargerDonnees() {
        delegue = delegueRepository.findByIdentifiantIgnoreCase("pkodjo").orElseThrow();
        agent = agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow();
        videoprojecteur = materielRepository.findByCodeUniqueIgnoreCase("VP-01").orElseThrow();
        marqueur = materielRepository.findAll().stream()
                .filter(m -> m.getDesignation().equals("Marqueur Noir")).findFirst().orElseThrow();
    }

    private Emprunt demander(int quantiteMarqueurs) {
        return empruntService.demanderEmprunt(delegue.getId(), "101", LocalTime.of(12, 0), List.of(
                new ArticleDemande(videoprojecteur.getId(), 1),
                new ArticleDemande(marqueur.getId(), quantiteMarqueurs)));
    }

    private DetailEmprunt ligneDurable(Emprunt emprunt) {
        return emprunt.getDetails().stream()
                .filter(d -> d.getMateriel().getTypeGestion() == Materiel.TypeGestion.DURABLE)
                .findFirst().orElseThrow();
    }

    @Test
    void cycleCompletDemandeValidationRetour() {
        int stockInitial = marqueur.getQuantiteStock();

        Emprunt emprunt = demander(3);
        assertEquals(StatutEmprunt.EN_ATTENTE, emprunt.getStatutEmprunt());
        assertNotNull(emprunt.getDateDemande());
        assertNull(emprunt.getDateSortie(), "pas encore sorti tant que la demande n'est pas validée");
        assertEquals(Materiel.StatutMateriel.EMPRUNTE, videoprojecteur.getStatut());
        assertEquals(stockInitial - 3, marqueur.getQuantiteStock());
        assertEquals(1, empruntService.listerDemandesEnAttente().size());
        assertTrue(empruntService.listerEmpruntsEnCours().isEmpty());

        empruntService.validerEmprunt(emprunt.getId(), agent.getId());
        assertEquals(StatutEmprunt.EN_COURS, emprunt.getStatutEmprunt());
        assertNotNull(emprunt.getDateSortie());
        assertEquals(agent.getId(), emprunt.getAgentSortie().getId());
        assertEquals(1, empruntService.listerEmpruntsEnCours().size());

        empruntService.enregistrerRetour(emprunt.getId(), agent.getId(), "Lampe faible",
                List.of(new DetailRetourRequest(ligneDurable(emprunt).getId(), Emprunt.EtatRetour.ENDOMMAGE)));

        assertEquals(StatutEmprunt.RETOURNE, emprunt.getStatutEmprunt());
        assertNotNull(emprunt.getDateRetour());
        assertEquals("Lampe faible", emprunt.getObservations());
        assertEquals(Materiel.StatutMateriel.MAINTENANCE, videoprojecteur.getStatut());
        assertEquals(stockInitial - 3, marqueur.getQuantiteStock(), "les consommables ne reviennent pas en stock");
        assertTrue(empruntService.listerEmpruntsEnCours().isEmpty());
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
    void refusConserveLaFicheEtLibereLeMateriel() {
        int stockInitial = marqueur.getQuantiteStock();
        Emprunt emprunt = demander(5);

        empruntService.refuserDemande(emprunt.getId(), agent.getId(), "Matériel réservé pour un examen");

        assertEquals(StatutEmprunt.REFUSEE, emprunt.getStatutEmprunt());
        assertEquals("Matériel réservé pour un examen", emprunt.getMotifRefus());
        assertEquals(agent.getId(), emprunt.getAgentRefus().getId());
        assertEquals(Materiel.StatutMateriel.DISPONIBLE, videoprojecteur.getStatut());
        assertEquals(stockInitial, marqueur.getQuantiteStock());
        assertTrue(empruntRepository.findById(emprunt.getId()).isPresent(), "la fiche reste dans l'historique");
    }

    @Test
    void annulationParLeDelegue() {
        int stockInitial = marqueur.getQuantiteStock();
        Emprunt emprunt = demander(2);

        empruntService.annulerDemande(emprunt.getId(), delegue.getId());

        assertEquals(StatutEmprunt.ANNULEE, emprunt.getStatutEmprunt());
        assertEquals(Materiel.StatutMateriel.DISPONIBLE, videoprojecteur.getStatut());
        assertEquals(stockInitial, marqueur.getQuantiteStock());
    }

    @Test
    void unDelegueNePeutPasAnnulerLaDemandeDUnAutre() {
        Emprunt emprunt = demander(1);
        assertThrows(OperationException.class,
                () -> empruntService.annulerDemande(emprunt.getId(), delegue.getId() + 999));
    }

    @Test
    void uneDemandeValideeNePeutPlusEtreAnnuleeNiRefusee() {
        Emprunt emprunt = demander(1);
        empruntService.validerEmprunt(emprunt.getId(), agent.getId());
        assertThrows(OperationException.class, () -> empruntService.annulerDemande(emprunt.getId(), delegue.getId()));
        assertThrows(OperationException.class, () -> empruntService.refuserDemande(emprunt.getId(), agent.getId(), null));
    }

    @Test
    void stockInsuffisantRefuse() {
        int stock = marqueur.getQuantiteStock();
        assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "101", null, List.of(new ArticleDemande(marqueur.getId(), stock + 1))));
    }

    @Test
    void materielDurableDejaEmprunteRefuse() {
        demander(1);
        assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "102", null, List.of(new ArticleDemande(videoprojecteur.getId(), 1))));
    }

    @Test
    void salleInconnueRefusee() {
        OperationException e = assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "Salle imaginaire", null, List.of(new ArticleDemande(marqueur.getId(), 1))));
        assertTrue(e.getMessage().contains("Salle inconnue"));
    }

    @Test
    void memeMaterielDeuxFoisRefuse() {
        assertThrows(OperationException.class, () -> empruntService.demanderEmprunt(
                delegue.getId(), "101", null, List.of(
                        new ArticleDemande(marqueur.getId(), 1), new ArticleDemande(marqueur.getId(), 2))));
    }

    @Test
    void retardDetecteEtCompteDansLeTableauDeBord() {
        Emprunt emprunt = demander(1);
        empruntService.validerEmprunt(emprunt.getId(), agent.getId());
        // Sorti hier, à rendre avant midi : en retard
        emprunt.setDateSortie(LocalDateTime.now().minusDays(1));
        emprunt.setHeureRetourPrevue(LocalTime.NOON);

        assertTrue(emprunt.isEnRetard());
        StatistiqueService.TableauDeBord tableau = statistiqueService.tableauDeBord(true);
        assertEquals(1, tableau.compteurs().empruntsEnCours());
        assertEquals(1, tableau.compteurs().empruntsEnRetard());
        assertEquals(7, tableau.activite7Jours().size());
        assertTrue(tableau.stocksBas().stream().anyMatch(m -> m.getDesignation().equals("Effaceur")),
                "l'effaceur (stock 5, seuil 5) est en stock bas");
    }
}
