package com.example.gestionmateriels;

import com.example.gestionmateriels.securite.LoginAttemptService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTests {

    @Test
    void blocageApresCinqEchecs() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            service.echec("awa", "10.0.0.1");
        }
        assertEquals(0, service.minutesRestantes("awa", "10.0.0.1"));
        service.echec("awa", "10.0.0.1");
        assertTrue(service.minutesRestantes("awa", "10.0.0.1") > 0);
        // un autre identifiant, depuis la même adresse, n'est pas bloqué
        assertEquals(0, service.minutesRestantes("bob", "10.0.0.1"));
    }

    @Test
    void succesRemetLeCompteurAZero() {
        LoginAttemptService service = new LoginAttemptService();
        for (int i = 0; i < 4; i++) {
            service.echec("awa", "10.0.0.2");
        }
        service.succes("awa", "10.0.0.2");
        service.echec("awa", "10.0.0.2");
        assertEquals(0, service.minutesRestantes("awa", "10.0.0.2"));
    }
}
