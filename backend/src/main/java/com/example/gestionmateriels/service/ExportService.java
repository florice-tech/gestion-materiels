package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Emprunt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Export de l'historique des emprunts au format CSV, lisible directement par Excel
 * (séparateur point-virgule, encodage UTF-8 avec BOM).
 */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<Emprunt.StatutEmprunt, String> LIBELLES_STATUT = Map.of(
            Emprunt.StatutEmprunt.EN_ATTENTE, "En attente",
            Emprunt.StatutEmprunt.EN_COURS, "En cours",
            Emprunt.StatutEmprunt.RETOURNE, "Rendu",
            Emprunt.StatutEmprunt.REFUSEE, "Refusée",
            Emprunt.StatutEmprunt.ANNULEE, "Annulée");

    private static final Map<Emprunt.EtatRetour, String> LIBELLES_ETAT = Map.of(
            Emprunt.EtatRetour.BON_ETAT, "bon état",
            Emprunt.EtatRetour.A_VERIFIER, "à vérifier",
            Emprunt.EtatRetour.ENDOMMAGE, "endommagé",
            Emprunt.EtatRetour.VIDE_EPUISE, "vide/épuisé");

    private final EmpruntService empruntService;

    public ExportService(EmpruntService empruntService) {
        this.empruntService = empruntService;
    }

    public String historiqueCsv() {
        StringBuilder csv = new StringBuilder("﻿");
        ligne(csv, "N°", "Statut", "Délégué", "Filière/Niveau", "Salle", "Demande", "Sortie", "Remis par",
                "Retour prévu", "En retard", "Retour", "Réceptionné par", "Matériel", "Observations / motif");

        List<Emprunt> historique = empruntService.listerHistorique();
        for (Emprunt e : historique) {
            String materiel = e.getDetails().stream().map(this::decrire).collect(Collectors.joining(", "));
            String remarque = e.getStatutEmprunt() == Emprunt.StatutEmprunt.REFUSEE
                    ? "Refus : " + (e.getMotifRefus() != null ? e.getMotifRefus() : "")
                    : e.getObservations();
            ligne(csv,
                    String.valueOf(e.getId()),
                    LIBELLES_STATUT.get(e.getStatutEmprunt()),
                    e.getDelegue().getNom(),
                    e.getDelegue().getFiliereNiveau(),
                    e.getSalle(),
                    date(e.getDateDemande()),
                    date(e.getDateSortie()),
                    e.getAgentSortie() != null ? e.getAgentSortie().getNom() : "",
                    e.getHeureRetourPrevue() != null ? e.getHeureRetourPrevue().toString().substring(0, 5) : "",
                    e.isEnRetard() ? "oui" : "",
                    date(e.getDateRetour()),
                    e.getAgentRetour() != null ? e.getAgentRetour().getNom() : "",
                    materiel,
                    remarque);
        }
        return csv.toString();
    }

    private String decrire(DetailEmprunt d) {
        StringBuilder s = new StringBuilder(d.getMateriel().getDesignation());
        if (d.getMateriel().getTypeGestion() == com.example.gestionmateriels.model.Materiel.TypeGestion.CONSOMMABLE) {
            s.append(" x").append(d.getQuantite());
        }
        if (d.getEtatRetour() != null) {
            s.append(" (").append(LIBELLES_ETAT.get(d.getEtatRetour())).append(")");
        }
        return s.toString();
    }

    private static String date(LocalDateTime date) {
        return date == null ? "" : date.format(FORMAT_DATE);
    }

    private static void ligne(StringBuilder csv, String... valeurs) {
        for (int i = 0; i < valeurs.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(echapper(valeurs[i]));
        }
        csv.append("\r\n");
    }

    /** Guillemets autour des valeurs contenant ; " ou un retour à la ligne ; neutralise les formules Excel. */
    private static String echapper(String valeur) {
        if (valeur == null) {
            return "";
        }
        String v = valeur;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
