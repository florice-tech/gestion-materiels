package com.example.gestionmateriels.securite;

import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.DelegueRepository;
import com.example.gestionmateriels.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * À chaque appel de l'API par un utilisateur connecté :
 * - on vérifie que son compte existe encore, est toujours actif et garde les mêmes droits
 *   (sinon la session est fermée : un compte désactivé ou rétrogradé est sans effet immédiat) ;
 * - on note sa dernière activité (présences en ligne).
 */
@Configuration
public class PresenceConfig implements WebMvcConfigurer {

    private final PresenceService presenceService;
    private final AgentRepository agentRepository;
    private final DelegueRepository delegueRepository;

    public PresenceConfig(PresenceService presenceService, AgentRepository agentRepository,
                          DelegueRepository delegueRepository) {
        this.presenceService = presenceService;
        this.agentRepository = agentRepository;
        this.delegueRepository = delegueRepository;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws IOException {
                Authentication a = SecurityContextHolder.getContext().getAuthentication();
                if (a == null || !(a.getPrincipal() instanceof UtilisateurConnecte moi)) {
                    return true;
                }
                if (!compteToujoursValide(moi)) {
                    presenceService.retirer(moi);
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    SecurityContextHolder.clearContext();
                    SecuriteConfig.repondre(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Votre compte a été modifié ou désactivé. Reconnectez-vous.");
                    return false;
                }
                presenceService.enregistrer(moi);
                return true;
            }
        }).addPathPatterns("/api/**");
    }

    private boolean compteToujoursValide(UtilisateurConnecte moi) {
        if (moi.estAgent()) {
            return agentRepository.findById(moi.getId())
                    .map(ag -> ag.isActif() && ag.isAdministrateur() == moi.isAdministrateur())
                    .orElse(false);
        }
        return delegueRepository.findById(moi.getId()).map(d -> d.isActif()).orElse(false);
    }
}
