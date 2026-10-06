package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.DetailEmprunt;
import com.example.gestionmateriels.model.Emprunt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DetailEmpruntRepository extends JpaRepository<DetailEmprunt, Long> {

    List<DetailEmprunt> findByEmpruntIdOrderByIdAsc(Long empruntId);

    boolean existsByMaterielId(Long materielId);

    /** Ligne d'emprunt en cours pour ce matériel durable (qui l'a entre les mains), s'il y en a une. */
    @Query("""
            select d from DetailEmprunt d
            where d.materiel.id = :materielId and d.dateRetour is null
              and d.emprunt.statutEmprunt = com.example.gestionmateriels.model.Emprunt.StatutEmprunt.EN_COURS
            """)
    List<DetailEmprunt> lignesEnMain(@Param("materielId") Long materielId);

    /** Tout le matériel durable sorti en ce moment (qui a quoi), le plus ancien d'abord. */
    @Query("""
            select d from DetailEmprunt d
            where d.dateRetour is null
              and d.materiel.typeGestion = com.example.gestionmateriels.model.Materiel.TypeGestion.DURABLE
              and d.emprunt.statutEmprunt = com.example.gestionmateriels.model.Emprunt.StatutEmprunt.EN_COURS
            order by d.emprunt.dateSortie asc
            """)
    List<DetailEmprunt> toutesLignesEnMain();

    /** Matériel durable rendu (ou passé à un autre) par un délégué depuis une date : base du score de confiance. */
    @Query("""
            select d from DetailEmprunt d
            where d.emprunt.delegue.id = :delegueId and d.dateRetour >= :depuis
              and d.materiel.typeGestion = com.example.gestionmateriels.model.Materiel.TypeGestion.DURABLE
            """)
    List<DetailEmprunt> lignesRenduesDuDelegue(@Param("delegueId") Long delegueId,
                                              @Param("depuis") java.time.LocalDateTime depuis);

    /** Articles rendus depuis une date (pour le fil d'activité). */
    List<DetailEmprunt> findByDateRetourGreaterThanEqualOrderByDateRetourDesc(java.time.LocalDateTime depuis);

    /** Toutes les lignes d'un matériel, pour retracer son parcours. */
    @Query("""
            select d from DetailEmprunt d
            where d.materiel.id = :materielId
            order by coalesce(d.emprunt.dateSortie, d.emprunt.dateDemande) desc, d.id desc
            """)
    List<DetailEmprunt> parcoursMateriel(@Param("materielId") Long materielId);

    /** Lignes de durables réservées par d'autres fiches (attente ou réservation) pour ce matériel. */
    @Query("""
            select d from DetailEmprunt d
            where d.materiel.id = :materielId
              and d.emprunt.statutEmprunt in :statuts
            """)
    List<DetailEmprunt> lignesParStatut(@Param("materielId") Long materielId,
                                       @Param("statuts") Collection<Emprunt.StatutEmprunt> statuts);

    /**
     * Matériels les plus demandés, parmi les emprunts réellement sortis.
     * Chaque ligne : [désignation, nombre d'emprunts, quantité totale].
     */
    @Query("""
            select d.materiel.designation, count(d), sum(d.quantite)
            from DetailEmprunt d
            where d.emprunt.statutEmprunt in :statuts
            group by d.materiel.id, d.materiel.designation
            order by count(d) desc, sum(d.quantite) desc
            """)
    List<Object[]> classementMateriels(@Param("statuts") Collection<Emprunt.StatutEmprunt> statuts, Pageable page);
}
