package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.dto.ChangementMotDePasseRequest;
import com.example.gestionmateriels.dto.InscriptionDelegueRequest;
import com.example.gestionmateriels.dto.LoginRequest;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.CompteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Connexion, session courante, inscription des délégués, changement de mot de passe.
 * La déconnexion (POST /api/auth/logout) est gérée par Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CompteService compteService;

    public AuthController(AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          CompteService compteService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.compteService = compteService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest requete,
                                                     HttpServletRequest request, HttpServletResponse response) {
        String identifiant = requete.identifiant() == null ? "" : requete.identifiant().trim();
        String motDePasse = requete.motDePasse() == null ? "" : requete.motDePasse();

        Authentication authentification;
        try {
            authentification = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(identifiant, motDePasse));
        } catch (DisabledException e) {
            return Reponses.erreur(401, "Ce compte est désactivé. Contactez un administrateur.");
        } catch (BadCredentialsException e) {
            return Reponses.erreur(401, "Identifiant ou mot de passe incorrect.");
        } catch (AuthenticationException e) {
            return Reponses.erreur(401, "Connexion impossible.");
        }

        // Nouvel identifiant de session à la connexion (protection contre la fixation de session)
        HttpSession session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
        }

        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(authentification);
        SecurityContextHolder.setContext(contexte);
        securityContextRepository.saveContext(contexte, request, response);

        UtilisateurConnecte moi = (UtilisateurConnecte) authentification.getPrincipal();
        return ResponseEntity.ok(profil(moi));
    }

    /** Utilisateur de la session courante, ou { success: false } si personne n'est connecté. */
    @GetMapping("/moi")
    public ResponseEntity<Map<String, Object>> moi(@AuthenticationPrincipal UtilisateurConnecte moi) {
        if (moi == null) {
            return Reponses.negatif("Aucun utilisateur connecté.");
        }
        return ResponseEntity.ok(profil(moi));
    }

    @PostMapping("/inscription")
    public ResponseEntity<Map<String, Object>> inscrire(@RequestBody InscriptionDelegueRequest requete) {
        compteService.inscrireDelegue(requete.nom(), requete.filiereNiveau(),
                requete.identifiant(), requete.motDePasse());
        return Reponses.ok("Compte créé avec succès. Vous pouvez maintenant vous connecter.");
    }

    @PostMapping("/mot-de-passe")
    public ResponseEntity<Map<String, Object>> changerMotDePasse(@AuthenticationPrincipal UtilisateurConnecte moi,
                                                                 @RequestBody ChangementMotDePasseRequest requete) {
        compteService.changerMonMotDePasse(moi, requete.ancienMotDePasse(), requete.nouveauMotDePasse());
        return Reponses.ok("Mot de passe modifié.");
    }

    private Map<String, Object> profil(UtilisateurConnecte moi) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", true);
        corps.put("id", moi.getId());
        corps.put("type", moi.getType().name());
        corps.put("identifiant", moi.getUsername());
        corps.put("nom", moi.getNom());
        corps.put("administrateur", moi.isAdministrateur());
        if (moi.estAgent()) {
            corps.put("role", moi.getComplement());
        } else {
            corps.put("filiereNiveau", moi.getComplement());
        }
        return corps;
    }
}
