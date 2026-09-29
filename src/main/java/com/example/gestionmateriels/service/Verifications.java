package com.example.gestionmateriels.service;

/** Petites vérifications de saisie communes aux services. */
final class Verifications {

    static final int LONGUEUR_MIN_MOT_DE_PASSE = 6;

    private Verifications() {
    }

    /** Renvoie le texte sans espaces autour, ou lève une erreur s'il est vide. */
    static String obligatoire(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new OperationException(message);
        }
        return valeur.trim();
    }

    /** Renvoie le texte sans espaces autour, ou null s'il est vide. */
    static String facultatif(String valeur) {
        return (valeur == null || valeur.isBlank()) ? null : valeur.trim();
    }

    static void motDePasseValide(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw new OperationException(
                    "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MOT_DE_PASSE + " caractères.");
        }
    }

    static String identifiantValide(String identifiant) {
        String id = obligatoire(identifiant, "L'identifiant est obligatoire.");
        if (!id.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new OperationException(
                    "L'identifiant doit faire 3 à 50 caractères : lettres, chiffres, point, tiret ou souligné.");
        }
        return id;
    }
}
