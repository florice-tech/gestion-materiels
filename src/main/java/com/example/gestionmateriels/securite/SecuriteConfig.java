package com.example.gestionmateriels.securite;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;

/**
 * Sécurité de l'application :
 * - connexion par session serveur (cookie JSESSIONID), mots de passe hachés BCrypt ;
 * - protection CSRF : le jeton est déposé dans le cookie XSRF-TOKEN et renvoyé par
 *   les pages dans l'en-tête X-XSRF-TOKEN (voir js/commun.js) ;
 * - droits par rôle sur l'API ; les pages HTML sont publiques mais vérifient la session
 *   au chargement et redirigent vers la connexion si besoin.
 */
@Configuration
@EnableWebSecurity
public class SecuriteConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UtilisateurDetailsService utilisateurs,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider fournisseur = new DaoAuthenticationProvider(utilisateurs);
        fournisseur.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(fournisseur);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filtreSecurite(HttpSecurity http, SecurityContextRepository contextes) throws Exception {
        // Jeton CSRF "simple" (non masqué), lu par le JavaScript dans le cookie.
        // setCsrfRequestAttributeName(null) : le jeton est chargé à chaque requête, donc le cookie
        // est toujours présent dès la première page affichée.
        CsrfTokenRequestAttributeHandler gestionJeton = new CsrfTokenRequestAttributeHandler();
        gestionJeton.setCsrfRequestAttributeName(null);

        http
            .securityContext(sc -> sc.securityContextRepository(contextes))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(gestionJeton))
            .authorizeHttpRequests(regles -> regles
                // Connexion et inscription : ouvertes à tous
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/inscription").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/moi").permitAll()
                // Gestion des comptes : administrateurs uniquement
                .requestMatchers("/api/admin/**", "/api/statistiques/affluence").hasRole("ADMIN")
                // Actions réservées aux délégués
                .requestMatchers(HttpMethod.POST, "/api/emprunts/demande", "/api/emprunts/reservation",
                        "/api/emprunts/*/annuler", "/api/scan/**").hasRole("DELEGUE")
                .requestMatchers(HttpMethod.GET, "/api/emprunts/mes-emprunts").hasRole("DELEGUE")
                .requestMatchers("/api/transferts/**").hasRole("DELEGUE")
                // Listes d'emprunts réservées aux agents (avant la règle générale /api/emprunts/* ci-dessous)
                .requestMatchers(HttpMethod.GET, "/api/emprunts/en-attente", "/api/emprunts/en-cours",
                        "/api/emprunts/historique", "/api/emprunts/export", "/api/emprunts/reservations").hasRole("AGENT")
                // Lecture pour tout utilisateur connecté (une fiche : le contrôleur vérifie qu'elle appartient au délégué)
                .requestMatchers(HttpMethod.GET, "/api/materiels", "/api/salles", "/api/categories",
                        "/api/disponibilites", "/api/config", "/api/scan/*", "/api/emprunts/*",
                        "/api/photos/**", "/api/materiels/*/photos", "/api/salles/*/situation", "/api/confiance/moi").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                // Mon compte, déconnexion : tout utilisateur connecté
                .requestMatchers("/api/auth/**").authenticated()
                // Tout le reste de l'API : agents
                .requestMatchers("/api/**").hasRole("AGENT")
                // Pages HTML, scripts, styles
                .anyRequest().permitAll())
            .exceptionHandling(erreurs -> erreurs
                .authenticationEntryPoint((req, res, ex) ->
                        repondre(res, HttpServletResponse.SC_UNAUTHORIZED, "Vous devez vous connecter."))
                .accessDeniedHandler((req, res, ex) ->
                        repondre(res, HttpServletResponse.SC_FORBIDDEN,
                                "Action non autorisée (ou session expirée : rechargez la page).")))
            .logout(deconnexion -> deconnexion
                .logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) ->
                        repondre(res, HttpServletResponse.SC_OK, "Vous êtes déconnecté.")))
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable());

        return http.build();
    }

    /** Réponse JSON { success, message } au même format que le reste de l'API. */
    static void repondre(HttpServletResponse res, int statut, String message) throws IOException {
        res.setStatus(statut);
        res.setContentType("application/json;charset=UTF-8");
        String json = "{\"success\":" + (statut < 400) + ",\"message\":\""
                + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        res.getWriter().write(json);
    }
}
