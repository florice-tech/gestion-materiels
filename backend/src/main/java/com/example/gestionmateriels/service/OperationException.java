package com.example.gestionmateriels.service;

/**
 * Exception levée quand une règle métier est violée (matériel déjà emprunté, stock insuffisant...)
 * ou qu'un élément est introuvable. Le message est affiché tel quel à l'utilisateur.
 */
public class OperationException extends RuntimeException {

    private final int statutHttp;

    public OperationException(String message) {
        this(message, 400);
    }

    private OperationException(String message, int statutHttp) {
        super(message);
        this.statutHttp = statutHttp;
    }

    public static OperationException introuvable(String message) {
        return new OperationException(message, 404);
    }

    public int getStatutHttp() {
        return statutHttp;
    }
}
