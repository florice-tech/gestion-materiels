package com.example.gestionmateriels.service;

import com.example.gestionmateriels.securite.UtilisateurConnecte;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Qui est connecté en ce moment ? Chaque appel de l'API par un utilisateur connecté met à jour
 * sa « dernière activité » (les pages envoient aussi un signal toutes les minutes). Un utilisateur
 * est en ligne s'il a été actif dans les dernières minutes. Les données restent en mémoire.
 */
@Service
public class PresenceService {

    /** Au-delà de ce délai sans activité, on considère la personne hors ligne. */
    public static final Duration DELAI_EN_LIGNE = Duration.ofMinutes(3);

    private record Presence(String type, Long id, String nom, String complement, String identifiant,
                            Instant depuis, Instant derniere) {
    }

    private final Map<String, Presence> presences = new ConcurrentHashMap<>();

    private static String cle(UtilisateurConnecte moi) {
        return moi.getType() + ":" + moi.getId();
    }

    public void enregistrer(UtilisateurConnecte moi) {
        Instant maintenant = Instant.now();
        presences.compute(cle(moi), (k, ancienne) -> {
            Instant depuis = ancienne != null
                    && Duration.between(ancienne.derniere(), maintenant).compareTo(DELAI_EN_LIGNE) < 0
                    ? ancienne.depuis() : maintenant;
            return new Presence(moi.getType().name(), moi.getId(), moi.getNom(), moi.getComplement(),
                    moi.getUsername(), depuis, maintenant);
        });
    }

    public void retirer(UtilisateurConnecte moi) {
        presences.remove(cle(moi));
    }

    /** Personnes en ligne, les plus récemment actives d'abord. */
    public List<Map<String, Object>> enLigne() {
        Instant limite = Instant.now().minus(DELAI_EN_LIGNE);
        presences.values().removeIf(p -> p.derniere().isBefore(limite.minus(Duration.ofHours(1))));
        return presences.values().stream()
                .filter(p -> !p.derniere().isBefore(limite))
                .sorted(Comparator.comparing(Presence::derniere).reversed())
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", p.type());
                    m.put("id", p.id());
                    m.put("nom", p.nom());
                    m.put("complement", p.complement());
                    m.put("identifiant", p.identifiant());
                    m.put("connecteDepuis", p.depuis().toString());
                    m.put("derniereActivite", p.derniere().toString());
                    return m;
                })
                .toList();
    }
}
