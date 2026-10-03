package com.example.gestionmateriels.repository;

import com.example.gestionmateriels.model.SignalementManque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignalementManqueRepository extends JpaRepository<SignalementManque, Long> {

    List<SignalementManque> findByManqueIdOrderByDateSignalementDesc(Long manqueId);

    List<SignalementManque> findByDelegueIdOrderByDateSignalementDesc(Long delegueId);
}
