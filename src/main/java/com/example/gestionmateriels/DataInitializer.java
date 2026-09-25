package com.example.gestionmateriels;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Categorie;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import com.example.gestionmateriels.repository.AgentRepository;
import com.example.gestionmateriels.repository.CategorieRepository;
import com.example.gestionmateriels.repository.MaterielRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Insère quelques données de test au démarrage, uniquement si la base est vide.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(AgentRepository agentRepo, MaterielRepository materielRepo,
                               CategorieRepository categorieRepo) {
        return args -> {
            if (categorieRepo.count() == 0) {
                categorieRepo.save(new Categorie("AUDIOVISUEL"));
                categorieRepo.save(new Categorie("ACCESSOIRES"));
                categorieRepo.save(new Categorie("CONNECTIQUE"));
                categorieRepo.save(new Categorie("FOURNITURES"));
            }

            if (agentRepo.count() == 0) {
                agentRepo.save(new Agent("M. Daniel", "Administrateur"));
                agentRepo.save(new Agent("M. Guillaume", "Surveillant"));
            }

            if (materielRepo.count() == 0) {
                Categorie audiovisuel = categorieRepo.findByNom("AUDIOVISUEL").orElseThrow();
                Categorie fournitures = categorieRepo.findByNom("FOURNITURES").orElseThrow();
                Categorie connectique = categorieRepo.findByNom("CONNECTIQUE").orElseThrow();

                materielRepo.save(new Materiel("Vidéoprojecteur", audiovisuel,
                        TypeGestion.DURABLE, "VP-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Micro", audiovisuel,
                        TypeGestion.DURABLE, "MIC-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Baffe", audiovisuel,
                        TypeGestion.DURABLE, "BAF-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Marqueur Noir", fournitures,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 20));
                materielRepo.save(new Materiel("Effaceur", fournitures,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 5));
                materielRepo.save(new Materiel("Câble HDMI", connectique,
                        TypeGestion.DURABLE, "HDMI-01", StatutMateriel.DISPONIBLE, 1));
            }
        };
    }
}