package com.example.gestionmateriels;

import com.example.gestionmateriels.dto.AgentRequest;
import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Delegue;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.securite.UtilisateurConnecte;
import com.example.gestionmateriels.service.CompteService;
import com.example.gestionmateriels.service.OperationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CompteServiceTests {

    @Autowired private CompteService compteService;
    @Autowired private AgentRepository agentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UtilisateurConnecte admin() {
        return UtilisateurConnecte.depuis(agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow());
    }

    @Test
    void motsDePasseDeDemonstrationHaches() {
        Agent daniel = agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow();
        assertTrue(daniel.getMotDePasse().startsWith("$2"));
        assertTrue(passwordEncoder.matches("daniel2026", daniel.getMotDePasse()));
    }

    @Test
    void inscriptionDelegueHacheLeMotDePasse() {
        Delegue d = compteService.inscrireDelegue("Awa Diallo", "L1 Droit", "adiallo", "secret123");
        assertNotEquals("secret123", d.getMotDePasse());
        assertTrue(passwordEncoder.matches("secret123", d.getMotDePasse()));
    }

    @Test
    void identifiantUniqueEntreAgentsEtDelegues() {
        OperationException e = assertThrows(OperationException.class,
                () -> compteService.inscrireDelegue("X", "L1", "MDANIEL", "secret123"));
        assertTrue(e.getMessage().contains("déjà utilisé"));
    }

    @Test
    void motDePasseTropCourtRefuse() {
        assertThrows(OperationException.class, () -> compteService.inscrireDelegue("X", "L1", "xxx", "123"));
    }

    @Test
    void changementDeMonMotDePasse() {
        UtilisateurConnecte moi = admin();
        assertThrows(OperationException.class, () -> compteService.changerMonMotDePasse(moi, "faux", "nouveau123"));

        compteService.changerMonMotDePasse(moi, "daniel2026", "nouveau123");
        Agent daniel = agentRepository.findByIdentifiantIgnoreCase("mdaniel").orElseThrow();
        assertTrue(passwordEncoder.matches("nouveau123", daniel.getMotDePasse()));
    }

    @Test
    void gestionDesAgentsParLAdministrateur() {
        Agent nouvel = compteService.creerAgent(new AgentRequest("Mme Sow", null, "msow", "motdepasse", false));
        assertEquals("Surveillant", nouvel.getRole());
        assertFalse(nouvel.isAdministrateur());

        compteService.changerActifAgent(nouvel.getId(), false, admin());
        assertFalse(nouvel.isActif());
    }

    @Test
    void onNePeutPasSeDesactiverNiSupprimerLeDernierAdministrateur() {
        UtilisateurConnecte moi = admin();
        assertThrows(OperationException.class, () -> compteService.changerActifAgent(moi.getId(), false, moi));
        assertThrows(OperationException.class, () -> compteService.modifierAgent(moi.getId(),
                new AgentRequest("M. Daniel", null, null, null, false), moi));
    }
}
