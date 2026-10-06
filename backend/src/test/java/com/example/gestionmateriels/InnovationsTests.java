package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.ArticleDemande;
import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.repository.*;
import com.example.gestionmateriels.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Photos, numéro WhatsApp, score de confiance, QR des salles, heures chargées. */
@SpringBootTest
@Transactional
class InnovationsTests {

    private static final LocalTime TARD = LocalTime.of(23, 59);
    // Plus petite image JPEG reconnue : en-tête FF D8 FF suivi de quelques octets
    private static final String JPEG = "data:image/jpeg;base64,"
            + Base64.getEncoder().encodeToString(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'});

    @Autowired private PhotoService photoService;
    @Autowired private ScanService scanService;
    @Autowired private CompteService compteService;
    @Autowired private ConfianceService confianceService;
    @Autowired private EmpruntService empruntService;
    @Autowired private SuiviService suiviService;
    @Autowired private AffluenceService affluenceService;
    @Autowired private MaterielRepository materielRepository;
    @Autowired private DelegueRepository delegueRepository;
    @Autowired private SalleRepository salleRepository;
    @Autowired private PhotoRepository photoRepository;

    private Delegue pascal;
    private Materiel micro;

    @BeforeEach
    void preparer() {
        pascal = delegueRepository.findByIdentifiantIgnoreCase("pkodjo").orElseThrow();
        micro = materielRepository.findByCodeUniqueIgnoreCase("MIC-01").orElseThrow();
    }

    @Test
    void sixPhotosAuPlusParMateriel() {
        for (int i = 0; i < PhotoService.PHOTOS_MAX_PAR_MATERIEL; i++) {
            photoService.ajouterAuMateriel(micro.getId(), JPEG, "M. Daniel");
        }
        assertEquals(6, photoService.idsDuMateriel(micro.getId()).size());
        assertThrows(OperationException.class, () -> photoService.ajouterAuMateriel(micro.getId(), JPEG, "M. Daniel"));
        assertTrue(photoService.idsParMateriel().get(micro.getId()).size() == 6);

        photoService.supprimer(photoService.idsDuMateriel(micro.getId()).get(0));
        assertEquals(5, photoService.idsDuMateriel(micro.getId()).size());
    }

    @Test
    void uneFausseImageEstRefusee() {
        String texte = "data:image/png;base64," + Base64.getEncoder().encodeToString("bonjour".getBytes());
        assertThrows(OperationException.class, () -> photoService.ajouterAuMateriel(micro.getId(), texte, "x"));
        assertThrows(OperationException.class, () -> photoService.ajouterAuMateriel(micro.getId(), "data:text/html;base64,AAAA", "x"));
    }

    @Test
    void photoJointeAuRetourVisibleDansLeParcours() {
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        scanService.rendre("MIC-01", pascal.getId(), true, "Grésille", JPEG);
        SuiviService.Etape etape = suiviService.parcours(micro.getId()).get(0);
        assertNotNull(etape.photoRetourId());
        Long id = etape.photoRetourId();
        assertThrows(OperationException.class, () -> photoService.supprimer(id), "une photo de retour est une preuve");
    }

    @Test
    void numeroWhatsAppNormalise() {
        Delegue awa = compteService.inscrireDelegue("Awa Diallo", "L1 Droit", "adiallo", "secret12", "90 12 34 56");
        assertEquals("22890123456", awa.getTelephone());
        assertEquals("22890123456", compteService.changerTelephone(awa.getId(), "+228 90-12-34-56").getTelephone());
        assertNull(compteService.changerTelephone(awa.getId(), " ").getTelephone());
        assertThrows(OperationException.class, () -> compteService.changerTelephone(awa.getId(), "12ab"));
    }

    /** Emprunte puis rend le micro en retard (échéance placée la veille). */
    private void rendreEnRetard() {
        Emprunt e = scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        e.setDateSortie(e.getDateSortie().minusDays(1));
        scanService.rendre("MIC-01", pascal.getId(), false, null);
    }

    @Test
    void troisRetardsMettentLesReservationsEnPause() {
        assertEquals(ConfianceService.Niveau.NOUVEAU, confianceService.evaluer(pascal.getId()).niveau());
        for (int i = 0; i < 3; i++) rendreEnRetard();

        ConfianceService.Confiance c = confianceService.evaluer(pascal.getId());
        assertEquals(0, c.score());
        assertEquals(ConfianceService.Niveau.A_SURVEILLER, c.niveau());
        assertTrue(c.bloque());
        LocalDate demain = LocalDate.now().plusDays(1);
        List<ArticleDemande> articles = List.of(new ArticleDemande(micro.getId(), 1));
        assertThrows(OperationException.class, () -> empruntService.reserver(pascal.getId(), "101", demain,
                LocalTime.of(8, 0), LocalTime.of(10, 0), articles));

        confianceService.debloquer(pascal.getId());
        assertFalse(confianceService.evaluer(pascal.getId()).bloque());
        // Toujours « à surveiller » : 7 jours à l'avance au plus
        assertThrows(OperationException.class, () -> empruntService.reserver(pascal.getId(), "101",
                LocalDate.now().plusDays(10), LocalTime.of(8, 0), LocalTime.of(10, 0), articles));
        assertNotNull(empruntService.reserver(pascal.getId(), "101", demain, LocalTime.of(8, 0), LocalTime.of(10, 0), articles));
    }

    @Test
    void retoursALHeureDonnentLeBadgeFiable() {
        for (int i = 0; i < 3; i++) {
            scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
            scanService.rendre("MIC-01", pascal.getId(), false, null);
        }
        ConfianceService.Confiance c = confianceService.evaluer(pascal.getId());
        assertEquals(100, c.score());
        assertEquals(ConfianceService.Niveau.FIABLE, c.niveau());
        assertEquals(60, c.delaiReservationJours());
    }

    @Test
    void leQrDUneSalleMontreLeMaterielPresent() {
        Salle salle101 = salleRepository.findByNomIgnoreCase("101").orElseThrow();
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        SuiviService.SituationSalle s = suiviService.situationSalle(salle101.getId());
        assertEquals(1, s.materiels().size());
        assertEquals("Micro", s.materiels().get(0).designation());
        assertTrue(suiviService.situationSalle(salleRepository.findByNomIgnoreCase("102").orElseThrow().getId())
                .materiels().isEmpty());
    }

    @Test
    void heuresChargees() {
        scanService.recuperer("MIC-01", pascal.getId(), "101", TARD);
        AffluenceService.Affluence a = affluenceService.calculer(4, null);
        assertEquals(4, a.semaines());
        assertEquals(1, a.sorties());
        assertEquals(6, a.grille().length);
        assertFalse(a.tensions().isEmpty());
    }

    @Test
    void supprimerUnMaterielSupprimeSesPhotos() {
        Materiel nouveau = materielRepository.save(new Materiel("Pointeur laser", micro.getCategorie(),
                Materiel.TypeGestion.DURABLE, "LAS-01", Materiel.StatutMateriel.DISPONIBLE, 1));
        photoService.ajouterAuMateriel(nouveau.getId(), JPEG, "x");
        new MaterielServiceAppel().supprimer(nouveau.getId());
        assertEquals(0, photoRepository.countByMaterielId(nouveau.getId()));
    }

    @Autowired private MaterielService materielService;

    private class MaterielServiceAppel {
        void supprimer(Long id) {
            materielService.supprimerMateriel(id);
        }
    }
}
