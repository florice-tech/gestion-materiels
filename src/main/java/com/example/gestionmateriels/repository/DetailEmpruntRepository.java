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
