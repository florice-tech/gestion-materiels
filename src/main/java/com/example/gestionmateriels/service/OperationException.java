package com.example.gestionmateriels.service;

/**
 * Exception levée quand une règle métier est violée
 * (ex : matériel déjà emprunté, stock insuffisant, agent introuvable).
 */
public class OperationException extends RuntimeException {
    public OperationException(String message) {
        super(message);
    }
}