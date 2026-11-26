package com.example.gestionmateriels.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Réservation à l'avance : jour, créneau, salle et matériel. */
public record ReservationRequest(String salle, LocalDate jour, LocalTime heureDebut, LocalTime heureFin,
                                 List<ArticleDemande> articles) {
}
