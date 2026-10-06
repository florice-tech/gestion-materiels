package com.example.gestionmateriels.controller;

import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/** Réponses JSON homogènes : { "success": true|false, "message": "...", ...données }. */
final class Reponses {

    private Reponses() {
    }

    static ResponseEntity<Map<String, Object>> ok(String message) {
        return ResponseEntity.ok(corps(true, message));
    }

    static ResponseEntity<Map<String, Object>> ok(String message, String cle, Object valeur) {
        Map<String, Object> corps = corps(true, message);
        corps.put(cle, valeur);
        return ResponseEntity.ok(corps);
    }

    static ResponseEntity<Map<String, Object>> donnees(Object data) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", true);
        corps.put("data", data);
        return ResponseEntity.ok(corps);
    }

    /** Réponse 200 { success: false } : information négative qui n'est pas une erreur (ex : pas de session). */
    static ResponseEntity<Map<String, Object>> negatif(String message) {
        return ResponseEntity.ok(corps(false, message));
    }

    static ResponseEntity<Map<String, Object>> erreur(int statut, String message) {
        return ResponseEntity.status(statut).body(corps(false, message));
    }

    private static Map<String, Object> corps(boolean succes, String message) {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("success", succes);
        corps.put("message", message);
        return corps;
    }
}
