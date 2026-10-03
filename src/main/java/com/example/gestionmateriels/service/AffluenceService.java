package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Heures chargées : à quels jours et quelles heures le matériel durable est le plus sorti,
 * quels équipements sont les plus tendus et quand une catégorie entière a manqué.
 * Sert à l'administration pour décider quoi acheter, chiffres à l'appui.
 */
@Service
@Transactional(readOnly = true)
public class AffluenceService {

    public static final int HEURE_DEBUT = 7, HEURE_FIN = 20; // créneaux 7h–8h ... 19h–20h
    private static final int JOURS = 6; // lundi → samedi
    private static final String[] NOMS_JOURS = {"lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"};

    /** grille[jour][heure] : nombre moyen d'équipements sortis par semaine sur ce créneau. */
    public record Affluence(int semaines, String categorie, double[][] grille, double max, String pic,
                            List<Tension> tensions, List<Manque> manques, long sorties, long demandesTransfert) {
    }

    /** Taux d'occupation d'un équipement sur les heures d'ouverture, et demandes de transfert reçues. */
    public record Tension(Long materielId, String designation, String code, int tauxOccupation, long sorties,
                          long demandesTransfert) {
    }

    /** Une catégorie dont tous les équipements étaient sortis en même temps. */
    public record Manque(String categorie, int equipements, long creneauxSatures, String exemple) {
    }

    private final EmpruntRepository empruntRepository;
    private final MaterielRepository materielRepository;
    private final TransfertRepository transfertRepository;
    private final CategorieRepository categorieRepository;

    public AffluenceService(EmpruntRepository empruntRepository, MaterielRepository materielRepository,
                            TransfertRepository transfertRepository, CategorieRepository categorieRepository) {
        this.empruntRepository = empruntRepository;
        this.materielRepository = materielRepository;
        this.transfertRepository = transfertRepository;
        this.categorieRepository = categorieRepository;
    }

