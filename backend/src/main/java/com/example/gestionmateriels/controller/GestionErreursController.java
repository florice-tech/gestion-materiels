package com.example.gestionmateriels.controller;

import com.example.gestionmateriels.service.OperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Transforme toutes les erreurs en réponse JSON { "success": false, "message": "..." },
 * pour que les pages affichent toujours un message lisible.
 */
@RestControllerAdvice
public class GestionErreursController {

    private static final Logger LOG = LoggerFactory.getLogger(GestionErreursController.class);

    // Règle métier violée ou élément introuvable
    @ExceptionHandler(OperationException.class)
    public ResponseEntity<Map<String, Object>> operation(OperationException e) {
        return Reponses.erreur(e.getStatutHttp(), e.getMessage());
    }

    // JSON mal formé, heure invalide, valeur d'enum inconnue...
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonIllisible(HttpMessageNotReadableException e) {
        return Reponses.erreur(400, "Données envoyées invalides (vérifiez les champs du formulaire).");
    }

    // Identifiant non numérique dans l'URL (ex : /api/materiels/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> mauvaisParametre(MethodArgumentTypeMismatchException e) {
        return Reponses.erreur(400, "Paramètre invalide : " + e.getName() + ".");
    }

    // Contrainte de la base violée (ex : deux ajouts simultanés du même code)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrite(DataIntegrityViolationException e) {
        LOG.warn("Contrainte de base de données violée", e);
        return Reponses.erreur(409, "Opération impossible : elle entre en conflit avec des données existantes.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methode(HttpRequestMethodNotSupportedException e) {
        return Reponses.erreur(405, "Méthode non autorisée pour cette adresse.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> introuvable(NoResourceFoundException e) {
        return Reponses.erreur(404, "Adresse introuvable.");
    }

    // Toute autre erreur : message générique, détail dans les logs du serveur
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inattendue(Exception e) throws Exception {
        if (e instanceof org.springframework.security.access.AccessDeniedException
                || e instanceof org.springframework.security.core.AuthenticationException) {
            throw e; // laissé à Spring Security (réponses 401 / 403)
        }
        LOG.error("Erreur inattendue", e);
        return Reponses.erreur(500, "Erreur interne du serveur. Réessayez ou contactez l'administrateur.");
    }
}
