package com.example.gestionmateriels.service;

import com.example.gestionmateriels.model.Salle;
import com.example.gestionmateriels.repository.SalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Salles proposées dans les demandes. Les emprunts gardent le nom de la salle en texte :
 * renommer ou supprimer une salle ne modifie pas l'historique.
 */
@Service
@Transactional
public class SalleService {

    private final SalleRepository salleRepository;

    public SalleService(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Transactional(readOnly = true)
    public List<Salle> lister() {
        return salleRepository.findAllByOrderByNomAsc();
    }

    public Salle creer(String nom) {
        String nomPropre = Verifications.obligatoire(nom, "Le nom de la salle est obligatoire.");
        verifierNomLibre(nomPropre, null);
        return salleRepository.save(new Salle(nomPropre));
    }

    public Salle renommer(Long id, String nom) {
        Salle salle = trouver(id);
        String nomPropre = Verifications.obligatoire(nom, "Le nom de la salle est obligatoire.");
        verifierNomLibre(nomPropre, id);
        salle.setNom(nomPropre);
        return salle;
    }

    public void supprimer(Long id) {
        salleRepository.delete(trouver(id));
    }

    private void verifierNomLibre(String nom, Long idActuel) {
        salleRepository.findByNomIgnoreCase(nom)
                .filter(autre -> !autre.getId().equals(idActuel))
                .ifPresent(autre -> {
                    throw new OperationException("Cette salle existe déjà : " + nom);
                });
    }

    private Salle trouver(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> OperationException.introuvable("Salle introuvable (id=" + id + ")."));
    }
}
