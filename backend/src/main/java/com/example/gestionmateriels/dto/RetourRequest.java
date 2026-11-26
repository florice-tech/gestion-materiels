package com.example.gestionmateriels.dto;

import java.util.List;

/** Retour du matériel : un état par article durable + observations. */
public record RetourRequest(String observations, List<DetailRetourRequest> details) {
}
