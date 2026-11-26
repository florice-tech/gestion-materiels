package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /** Identifiants des photos d'un matériel, dans l'ordre d'ajout (sans charger les images). */
    @Query("select p.id from Photo p where p.materiel.id = :materielId order by p.id")
    List<Long> idsDuMateriel(@Param("materielId") Long materielId);

    /** [materielId, photoId] pour tout le catalogue, dans l'ordre d'ajout. */
    @Query("select p.materiel.id, p.id from Photo p where p.materiel is not null order by p.id")
    List<Object[]> idsParMateriel();

    long countByMaterielId(Long materielId);

    List<Photo> findByMaterielId(Long materielId);

    /** [detailEmpruntId, photoId] des photos de retour d'un matériel (pour son parcours). */
    @Query("select p.detailEmprunt.id, p.id from Photo p where p.detailEmprunt.materiel.id = :materielId")
    List<Object[]> idsRetoursDuMateriel(@Param("materielId") Long materielId);
}
