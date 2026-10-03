package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.dto.DetailRetourRequest;
import com.example.gestionmateriels.dto.MaterielRequest;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Matériel gâté (pannes) et matériel manquant. */
@SpringBootTest
@Transactional
class ManquesPannesTests {

    @Autowired private EmpruntService empruntService;
    @Autowired private PanneService panneService;
    @Autowired private ManqueService manqueService;
    @Autowired private MaterielService materielService;
    @Autowired private CompteService compteService;
    @Autowired private MaterielRepository materielRepository;
    @Autowired private DelegueRepository delegueRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private CategorieRepository categorieRepository;
    @Autowired private NotificationService notificationService;

    private Delegue pascal;
    private Delegue awa;
    private Agent agent;
    private Materiel baffle;

    @BeforeEach
    void preparer() {
        pascal = delegueRepository.findByIdentifiantIgnoreCase("pkodjo").orElseThrow();
        awa = compteService.inscrireDelegue("Awa Diallo", "L1 Droit", "adiallo", "secret12");
        agent = agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow();
        baffle = materielRepository.findByCodeUniqueIgnoreCase("BAF-01").orElseThrow();
    }

    private Emprunt sortir(Materiel m) {
        Emprunt e = empruntService.demanderEmprunt(pascal.getId(), "101", LocalTime.of(23, 59),
                List.of(new ArticleDemande(m.getId(), 1)));
        empruntService.validerEmprunt(e.getId(), agent.getId());
        return e;
    }

    // ------------------------------------------------------------------ pannes

    @Test
    void retourEndommagePuisReparation() {
        Emprunt e = sortir(baffle);
        empruntService.enregistrerRetour(e.getId(), agent.getId(), "Haut-parleur percé",
                List.of(new DetailRetourRequest(e.getDetails().get(0).getId(), Emprunt.EtatRetour.ENDOMMAGE)));

        Panne panne = panneService.lister().get(0);
        assertEquals(Panne.Statut.EN_PANNE, panne.getStatut());
        assertEquals(Panne.Origine.RETOUR, panne.getOrigine());
        assertEquals("Haut-parleur percé", panne.getDescription());
        assertEquals("Pascal Kodjo", panne.getDelegueConcerne());
        assertEquals(e.getId(), panne.getEmpruntId());
        assertEquals(StatutMateriel.MAINTENANCE, baffle.getStatut());
        assertEquals(1, panneService.nombreOuvertes());

        panneService.reparer(panne.getId(), "Membrane changée", 15000, "M. Daniel");
        assertEquals(Panne.Statut.REPAREE, panne.getStatut());
        assertEquals(15000, panne.getCoutReparation());
        assertEquals(StatutMateriel.DISPONIBLE, baffle.getStatut());
        assertEquals(0, panneService.nombreOuvertes());
        assertThrows(OperationException.class, () -> panneService.reparer(panne.getId(), null, null, "M. Daniel"));
    }

    @Test
    void retourVideEpuisePasseHorsService() {
        Emprunt e = sortir(baffle);
        empruntService.enregistrerRetour(e.getId(), agent.getId(), null,
                List.of(new DetailRetourRequest(e.getDetails().get(0).getId(), Emprunt.EtatRetour.VIDE_EPUISE)));
        assertEquals(Panne.Statut.HORS_SERVICE, panneService.lister().get(0).getStatut());
        assertEquals(StatutMateriel.HS, baffle.getStatut());
    }

    @Test
    void declarerGateEnScannant() {
        assertThrows(OperationException.class, () -> panneService.declarer(baffle.getId(), " ", Panne.Origine.SCAN, "M. Daniel"));
        Panne p = panneService.declarer(baffle.getId(), "Ne s'allume plus", Panne.Origine.SCAN, "M. Daniel");
        assertEquals(StatutMateriel.MAINTENANCE, baffle.getStatut());
        assertThrows(OperationException.class, () -> panneService.declarer(baffle.getId(), "Encore", Panne.Origine.SCAN, "M. Daniel"));

        panneService.mettreHorsService(p.getId(), "Irréparable", "M. Daniel");
        assertEquals(StatutMateriel.HS, baffle.getStatut());
        panneService.reparerMateriel(baffle.getId(), "Remplacé sous garantie", null, "M. Daniel");
        assertEquals(StatutMateriel.DISPONIBLE, baffle.getStatut());

        Materiel marqueur = materielRepository.findByCodeUniqueIgnoreCase("MAR-01").orElseThrow();
        assertThrows(OperationException.class, () -> panneService.declarer(marqueur.getId(), "Sec", Panne.Origine.SCAN, "M. Daniel"));
        sortir(baffle);
        assertThrows(OperationException.class, () -> panneService.declarer(baffle.getId(), "Cassé", Panne.Origine.SCAN, "M. Daniel"));
    }

