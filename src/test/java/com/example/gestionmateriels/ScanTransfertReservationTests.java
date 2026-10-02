package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** QR code (récupérer, rendre), transferts entre délégués, réservations, notifications, parcours. */
@SpringBootTest
@Transactional
class ScanTransfertReservationTests {

    private static final LocalTime TARD = LocalTime.of(23, 59);

    @Autowired private ScanService scanService;
    @Autowired private EmpruntService empruntService;
    @Autowired private NotificationService notificationService;
    @Autowired private SuiviService suiviService;
    @Autowired private CompteService compteService;
    @Autowired private TacheService tacheService;
    @Autowired private MaterielRepository materielRepository;
    @Autowired private DelegueRepository delegueRepository;
    @Autowired private AgentRepository agentRepository;

    private Delegue pascal;
    private Delegue awa;
    private Materiel micro;

    @BeforeEach
    void preparer() {
        pascal = delegueRepository.findByIdentifiantIgnoreCase("pkodjo").orElseThrow();
        awa = compteService.inscrireDelegue("Awa Diallo", "L1 Droit", "adiallo", "secret12");
        micro = materielRepository.findByCodeUniqueIgnoreCase("MIC-01").orElseThrow();
    }

    private UtilisateurConnecte session(Delegue d) {
        return UtilisateurConnecte.depuis(d);
    }

    @Test
    void scanRecupererPuisRendre() {
        assertEquals(ScanService.Action.RECUPERER, scanService.situation("mic-01", session(pascal)).action());

        Emprunt e = scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        assertEquals(StatutEmprunt.EN_COURS, e.getStatutEmprunt());
        assertEquals(Emprunt.Mode.SCAN, e.getMode());
        assertNull(e.getAgentSortie());
        assertEquals(Materiel.StatutMateriel.EMPRUNTE, micro.getStatut());
        assertEquals(ScanService.Action.RENDRE, scanService.situation("MIC-01", session(pascal)).action());

        scanService.rendre("MIC-01", pascal.getId(), false, null);
        assertEquals(StatutEmprunt.RETOURNE, e.getStatutEmprunt());
        assertEquals(Materiel.StatutMateriel.DISPONIBLE, micro.getStatut());
        assertEquals(Emprunt.Mode.SCAN, e.getDetails().get(0).getModeRetour());
    }

    @Test
    void rendreAvecProblemePasseLeMaterielAVerifier() {
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        assertThrows(OperationException.class, () -> scanService.rendre("MIC-01", pascal.getId(), true, " "));
        scanService.rendre("MIC-01", pascal.getId(), true, "Grésille");
        assertEquals(Materiel.StatutMateriel.A_VERIFIER, micro.getStatut());
        assertEquals(ScanService.Action.INDISPONIBLE, scanService.situation("MIC-01", session(awa)).action());
    }

    @Test
    void transfertAccepteChangeLeMaterielDeMain() {
        Emprunt dePascal = scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        assertEquals(ScanService.Action.DEMANDER_TRANSFERT, scanService.situation("MIC-01", session(awa)).action());

        Transfert t = scanService.demanderTransfert("MIC-01", awa.getId(), "Amphi A", TARD);
        assertEquals(ScanService.Action.TRANSFERT_EN_ATTENTE, scanService.situation("MIC-01", session(awa)).action());
        assertTrue(notificationService.nombreNonLues(session(pascal)) > 0, "Pascal est prévenu");
        assertThrows(OperationException.class, () -> scanService.accepterTransfert(t.getId(), awa.getId()),
                "seul celui qui a le matériel peut accepter");

        scanService.accepterTransfert(t.getId(), pascal.getId());
        assertEquals(Transfert.Statut.ACCEPTE, t.getStatut());
        assertEquals(StatutEmprunt.RETOURNE, dePascal.getStatutEmprunt());
        assertEquals(Emprunt.Mode.TRANSFERT, dePascal.getDetails().get(0).getModeRetour());
        Emprunt dAwa = t.getEmpruntCree();
        assertEquals(StatutEmprunt.EN_COURS, dAwa.getStatutEmprunt());
        assertEquals("Pascal Kodjo", dAwa.getTransmisPar());
        assertEquals(Materiel.StatutMateriel.EMPRUNTE, micro.getStatut());
        assertEquals(ScanService.Action.RENDRE, scanService.situation("MIC-01", session(awa)).action());

        List<SuiviService.Etape> parcours = suiviService.parcours(micro.getId());
        assertEquals(2, parcours.size());
        assertEquals("Awa Diallo", parcours.get(0).delegue());
        assertEquals("Pascal Kodjo", parcours.get(1).delegue());
    }

