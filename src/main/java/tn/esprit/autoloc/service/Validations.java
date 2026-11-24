package tn.esprit.autoloc.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Règles de gestion communes aux services (validation déclarative au Chapitre 10). */
final class Validations {

    private Validations() {
    }

    static void exigerTexte(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException("Le champ '" + champ + "' est obligatoire");
        }
    }

    static void exigerValeur(Object valeur, String champ) {
        if (valeur == null) {
            throw new IllegalArgumentException("Le champ '" + champ + "' est obligatoire");
        }
    }

    static void exigerPositifOuNul(BigDecimal valeur, String champ) {
        exigerValeur(valeur, champ);
        if (valeur.signum() < 0) {
            throw new IllegalArgumentException("Le champ '" + champ + "' ne peut pas être négatif");
        }
    }

    static void exigerPeriodeValide(LocalDate debut, LocalDate fin) {
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début");
        }
    }

    static void exigerNouveau(Object id, String ressource) {
        if (id != null) {
            throw new IllegalArgumentException("Création de " + ressource + " impossible : l'identifiant doit être null");
        }
    }
}
