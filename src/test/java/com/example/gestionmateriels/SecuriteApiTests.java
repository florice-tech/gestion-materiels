package com.example.gestionmateriels;

import com.example.gestionmateriels.repository.MaterielRepository;
import com.example.gestionmateriels.securite.UtilisateurDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Vérifie la connexion, les droits par rôle, la protection CSRF et le format des réponses. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecuriteApiTests {

    @Autowired private MockMvc mvc;
    @Autowired private UtilisateurDetailsService utilisateurs;
    @Autowired private MaterielRepository materielRepository;

    private UserDetails agent() { return utilisateurs.loadUserByUsername("mguillaume"); }
    private UserDetails admin() { return utilisateurs.loadUserByUsername("mdaniel"); }
    private UserDetails delegue() { return utilisateurs.loadUserByUsername("pkodjo"); }

    @Test
    void apiRefuseeSansConnexion() throws Exception {
        mvc.perform(get("/api/materiels"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void sessionAbsente() throws Exception {
        mvc.perform(get("/api/auth/moi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void pagesHtmlPubliques() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
    }

    @Test
    void connexionPuisSessionPuisDeconnexion() throws Exception {
        MvcResult resultat = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"pkodjo\",\"motDePasse\":\"pascal2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DELEGUE"))
                .andExpect(jsonPath("$.nom").value("Pascal Kodjo"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) resultat.getRequest().getSession(false);

        mvc.perform(get("/api/auth/moi").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiant").value("pkodjo"));

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void mauvaisMotDePasse() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"pkodjo\",\"motDePasse\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect."));
    }

    @Test
    void requeteSansJetonCsrfRefusee() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"pkodjo\",\"motDePasse\":\"pascal2026\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unDelegueNaPasAccesAuxActionsAgent() throws Exception {
        mvc.perform(get("/api/emprunts/historique").with(user(delegue())))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/materiels").with(user(delegue())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAgentNaPasAccesALAdministration() throws Exception {
        mvc.perform(get("/api/admin/agents").with(user(agent()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/agents").with(user(admin()))).andExpect(status().isOk());
    }

    @Test
    void lesMotsDePasseNeSontJamaisRenvoyes() throws Exception {
        mvc.perform(get("/api/admin/agents").with(user(admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("motDePasse"))));
    }

    @Test
    void parcoursDemandeParLApi() throws Exception {
        mvc.perform(get("/api/materiels").with(user(delegue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].designation").value("Baffle"));
        long idBaffle = materielRepository.findByCodeUniqueIgnoreCase("BAF-01").orElseThrow().getId();

        mvc.perform(post("/api/emprunts/demande").with(user(delegue())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salle\":\"101\",\"heureRetourPrevue\":\"12:30\",\"articles\":[{\"materielId\":"
                                + idBaffle + ",\"quantite\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(get("/api/emprunts/mes-emprunts").with(user(delegue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].statutEmprunt").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.data[0].heureRetourPrevue").value("12:30:00"));

        mvc.perform(get("/api/emprunts/en-attente").with(user(agent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].delegue.nom").value("Pascal Kodjo"));
    }

    @Test
    void erreurMetierAuFormatJson() throws Exception {
        mvc.perform(post("/api/emprunts/demande").with(user(delegue())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salle\":\"101\",\"articles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Aucun matériel sélectionné pour cette demande."));
    }

    @Test
    void tableauDeBordEtExport() throws Exception {
        mvc.perform(get("/api/statistiques").with(user(agent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compteurs.demandesEnAttente").exists())
                .andExpect(jsonPath("$.activite7Jours.length()").value(7));
        mvc.perform(get("/api/emprunts/export").with(user(agent())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("historique-emprunts-")))
                .andExpect(content().string(containsString("Délégué")));
    }
}
