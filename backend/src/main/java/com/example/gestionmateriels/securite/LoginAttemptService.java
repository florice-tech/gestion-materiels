package com.example.gestionmateriels.securite;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freine les attaques par essais répétés de mots de passe : après 5 échecs pour un même
 * identifiant depuis une même adresse, ou 30 échecs depuis une même adresse, la connexion
 * est bloquée quelques minutes. Les compteurs restent en mémoire.
 */
@Service
public class LoginAttemptService {

    static final int ECHECS_PAR_COMPTE = 5;
    static final int ECHECS_PAR_ADRESSE = 30;
    static final Duration BLOCAGE = Duration.ofMinutes(5);
    private static final int TAILLE_MAX = 20_000;

    private static final class Compteur {
        int echecs;
        Instant dernier = Instant.now();
        Instant bloqueJusqua;
    }

    private final Map<String, Compteur> compteurs = new ConcurrentHashMap<>();

    private static String cleCompte(String identifiant, String adresse) {
        return "c|" + identifiant.toLowerCase() + "|" + adresse;
    }

    private static String cleAdresse(String adresse) {
        return "a|" + adresse;
    }

    /** Minutes restantes avant de pouvoir réessayer, ou 0 si la connexion est permise. */
    public long minutesRestantes(String identifiant, String adresse) {
        Instant maintenant = Instant.now();
        long max = 0;
        for (String cle : new String[]{cleCompte(identifiant, adresse), cleAdresse(adresse)}) {
            Compteur c = compteurs.get(cle);
            if (c != null && c.bloqueJusqua != null && c.bloqueJusqua.isAfter(maintenant)) {
                long minutes = Duration.between(maintenant, c.bloqueJusqua).toMinutes() + 1;
                max = Math.max(max, minutes);
            }
        }
        return max;
    }

    public void echec(String identifiant, String adresse) {
        if (compteurs.size() > TAILLE_MAX) {
            nettoyer();
        }
        enregistrerEchec(cleCompte(identifiant, adresse), ECHECS_PAR_COMPTE);
        enregistrerEchec(cleAdresse(adresse), ECHECS_PAR_ADRESSE);
    }

    public void succes(String identifiant, String adresse) {
        compteurs.remove(cleCompte(identifiant, adresse));
    }

    private void enregistrerEchec(String cle, int seuil) {
        compteurs.compute(cle, (k, c) -> {
            Instant maintenant = Instant.now();
            if (c == null || Duration.between(c.dernier, maintenant).compareTo(Duration.ofMinutes(15)) > 0) {
                c = new Compteur();
            }
            c.dernier = maintenant;
            c.echecs++;
            if (c.echecs >= seuil) {
                c.bloqueJusqua = maintenant.plus(BLOCAGE);
                c.echecs = 0;
            }
            return c;
        });
    }

    private void nettoyer() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(20));
        compteurs.values().removeIf(c -> c.dernier.isBefore(limite));
    }
}
