package com.example.gestionmateriels;

import com.example.gestionmateriels.model.*;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import com.example.gestionmateriels.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Au démarrage :
 * 1. hache avec BCrypt les mots de passe encore stockés en clair (bases créées par l'ancienne version) ;
 * 2. insère des données de démonstration dans les tables vides.
 */
@Configuration
public class DataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initData(AgentRepository agentRepo, DelegueRepository delegueRepo,
                               CategorieRepository categorieRepo, MaterielRepository materielRepo,
                               SalleRepository salleRepo, PasswordEncoder encoder,
                               TransactionTemplate transaction) {
        return args -> transaction.executeWithoutResult(statut -> {
            hacherMotsDePasseEnClair(agentRepo, delegueRepo, encoder);

            if (categorieRepo.count() == 0) {
                categorieRepo.save(new Categorie("AUDIOVISUEL"));
                categorieRepo.save(new Categorie("ACCESSOIRES"));
                categorieRepo.save(new Categorie("CONNECTIQUE"));
                categorieRepo.save(new Categorie("FOURNITURES"));
            }

            if (salleRepo.count() == 0) {
                for (String nom : new String[]{"101", "102", "Amphi A", "Labo Info"}) {
                    salleRepo.save(new Salle(nom));
                }
            }

            if (agentRepo.count() == 0) {
                agentRepo.save(new Agent("M. Daniel", "Surveillant général", "mdaniel",
                        encoder.encode("daniel2026"), true));
                agentRepo.save(new Agent("M. Guillaume", "Surveillant", "mguillaume",
                        encoder.encode("guillaume2026"), false));
            }

            if (delegueRepo.count() == 0) {
                delegueRepo.save(new Delegue("Pascal Kodjo", "L2 Economie", "pkodjo", encoder.encode("pascal2026")));
            }

            if (materielRepo.count() == 0) {
                Categorie audiovisuel = categorieRepo.findByNomIgnoreCase("AUDIOVISUEL").orElseThrow();
                Categorie fournitures = categorieRepo.findByNomIgnoreCase("FOURNITURES").orElseThrow();
                Categorie connectique = categorieRepo.findByNomIgnoreCase("CONNECTIQUE").orElseThrow();

                materielRepo.save(new Materiel("Vidéoprojecteur", audiovisuel,
                        TypeGestion.DURABLE, "VP-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Micro", audiovisuel,
                        TypeGestion.DURABLE, "MIC-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Baffle", audiovisuel,
                        TypeGestion.DURABLE, "BAF-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Câble HDMI", connectique,
                        TypeGestion.DURABLE, "HDMI-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Marqueur Noir", fournitures,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 20));
                materielRepo.save(new Materiel("Effaceur", fournitures,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 5));
            }
        });
    }

    /** Un hachage BCrypt commence par "$2" : tout autre mot de passe est considéré comme en clair. */
    private static void hacherMotsDePasseEnClair(AgentRepository agentRepo, DelegueRepository delegueRepo,
                                                 PasswordEncoder encoder) {
        int convertis = 0;
        for (Agent agent : agentRepo.findAll()) {
            if (!estHache(agent.getMotDePasse())) {
                agent.setMotDePasse(encoder.encode(agent.getMotDePasse()));
                convertis++;
            }
        }
        for (Delegue delegue : delegueRepo.findAll()) {
            if (!estHache(delegue.getMotDePasse())) {
                delegue.setMotDePasse(encoder.encode(delegue.getMotDePasse()));
                convertis++;
            }
        }
        if (convertis > 0) {
            LOG.info("{} mot(s) de passe en clair ont été hachés avec BCrypt.", convertis);
        }
    }

    private static boolean estHache(String motDePasse) {
        return motDePasse != null && motDePasse.startsWith("$2");
    }
}
