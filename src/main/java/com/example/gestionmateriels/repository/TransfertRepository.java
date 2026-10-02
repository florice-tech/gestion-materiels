package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Transfert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransfertRepository extends JpaRepository<Transfert, Long> {

    // Demandes reçues (le délégué a le matériel) ou envoyées, les plus récentes d'abord
    List<Transfert> findByDetenteurIdOrderByDateDemandeDesc(Long detenteurId);

    List<Transfert> findByDemandeurIdOrderByDateDemandeDesc(Long demandeurId);

    Optional<Transfert> findFirstByMaterielIdAndDemandeurIdAndStatut(Long materielId, Long demandeurId,
                                                                     Transfert.Statut statut);

    List<Transfert> findByDetailSourceIdAndStatut(Long detailSourceId, Transfert.Statut statut);

    List<Transfert> findByMaterielIdOrderByDateDemandeDesc(Long materielId);
}
