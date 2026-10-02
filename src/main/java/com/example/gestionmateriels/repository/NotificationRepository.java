package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByDestinataireTypeAndDestinataireIdOrderByDateCreationDesc(
            Notification.Destinataire type, Long id, Pageable page);

    long countByDestinataireTypeAndDestinataireIdAndLueFalse(Notification.Destinataire type, Long id);

    @Modifying
    @Query("update Notification n set n.lue = true where n.destinataireType = :type and n.destinataireId = :id and n.lue = false")
    int toutMarquerLu(@Param("type") Notification.Destinataire type, @Param("id") Long id);
}
