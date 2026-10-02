package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.model.Notification;
import com.example.gestionmateriels.model.Notification.Categorie;
import com.example.gestionmateriels.model.Notification.Destinataire;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.NotificationRepository;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Notifications affichées dans la cloche de l'en-tête. */
@Service
@Transactional
public class NotificationService {

    private static final int NOMBRE_AFFICHE = 30;

    private final NotificationRepository notificationRepository;
    private final AgentRepository agentRepository;

    public NotificationService(NotificationRepository notificationRepository, AgentRepository agentRepository) {
        this.notificationRepository = notificationRepository;
        this.agentRepository = agentRepository;
    }

    /** Une notification pour chaque agent actif (chacun la marque lue de son côté). */
    public void notifierAgents(Categorie categorie, String titre, String message, String lien) {
        for (Agent agent : agentRepository.findAll()) {
            if (agent.isActif()) {
                notificationRepository.save(new Notification(Destinataire.AGENT, agent.getId(),
                        categorie, titre, message, lien));
            }
        }
    }

    public void notifierDelegue(Delegue delegue, Categorie categorie, String titre, String message, String lien) {
        notificationRepository.save(new Notification(Destinataire.DELEGUE, delegue.getId(),
                categorie, titre, message, lien));
    }

    @Transactional(readOnly = true)
    public List<Notification> mesNotifications(UtilisateurConnecte moi) {
        return notificationRepository.findByDestinataireTypeAndDestinataireIdOrderByDateCreationDesc(
                type(moi), moi.getId(), PageRequest.of(0, NOMBRE_AFFICHE));
    }

    @Transactional(readOnly = true)
    public long nombreNonLues(UtilisateurConnecte moi) {
        return notificationRepository.countByDestinataireTypeAndDestinataireIdAndLueFalse(type(moi), moi.getId());
    }

    public void marquerLue(Long id, UtilisateurConnecte moi) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Notification introuvable."));
        if (n.getDestinataireType() != type(moi) || !n.getDestinataireId().equals(moi.getId())) {
            throw OperationException.introuvable("Notification introuvable.");
        }
        n.setLue(true);
    }

    public void toutMarquerLu(UtilisateurConnecte moi) {
        notificationRepository.toutMarquerLu(type(moi), moi.getId());
    }

    private static Destinataire type(UtilisateurConnecte moi) {
        return moi.estAgent() ? Destinataire.AGENT : Destinataire.DELEGUE;
    }
}
