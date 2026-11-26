package com.example.gestionmateriels.service;

import com.example.gestionmateriels.dto.AgentRequest;
import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.model.Notification;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.DelegueRepository;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comptes utilisateurs : inscription des délégués, gestion des agents et délégués
 * par un administrateur, changement de mot de passe.
 */
@Service
@Transactional
public class CompteService {

    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    // Dernière demande de « mot de passe oublié » par identifiant (évite d'inonder les administrateurs)
    private final Map<String, Instant> demandesMotDePasse = new ConcurrentHashMap<>();

    public CompteService(AgentRepository agentRepository, DelegueRepository delegueRepository,
                         PasswordEncoder passwordEncoder, NotificationService notificationService) {
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    // ---------- Délégués ----------

    public Delegue inscrireDelegue(String nom, String filiereNiveau, String identifiant, String motDePasse) {
        return inscrireDelegue(nom, filiereNiveau, identifiant, motDePasse, null);
    }

    public Delegue inscrireDelegue(String nom, String filiereNiveau, String identifiant, String motDePasse,
                                   String telephone) {
        return creerDelegue(nom, filiereNiveau, null, identifiant, motDePasse, telephone);
    }

    /** Création d'un compte délégué avec son matricule (obligatoire). */
    public Delegue inscrireDelegue(String nom, String filiereNiveau, String matricule, String identifiant,
                                   String motDePasse, String telephone) {
        return creerDelegue(nom, filiereNiveau, Verifications.matriculeValide(matricule),
                identifiant, motDePasse, telephone);
    }

    private Delegue creerDelegue(String nom, String filiereNiveau, String matricule, String identifiant,
                                 String motDePasse, String telephone) {
        String nomPropre = Verifications.obligatoire(nom, "Le nom est obligatoire.");
        String filiere = Verifications.obligatoire(filiereNiveau, "La filière/niveau est obligatoire.");
        String id = Verifications.identifiantValide(identifiant);
        Verifications.motDePasseValide(motDePasse);
        verifierIdentifiantLibre(id);
        if (matricule != null) {
            verifierMatriculeLibre(matricule, null);
        }

        Delegue delegue = new Delegue(nomPropre, filiere, id, passwordEncoder.encode(motDePasse));
        delegue.setMatricule(matricule);
        delegue.setTelephone(Verifications.telephone(telephone));
        return delegueRepository.save(delegue);
    }

    /** Modifie le matricule d'un délégué (administrateur). */
    public Delegue changerMatricule(Long id, String matricule) {
        Delegue delegue = trouverDelegue(id);
        String m = Verifications.matriculeValide(matricule);
        verifierMatriculeLibre(m, delegue);
        delegue.setMatricule(m);
        return delegue;
    }

    /** Modifie l'identifiant de connexion d'un délégué (administrateur). */
    public Delegue changerIdentifiantDelegue(Long id, String identifiant) {
        Delegue delegue = trouverDelegue(id);
        String nouveau = Verifications.identifiantValide(identifiant);
        if (!nouveau.equalsIgnoreCase(delegue.getIdentifiant())) {
            verifierIdentifiantLibre(nouveau);
        }
        delegue.setIdentifiant(nouveau);
        return delegue;
    }

    /** Modifie l'identifiant de connexion d'un agent (administrateur). */
    public Agent changerIdentifiantAgent(Long id, String identifiant) {
        Agent agent = trouverAgent(id);
        String nouveau = Verifications.identifiantValide(identifiant);
        if (!nouveau.equalsIgnoreCase(agent.getIdentifiant())) {
            verifierIdentifiantLibre(nouveau);
        }
        agent.setIdentifiant(nouveau);
        return agent;
    }

    /**
     * « Mot de passe oublié » : prévient les administrateurs, qui réinitialisent le mot de passe
     * et transmettent les nouveaux identifiants. Sans effet visible si l'identifiant est inconnu
     * (on ne révèle pas quels comptes existent).
     */
    public void demanderMotDePasseOublie(String identifiant) {
        String id = identifiant == null ? "" : identifiant.trim();
        if (id.isEmpty()) {
            throw new OperationException("Saisissez votre identifiant.");
        }
        Instant derniere = demandesMotDePasse.get(id.toLowerCase());
        if (derniere != null && Duration.between(derniere, Instant.now()).toMinutes() < 5) {
            return;
        }
        String qui = delegueRepository.findByIdentifiantIgnoreCase(id)
                .map(d -> d.getNom() + " (délégué, " + d.getFiliereNiveau() + ")")
                .or(() -> agentRepository.findByIdentifiantIgnoreCase(id).map(a -> a.getNom() + " (" + a.getRole() + ")"))
                .orElse(null);
        if (qui != null) {
            demandesMotDePasse.put(id.toLowerCase(), Instant.now());
            notificationService.notifierAdministrateurs(Notification.Categorie.INFO,
                    "Mot de passe oublié",
                    qui + " a oublié son mot de passe (identifiant : " + id + "). Réinitialisez-le dans Comptes.",
                    "comptes.html");
        }
    }

    /** Numéro WhatsApp d'un délégué (par lui-même ou par un administrateur). */
    public Delegue changerTelephone(Long delegueId, String telephone) {
        Delegue delegue = delegueRepository.findById(delegueId)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable."));
        delegue.setTelephone(Verifications.telephone(telephone));
        return delegue;
    }

    @Transactional(readOnly = true)
    public List<Delegue> listerDelegues() {
        return delegueRepository.findAllByOrderByNomAsc();
    }

    public Delegue changerActifDelegue(Long id, Boolean actif) {
        if (actif == null) {
            throw new OperationException("Indiquez si le compte doit être actif ou non.");
        }
        Delegue delegue = trouverDelegue(id);
        delegue.setActif(actif);
        return delegue;
    }

    public void reinitialiserMotDePasseDelegue(Long id, String motDePasse) {
        Verifications.motDePasseValide(motDePasse);
        trouverDelegue(id).setMotDePasse(passwordEncoder.encode(motDePasse));
    }

    // ---------- Agents ----------

    @Transactional(readOnly = true)
    public List<Agent> listerAgents() {
        return agentRepository.findAllByOrderByNomAsc();
    }

    public Agent creerAgent(AgentRequest requete) {
        String nom = Verifications.obligatoire(requete.nom(), "Le nom est obligatoire.");
        String id = Verifications.identifiantValide(requete.identifiant());
        Verifications.motDePasseValide(requete.motDePasse());
        verifierIdentifiantLibre(id);

        String role = Verifications.facultatif(requete.role());
        Agent agent = new Agent(nom, role != null ? role : "Surveillant", id,
                passwordEncoder.encode(requete.motDePasse()), Boolean.TRUE.equals(requete.administrateur()));
        return agentRepository.save(agent);
    }

    /** Modifie le nom, l'intitulé et le droit administrateur (pas l'identifiant ni le mot de passe). */
    public Agent modifierAgent(Long id, AgentRequest requete, UtilisateurConnecte moi) {
        Agent agent = trouverAgent(id);
        agent.setNom(Verifications.obligatoire(requete.nom(), "Le nom est obligatoire."));
        String role = Verifications.facultatif(requete.role());
        if (role != null) {
            agent.setRole(role);
        }
        if (requete.administrateur() != null && requete.administrateur() != agent.isAdministrateur()) {
            if (!requete.administrateur()) {
                if (agent.getId().equals(moi.getId())) {
                    throw new OperationException("Vous ne pouvez pas retirer vos propres droits d'administrateur.");
                }
                verifierAutreAdministrateurActif(agent);
            }
            agent.setAdministrateur(requete.administrateur());
        }
        return agent;
    }

    public Agent changerActifAgent(Long id, Boolean actif, UtilisateurConnecte moi) {
        if (actif == null) {
            throw new OperationException("Indiquez si le compte doit être actif ou non.");
        }
        Agent agent = trouverAgent(id);
        if (!actif) {
            if (agent.getId().equals(moi.getId())) {
                throw new OperationException("Vous ne pouvez pas désactiver votre propre compte.");
            }
            if (agent.isAdministrateur()) {
                verifierAutreAdministrateurActif(agent);
            }
        }
        agent.setActif(actif);
        return agent;
    }

    public void reinitialiserMotDePasseAgent(Long id, String motDePasse) {
        Verifications.motDePasseValide(motDePasse);
        trouverAgent(id).setMotDePasse(passwordEncoder.encode(motDePasse));
    }

    // ---------- Mon compte ----------

    public void changerMonMotDePasse(UtilisateurConnecte moi, String ancien, String nouveau) {
        Verifications.motDePasseValide(nouveau);
        if (moi.estAgent()) {
            Agent agent = trouverAgent(moi.getId());
            verifierAncien(ancien, agent.getMotDePasse());
            agent.setMotDePasse(passwordEncoder.encode(nouveau));
        } else {
            Delegue delegue = trouverDelegue(moi.getId());
            verifierAncien(ancien, delegue.getMotDePasse());
            delegue.setMotDePasse(passwordEncoder.encode(nouveau));
        }
    }

    // ---------- Outils ----------

    private void verifierAncien(String ancien, String hache) {
        if (ancien == null || !passwordEncoder.matches(ancien, hache)) {
            throw new OperationException("L'ancien mot de passe est incorrect.");
        }
    }

    private void verifierMatriculeLibre(String matricule, Delegue courant) {
        if (delegueRepository.existsByMatriculeIgnoreCase(matricule)
                && (courant == null || !matricule.equalsIgnoreCase(courant.getMatricule()))) {
            throw new OperationException("Ce matricule est déjà utilisé.");
        }
    }

    /** Un identifiant est unique sur l'ensemble des agents ET des délégués. */
    private void verifierIdentifiantLibre(String identifiant) {
        if (agentRepository.existsByIdentifiantIgnoreCase(identifiant)
                || delegueRepository.existsByIdentifiantIgnoreCase(identifiant)) {
            throw new OperationException("Cet identifiant est déjà utilisé.");
        }
    }

    private void verifierAutreAdministrateurActif(Agent agent) {
        long adminsActifs = agentRepository.countByAdministrateurTrueAndActifTrue();
        boolean luiCompte = agent.isAdministrateur() && agent.isActif();
        if (luiCompte && adminsActifs <= 1) {
            throw new OperationException("Il doit rester au moins un administrateur actif.");
        }
    }

    private Agent trouverAgent(Long id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Agent introuvable (id=" + id + ")."));
    }

    private Delegue trouverDelegue(Long id) {
        return delegueRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Délégué introuvable (id=" + id + ")."));
    }
}