    @Test
    void transfertRefuse() {
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        Transfert t = scanService.demanderTransfert("MIC-01", awa.getId(), "102", null);
        scanService.refuserTransfert(t.getId(), pascal.getId());
        assertEquals(Transfert.Statut.REFUSE, t.getStatut());
        assertEquals(ScanService.Action.DEMANDER_TRANSFERT, scanService.situation("MIC-01", session(awa)).action());
    }

    @Test
    void reservationBloqueLeCreneauPourLesAutres() {
        LocalDate demain = LocalDate.now().plusDays(1);
        empruntService.reserver(pascal.getId(), "101", demain, LocalTime.of(10, 0), LocalTime.of(12, 0),
                List.of(new ArticleDemande(micro.getId(), 1)));
        assertEquals(Materiel.StatutMateriel.DISPONIBLE, micro.getStatut(), "une réservation ne bloque pas le matériel avant le jour J");

        OperationException e = assertThrows(OperationException.class, () -> empruntService.reserver(awa.getId(), "102",
                demain, LocalTime.of(11, 0), LocalTime.of(13, 0), List.of(new ArticleDemande(micro.getId(), 1))));
        assertTrue(e.getMessage().contains("réservé"));

        // Créneau qui ne chevauche pas : accepté
        empruntService.reserver(awa.getId(), "102", demain, LocalTime.of(12, 0), LocalTime.of(14, 0),
                List.of(new ArticleDemande(micro.getId(), 1)));

        List<SuiviService.Occupation> occupation = suiviService.disponibilites(demain);
        SuiviService.Occupation duMicro = occupation.stream()
                .filter(o -> o.materiel().getId().equals(micro.getId())).findFirst().orElseThrow();
        assertEquals(2, duMicro.creneaux().size());
    }

    @Test
    void reservationDuJourRetireeParScan() {
        LocalTime debut = LocalTime.now().isAfter(LocalTime.of(23, 0)) ? LocalTime.now() : LocalTime.now().plusMinutes(10);
        Emprunt r = empruntService.reserver(pascal.getId(), "101", LocalDate.now(), debut, TARD,
                List.of(new ArticleDemande(micro.getId(), 1)));
        assertEquals(ScanService.Action.RETIRER_DEMANDE, scanService.situation("MIC-01", session(pascal)).action());

        scanService.retirer("MIC-01", pascal.getId());
        assertEquals(StatutEmprunt.EN_COURS, r.getStatutEmprunt());
        assertEquals(Materiel.StatutMateriel.EMPRUNTE, micro.getStatut());
    }

    @Test
    void reservationFutureNePeutPasEtreRetiree() {
        Emprunt r = empruntService.reserver(pascal.getId(), "101", LocalDate.now().plusDays(2),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(new ArticleDemande(micro.getId(), 1)));
        Long agentId = agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow().getId();
        assertThrows(OperationException.class, () -> empruntService.validerEmprunt(r.getId(), agentId));
    }

