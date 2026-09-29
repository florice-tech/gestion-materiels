package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.service.OperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Transforme les erreurs non prévues par les contrôleurs en réponse JSON
 * { "success": false, "message": "..." }, le même format que partout ailleurs,
 * pour que les pages puissent toujours afficher un message lisible.
 */
@RestControllerAdvice
public class GestionErreursController {

    // Règle métier violée (au cas où une exception n'aurait pas été attrapée dans un contrôleur)
    @ExceptionHandler(OperationException.class)
    public ResponseEntity<Map<String, Object>> operation(OperationException e) {
        return erreur(e.getMessage());
    }

    // JSON mal formé, heure invalide, valeur d'enum inconnue, etc.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonIllisible(HttpMessageNotReadableException e) {
        return erreur("Données envoyées invalides (vérifiez les champs du formulaire).");
    }

    // Identifiant non numérique dans l'URL (ex: /api/emprunts/delegue/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> mauvaisParametre(MethodArgumentTypeMismatchException e) {
        return erreur("Paramètre invalide : " + e.getName() + ".");
    }

    private ResponseEntity<Map<String, Object>> erreur(String message) {
        Map<String, Object> reponse = new HashMap<>();
        reponse.put("success", false);
        reponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reponse);
    }
}