    public Affluence calculer(int semainesDemandees, Long categorieId) {
        int semaines = Math.max(1, Math.min(26, semainesDemandees));
        LocalDateTime depuis = LocalDate.now().minusWeeks(semaines).atStartOfDay();
        String nomCategorie = categorieId == null ? null : categorieRepository.findById(categorieId)
                .map(Categorie::getNom).orElseThrow(() -> OperationException.introuvable("Catégorie introuvable."));

        List<Materiel> durables = materielRepository.findByTypeGestionOrderByDesignationAsc(Materiel.TypeGestion.DURABLE);
        Map<Long, Long> heuresParMateriel = new HashMap<>();
        Map<Long, Long> sortiesParMateriel = new HashMap<>();
        // (date, heure, catégorie) → équipements de la catégorie sortis en même temps
        Map<String, Integer> simultanes = new HashMap<>();
        long[][] total = new long[JOURS][HEURE_FIN - HEURE_DEBUT];
        long sorties = 0;

        for (Emprunt e : empruntRepository.findByDateSortieGreaterThanEqual(depuis)) {
            if (e.getStatutEmprunt() != StatutEmprunt.EN_COURS && e.getStatutEmprunt() != StatutEmprunt.RETOURNE) {
                continue;
            }
            for (DetailEmprunt d : e.getDetails()) {
                Materiel m = d.getMateriel();
                if (m.getTypeGestion() != Materiel.TypeGestion.DURABLE) continue;
                LocalDateTime debut = e.getDateSortie();
                LocalDateTime fin = finOccupation(e, d);
                if (!fin.isAfter(debut)) fin = debut.plusMinutes(30);
                sortiesParMateriel.merge(m.getId(), 1L, Long::sum);
                boolean compte = categorieId == null || m.getCategorie().getId().equals(categorieId);
                if (compte) sorties++;

                // Parcourt chaque créneau horaire touché par l'emprunt
                LocalDateTime h = debut.withMinute(0).withSecond(0).withNano(0);
                while (h.isBefore(fin)) {
                    int jour = h.getDayOfWeek().getValue() - 1, heure = h.getHour();
                    if (jour < JOURS && heure >= HEURE_DEBUT && heure < HEURE_FIN) {
                        heuresParMateriel.merge(m.getId(), 1L, Long::sum);
                        simultanes.merge(h.toLocalDate() + "|" + heure + "|" + m.getCategorie().getId(), 1, Integer::sum);
                        if (compte) total[jour][heure - HEURE_DEBUT]++;
                    }
                    h = h.plusHours(1);
                }
            }
        }

        double[][] grille = new double[JOURS][HEURE_FIN - HEURE_DEBUT];
        double max = 0;
        int picJour = -1, picHeure = -1;
        for (int j = 0; j < JOURS; j++) {
            for (int k = 0; k < HEURE_FIN - HEURE_DEBUT; k++) {
                grille[j][k] = Math.round(total[j][k] * 10.0 / semaines) / 10.0;
                if (grille[j][k] > max) {
                    max = grille[j][k];
                    picJour = j;
                    picHeure = k + HEURE_DEBUT;
                }
            }
        }
        String pic = picJour < 0 ? null : "Le " + NOMS_JOURS[picJour] + " entre " + picHeure + " h et " + (picHeure + 1)
                + " h : " + formater(max) + " équipement" + (max >= 2 ? "s" : "") + " sorti" + (max >= 2 ? "s" : "")
                + " en moyenne.";

        // Tensions : occupation des heures d'ouverture et demandes de transfert reçues
        long heuresOuvertes = (long) semaines * JOURS * (HEURE_FIN - HEURE_DEBUT);
        Map<Long, Long> transferts = new HashMap<>();
        long demandesTransfert = 0;
        for (Transfert t : transfertRepository.findAll()) {
            if (t.getDateDemande() != null && !t.getDateDemande().isBefore(depuis)) {
                transferts.merge(t.getMateriel().getId(), 1L, Long::sum);
                if (categorieId == null || t.getMateriel().getCategorie().getId().equals(categorieId)) demandesTransfert++;
            }
        }
        List<Tension> tensions = durables.stream()
                .filter(m -> categorieId == null || m.getCategorie().getId().equals(categorieId))
                .map(m -> new Tension(m.getId(), m.getDesignation(), m.getCodeUnique(),
                        (int) Math.min(100, Math.round(heuresParMateriel.getOrDefault(m.getId(), 0L) * 100.0 / heuresOuvertes)),
                        sortiesParMateriel.getOrDefault(m.getId(), 0L), transferts.getOrDefault(m.getId(), 0L)))
                .filter(t -> t.sorties() > 0 || t.demandesTransfert() > 0)
                .sorted(Comparator.comparingInt(Tension::tauxOccupation).reversed()
                        .thenComparing(Comparator.comparingLong(Tension::demandesTransfert).reversed()))
                .limit(8).toList();

        // Manques : créneaux où toute une catégorie était dehors
        Map<Long, Long> parCategorie = new HashMap<>();
        for (Materiel m : durables) parCategorie.merge(m.getCategorie().getId(), 1L, Long::sum);
        Map<Long, Long> saturations = new HashMap<>();
        Map<Long, String> exemples = new HashMap<>();
        for (Map.Entry<String, Integer> entree : simultanes.entrySet()) {
            String[] cle = entree.getKey().split("\\|");
            Long cat = Long.valueOf(cle[2]);
            if (entree.getValue() >= parCategorie.getOrDefault(cat, Long.MAX_VALUE)) {
                saturations.merge(cat, 1L, Long::sum);
                LocalDate jour = LocalDate.parse(cle[0]);
                exemples.merge(cat, NOMS_JOURS[Math.min(5, jour.getDayOfWeek().getValue() - 1)] + " "
                        + jour.getDayOfMonth() + "/" + jour.getMonthValue() + " à " + cle[1] + " h", (a, b) -> a);
            }
        }
        List<Manque> manques = new ArrayList<>();
        for (Map.Entry<Long, Long> s : saturations.entrySet()) {
            if (categorieId != null && !s.getKey().equals(categorieId)) continue;
            categorieRepository.findById(s.getKey()).ifPresent(c -> manques.add(new Manque(c.getNom(),
                    parCategorie.get(s.getKey()).intValue(), s.getValue(), exemples.get(s.getKey()))));
        }
        manques.sort(Comparator.comparingLong(Manque::creneauxSatures).reversed());

        return new Affluence(semaines, nomCategorie, grille, max, pic, tensions, manques, sorties, demandesTransfert);
    }

    /** Fin de l'occupation : retour réel, sinon heure prévue du jour de sortie, sinon maintenant. */
    private static LocalDateTime finOccupation(Emprunt e, DetailEmprunt d) {
        if (d.getDateRetour() != null) return d.getDateRetour();
        if (e.getDateRetour() != null) return e.getDateRetour();
        LocalDateTime maintenant = LocalDateTime.now();
        if (e.getHeureRetourPrevue() != null) {
            LocalDateTime prevue = e.getDateSortie().toLocalDate().atTime(e.getHeureRetourPrevue());
            if (prevue.isAfter(maintenant)) {
                return prevue; // encore en main, retour prévu plus tard
            }
            // En retard : occupé jusqu'à maintenant (au plus une journée de plus)
            return maintenant.isBefore(prevue.plusDays(1)) ? maintenant : prevue.plusDays(1);
        }
        return maintenant.isAfter(e.getDateSortie().toLocalDate().atTime(LocalTime.of(HEURE_FIN, 0)))
                ? e.getDateSortie().toLocalDate().atTime(LocalTime.of(HEURE_FIN, 0)) : maintenant;
    }

    private static String formater(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',');
    }
}
