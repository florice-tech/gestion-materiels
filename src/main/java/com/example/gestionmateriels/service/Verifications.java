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

    /**
     * Numéro WhatsApp : chiffres seulement, indicatif compris (228 pour le Togo).
     * « 90 12 34 56 » devient 22890123456 ; vide → null.
     */
    static String telephone(String saisie) {
        String brut = facultatif(saisie);
        if (brut == null) {
            return null;
        }
        String chiffres = brut.replaceAll("[\\s.()\\-]", "");
        if (chiffres.startsWith("+")) {
            chiffres = chiffres.substring(1);
        } else if (chiffres.startsWith("00")) {
            chiffres = chiffres.substring(2);
        }
        if (!chiffres.matches("\\d+")) {
            throw new OperationException("Le numéro ne doit contenir que des chiffres (ex. 90 12 34 56).");
        }
        if (chiffres.length() == 8) {
            chiffres = "228" + chiffres; // numéro togolais sans indicatif
        }
        if (chiffres.length() < 10 || chiffres.length() > 15) {
            throw new OperationException("Numéro invalide : 8 chiffres pour un numéro togolais, ou le numéro complet avec l'indicatif.");
        }
        return chiffres;
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
