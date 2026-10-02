package com.example.gestionmateriels.dto;

import java.time.LocalTime;

/** Action après un scan : salle et heure de retour (récupérer, transfert) ou état (rendre). */
public record ScanRequest(String salle, LocalTime heureRetourPrevue, Boolean probleme, String remarque) {
}
