package com.example.gestionmateriels;

import com.example.gestionmateriels.model.Agent;
import com.example.gestionmateriels.model.Materiel;
import com.example.gestionmateriels.model.Materiel.Categorie;
import com.example.gestionmateriels.model.Materiel.StatutMateriel;
import com.example.gestionmateriels.model.Materiel.TypeGestion;
import com.example.gestionmateriels.repository.AgentRepository;
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
    CommandLineRunner initData(AgentRepository agentRepo, MaterielRepository materielRepo) {
        return args -> {
            if (agentRepo.count() == 0) {
                agentRepo.save(new Agent("M. Daniel", "Administrateur"));
                agentRepo.save(new Agent("M. Guillaume", "Surveillant"));
            }

            if (materielRepo.count() == 0) {
                materielRepo.save(new Materiel("Vidéoprojecteur", Categorie.AUDIOVISUEL,
                        TypeGestion.DURABLE, "VP-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Micro", Categorie.AUDIOVISUEL,
                        TypeGestion.DURABLE, "MIC-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Baffe", Categorie.AUDIOVISUEL,
                        TypeGestion.DURABLE, "BAF-01", StatutMateriel.DISPONIBLE, 1));
                materielRepo.save(new Materiel("Marqueur Noir", Categorie.FOURNITURES,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 20));
                materielRepo.save(new Materiel("Effaceur", Categorie.FOURNITURES,
                        TypeGestion.CONSOMMABLE, null, StatutMateriel.DISPONIBLE, 5));
                materielRepo.save(new Materiel("Câble HDMI", Categorie.CONNECTIQUE,
                        TypeGestion.DURABLE, "HDMI-01", StatutMateriel.DISPONIBLE, 1));
            }
        };
    }
}