    @Test
    void leCatalogueEtLaListeDesPannesRestentAlignes() {
        materielService.changerStatut(baffle.getId(), StatutMateriel.MAINTENANCE, "M. Daniel");
        assertEquals(Panne.Origine.CATALOGUE, panneService.lister().get(0).getOrigine());
        materielService.changerStatut(baffle.getId(), StatutMateriel.DISPONIBLE, "M. Daniel");
        assertEquals(Panne.Statut.REPAREE, panneService.lister().get(0).getStatut());
        assertEquals(0, panneService.nombreOuvertes());
    }

    @Test
    void materielAVerifierControle() {
        baffle.setStatut(StatutMateriel.A_VERIFIER);
        panneService.verifierBonEtat(baffle.getId());
        assertEquals(StatutMateriel.DISPONIBLE, baffle.getStatut());
        assertThrows(OperationException.class, () -> panneService.verifierBonEtat(baffle.getId()));
    }

    // ------------------------------------------------------------------ manques

    @Test
    void demandesRepeteesPuisAjoutAuCatalogue() {
        UtilisateurConnecte sPascal = UtilisateurConnecte.depuis(pascal), sAwa = UtilisateurConnecte.depuis(awa);
        manqueService.signaler(null, "Vidéoprojecteur portable", null, 1, "101", "Pour les soutenances", sPascal);
        var deuxieme = manqueService.signaler(null, "videoprojecteurs portables", null, 2, "Amphi A", null, sAwa);
        assertEquals(2, deuxieme.getKey().getNombreDemandes());
        assertEquals(2, deuxieme.getKey().getQuantiteSouhaitee());

        // Le même délégué qui insiste le même jour ne compte qu'une fois
        manqueService.signaler(null, "Vidéoprojecteur portable", null, 1, "101", null, sPascal);
        ManqueService.Vue vue = manqueService.lister().get(0);
        assertEquals(2, vue.nombreDemandes());
        assertEquals(2, vue.nombreDelegues());
        assertEquals(1, manqueService.nombreOuverts());
        assertEquals(1, manqueService.mesSignalements(awa.getId()).size());

        Long audiovisuel = categorieRepository.findByNomIgnoreCase("AUDIOVISUEL").orElseThrow().getId();
        materielService.creerMateriel(new MaterielRequest("Vidéoprojecteur portable", audiovisuel,
                Materiel.TypeGestion.DURABLE, null, null, null));
        assertEquals(Manque.Statut.RESOLU, manqueService.lister().get(0).statut());
        assertEquals(0, manqueService.nombreOuverts());
        assertTrue(notificationService.mesNotifications(sAwa).stream().anyMatch(n -> n.getTitre().equals("Matériel disponible")));
    }

    @Test
    void stockEpuiseEntreDansLesManquesPuisRepartAuReapprovisionnement() {
        Materiel effaceur = materielRepository.findByCodeUniqueIgnoreCase("EFF-01").orElseThrow();
        empruntService.demanderEmprunt(pascal.getId(), "101", LocalTime.of(23, 59),
                List.of(new ArticleDemande(effaceur.getId(), effaceur.getQuantiteStock())));
        ManqueService.Vue vue = manqueService.lister().get(0);
        assertEquals(Manque.Origine.STOCK_EPUISE, vue.origine());
        assertEquals(effaceur.getId(), vue.materielId());

        materielService.reapprovisionner(effaceur.getId(), 10);
        assertEquals(Manque.Statut.RESOLU, manqueService.lister().get(0).statut());
    }

    @Test
    void abandonnerDemandeUnMotif() {
        var m = manqueService.signaler(null, "Imprimante 3D", null, 1, null, null, UtilisateurConnecte.depuis(pascal)).getKey();
        assertThrows(OperationException.class, () -> manqueService.abandonner(m.getId(), "", "M. Daniel"));
        manqueService.abandonner(m.getId(), "Budget non prévu cette année", "M. Daniel");
        assertEquals(Manque.Statut.ABANDONNE, m.getStatut());
        manqueService.rouvrir(m.getId());
        assertEquals(Manque.Statut.OUVERT, m.getStatut());
    }
}