    @Test
    void reservationInvalide() {
        assertThrows(OperationException.class, () -> empruntService.reserver(pascal.getId(), "101",
                LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0),
                List.of(new ArticleDemande(micro.getId(), 1))), "date passée");
        assertThrows(OperationException.class, () -> empruntService.reserver(pascal.getId(), "101",
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(9, 0),
                List.of(new ArticleDemande(micro.getId(), 1))), "fin avant début");
    }

    @Test
    void reservationExpireeAnnuleeAutomatiquement() {
        Emprunt r = empruntService.reserver(pascal.getId(), "101", LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), List.of(new ArticleDemande(micro.getId(), 1)));
        r.setDateReservation(LocalDate.now().minusDays(1)); // le jour est passé
        assertEquals(1, tacheService.annulerReservationsExpirees());
        assertEquals(StatutEmprunt.ANNULEE, r.getStatutEmprunt());
    }

    @Test
    void demandeImmediateEnvoieUneNotificationAuxAgents() {
        UtilisateurConnecte agent = UtilisateurConnecte.depuis(agentRepository.findByIdentifiantIgnoreCase("mguillaume").orElseThrow());
        long avant = notificationService.nombreNonLues(agent);
        empruntService.demanderEmprunt(pascal.getId(), "101", TARD, List.of(new ArticleDemande(micro.getId(), 1)));
        assertEquals(avant + 1, notificationService.nombreNonLues(agent));
        notificationService.toutMarquerLu(agent);
        assertEquals(0, notificationService.nombreNonLues(agent));
    }

    @Test
    void codeInconnu() {
        assertThrows(OperationException.class, () -> scanService.situation("XXX-99", session(pascal)));
    }

    // ---------------------------------------------------------------------
    // Un QR code pour chaque matériel, fournitures comprises
    // ---------------------------------------------------------------------

    @Autowired private MaterielService materielService;
    @Autowired private CategorieRepository categorieRepository;
    @Autowired private StatistiqueService statistiqueService;

    @Test
    void chaqueMaterielRecoitUnCode() {
        assertTrue(materielRepository.findAll().stream().allMatch(m -> m.getCodeUnique() != null && !m.getCodeUnique().isBlank()));
        Long fournitures = categorieRepository.findByNomIgnoreCase("FOURNITURES").orElseThrow().getId();
        Materiel craie = materielService.creerMateriel(new com.example.gestionmateriels.dto.MaterielRequest(
                "Craie blanche (boîte)", fournitures, Materiel.TypeGestion.CONSOMMABLE, null, 10, 2));
        assertEquals("CRA-01", craie.getCodeUnique());
        Materiel craie2 = materielService.creerMateriel(new com.example.gestionmateriels.dto.MaterielRequest(
                "Craie de couleur", fournitures, Materiel.TypeGestion.CONSOMMABLE, " ", 10, 2));
        assertEquals("CRA-02", craie2.getCodeUnique());
        Materiel ecran = materielService.creerMateriel(new com.example.gestionmateriels.dto.MaterielRequest(
                "Écran de projection", fournitures, Materiel.TypeGestion.DURABLE, null, null, null));
        assertEquals("ECR-01", ecran.getCodeUnique());
        assertThrows(OperationException.class, () -> materielService.creerMateriel(new com.example.gestionmateriels.dto.MaterielRequest(
                "Autre", fournitures, Materiel.TypeGestion.DURABLE, "mic-01", null, null)));
        assertThrows(OperationException.class, () -> materielService.creerMateriel(new com.example.gestionmateriels.dto.MaterielRequest(
                "Autre", fournitures, Materiel.TypeGestion.DURABLE, "A/B", null, null)));
    }

    @Test
    void scannerUneFournitureEnvoieUneDemandeAuPoste() {
        Materiel marqueur = materielRepository.findByCodeUniqueIgnoreCase("MAR-01").orElseThrow();
        assertEquals(ScanService.Action.DEMANDER_FOURNITURE, scanService.situation("MAR-01", session(awa)).action());
        assertThrows(OperationException.class, () -> scanService.recuperer("MAR-01", awa.getId(), "101", TARD));
        assertThrows(OperationException.class, () -> scanService.demanderFourniture("MAR-01", awa.getId(), "101", 0));

        int avant = marqueur.getQuantiteStock();
        Emprunt e = scanService.demanderFourniture("MAR-01", awa.getId(), "101", 3);
        assertEquals(StatutEmprunt.EN_ATTENTE, e.getStatutEmprunt());
        assertEquals(avant - 3, marqueur.getQuantiteStock());
    }

    @Test
    void leTableauDeBordMontreQuiAQuoi() {
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        StatistiqueService.TableauDeBord t = statistiqueService.tableauDeBord(false);
        assertEquals(1, t.compteurs().materielsSortis());
        assertEquals("Pascal Kodjo", t.enCirculation().get(0).delegue());
        assertTrue(t.activiteRecente().stream().anyMatch(ev -> ev.type().equals("SCAN")));
        assertNull(t.administration());
        assertNotNull(statistiqueService.tableauDeBord(true).administration());
    }
}
