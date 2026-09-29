package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import com.example.gestionmateriels.repository.DetailEmpruntRepository;
import com.example.gestionmateriels.repository.EmpruntRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Chiffres du tableau de bord des agents. */
@Service
@Transactional(readOnly = true)
public class StatistiqueService {

    public record Compteurs(long demandesEnAttente, long empruntsEnCours, long empruntsEnRetard,
                            long sortiesAujourdhui, long retoursAujourdhui) {
    }

    public record EtatParc(long total, long disponibles, long empruntes, long aVerifier,
                           long maintenance, long horsService) {
    }

    public record MaterielPopulaire(String designation, long nombreEmprunts, long quantiteTotale) {
    }

    public record ActiviteJour(LocalDate jour, long sorties) {
    }

    public record TableauDeBord(Compteurs compteurs, EtatParc parcDurable, List<Materiel> stocksBas,
                                List<Emprunt> retards, List<MaterielPopulaire> materielsPopulaires,
                                List<ActiviteJour> activite7Jours) {
    }

    private static final int JOURS_ACTIVITE = 7;

    private final EmpruntRepository empruntRepository;
    private final MaterielRepository materielRepository;
    private final DetailEmpruntRepository detailEmpruntRepository;

    public StatistiqueService(EmpruntRepository empruntRepository, MaterielRepository materielRepository,
                              DetailEmpruntRepository detailEmpruntRepository) {
        this.empruntRepository = empruntRepository;
        this.materielRepository = materielRepository;
        this.detailEmpruntRepository = detailEmpruntRepository;
    }

    public TableauDeBord tableauDeBord() {
        LocalDateTime debutJournee = LocalDate.now().atStartOfDay();

        List<Emprunt> enCours = empruntRepository.findByStatutEmpruntOrderByDateSortieAsc(StatutEmprunt.EN_COURS);
        List<Emprunt> retards = enCours.stream().filter(Emprunt::isEnRetard).toList();

        List<Emprunt> sortiesRecentes = empruntRepository.findByDateSortieGreaterThanEqual(
                debutJournee.minusDays(JOURS_ACTIVITE - 1));

        Compteurs compteurs = new Compteurs(
                empruntRepository.countByStatutEmprunt(StatutEmprunt.EN_ATTENTE),
                enCours.size(),
                retards.size(),
                sortiesRecentes.stream().filter(e -> !e.getDateSortie().isBefore(debutJournee)).count(),
                empruntRepository.countByDateRetourGreaterThanEqual(debutJournee));

        EtatParc parc = new EtatParc(
                materielRepository.countByTypeGestion(TypeGestion.DURABLE),
                compterDurables(StatutMateriel.DISPONIBLE),
                compterDurables(StatutMateriel.EMPRUNTE),
                compterDurables(StatutMateriel.A_VERIFIER),
                compterDurables(StatutMateriel.MAINTENANCE),
                compterDurables(StatutMateriel.HS));

        List<Materiel> stocksBas = materielRepository
                .findByTypeGestionOrderByDesignationAsc(TypeGestion.CONSOMMABLE)
                .stream().filter(Materiel::isStockBas).toList();

        List<MaterielPopulaire> populaires = new ArrayList<>();
        for (Object[] ligne : detailEmpruntRepository.classementMateriels(
                List.of(StatutEmprunt.EN_COURS, StatutEmprunt.RETOURNE), PageRequest.of(0, 5))) {
            populaires.add(new MaterielPopulaire((String) ligne[0],
                    ((Number) ligne[1]).longValue(), ((Number) ligne[2]).longValue()));
        }

        // Sorties par jour sur les 7 derniers jours (jours sans sortie compris)
        Map<LocalDate, Long> parJour = new LinkedHashMap<>();
        for (int i = JOURS_ACTIVITE - 1; i >= 0; i--) {
            parJour.put(LocalDate.now().minusDays(i), 0L);
        }
        for (Emprunt e : sortiesRecentes) {
            parJour.computeIfPresent(e.getDateSortie().toLocalDate(), (jour, n) -> n + 1);
        }
        List<ActiviteJour> activite = parJour.entrySet().stream()
                .map(entree -> new ActiviteJour(entree.getKey(), entree.getValue()))
                .toList();

        return new TableauDeBord(compteurs, parc, stocksBas, retards, populaires, activite);
    }

    private long compterDurables(StatutMateriel statut) {
        return materielRepository.countByTypeGestionAndStatut(TypeGestion.DURABLE, statut);
    }
}
