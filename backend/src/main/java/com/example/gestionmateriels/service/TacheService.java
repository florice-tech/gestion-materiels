package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Emprunt;
import com.example.gestionmateriels.model.Emprunt.StatutEmprunt;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.repository.EmpruntRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tâches automatiques, toutes les 5 minutes :
 * - prévenir le délégué et les agents d'un retard (une seule fois par fiche) ;
 * - annuler les réservations dont le jour est passé sans que le matériel ait été retiré.
 */
@Service
public class TacheService {

    private static final Logger LOG = LoggerFactory.getLogger(TacheService.class);

    private final EmpruntRepository empruntRepository;
    private final NotificationService notifications;

    public TacheService(EmpruntRepository empruntRepository, NotificationService notifications) {
        this.empruntRepository = empruntRepository;
        this.notifications = notifications;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    @Transactional
    public void executer() {
        int retards = signalerRetards();
        int expirees = annulerReservationsExpirees();
        if (retards + expirees > 0) {
            LOG.info("Tâches automatiques : {} retard(s) signalé(s), {} réservation(s) expirée(s).", retards, expirees);
        }
    }

    @Transactional
    public int signalerRetards() {
        int n = 0;
        for (Emprunt e : empruntRepository.findByStatutEmpruntAndRappelRetardEnvoyeFalse(StatutEmprunt.EN_COURS)) {
            if (!e.isEnRetard()) {
                continue;
            }
            e.setRappelRetardEnvoye(true);
            String quoi = EmpruntService.resume(e);
            String echeance = e.getEcheance().format(EmpruntService.HEURE);
            notifications.notifierDelegue(e.getDelegue(), Categorie.RETARD, "Matériel à rendre",
                    "Le retour de " + quoi + " était prévu à " + echeance + ". Merci de le rapporter au poste de surveillance.",
                    "mes-emprunts.html");
            notifications.notifierAgents(Categorie.RETARD, "Retard de retour",
                    e.getDelegue().getNom() + " (salle " + e.getSalle() + ") devait rendre " + quoi + " à " + echeance + ".",
                    "retour.html");
            n++;
        }
        return n;
    }

    @Transactional
    public int annulerReservationsExpirees() {
        int n = 0;
        for (Emprunt e : empruntRepository.findByStatutEmpruntAndDateReservationBefore(StatutEmprunt.RESERVEE, LocalDate.now())) {
            e.setStatutEmprunt(StatutEmprunt.ANNULEE);
            e.setMotifRefus("Réservation non retirée le jour prévu.");
            e.setDateTraitement(LocalDateTime.now());
            notifications.notifierDelegue(e.getDelegue(), Categorie.INFO, "Réservation expirée",
                    "Votre réservation du " + e.getDateReservation().format(EmpruntService.JOUR)
                            + " n'a pas été retirée : elle a été annulée.", "mes-emprunts.html");
            n++;
        }
        return n;
    }
}
