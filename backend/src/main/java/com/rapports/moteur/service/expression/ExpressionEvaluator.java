package com.rapports.moteur.service.expression;

import java.util.Map;

/**
 * Contrat pour l'évaluation sécurisée d'expressions dynamiques et de conditions.
 */
public interface ExpressionEvaluator {

    /**
     * Évalue une expression unique (ex: "quantite * prix", "client.nom", "#formatCurrency(total)")
     * dans le contexte fourni.
     *
     * @param expression l'expression brute (sans les accolades {{ }})
     * @param context    le dictionnaire de variables
     * @return le résultat de l'évaluation, ou null
     */
    Object evaluate(String expression, Map<String, Object> context);

    /**
     * Résout toutes les interpolations {{ ... }} contenues dans une chaîne de texte.
     *
     * @param textWithInterpolations texte contenant potentiellement des variables {{ expr }}
     * @param context                le dictionnaire de variables
     * @return le texte avec les interpolations résolues
     */
    String evaluateString(String textWithInterpolations, Map<String, Object> context);

    /**
     * Évalue une condition booléenne (ex: "remise > 0", "statut == 'VALIDE' && total >= 100").
     * Retourne true si la condition est vide ou nulle.
     *
     * @param condition l'expression conditionnelle (avec ou sans {{ }})
     * @param context   le dictionnaire de variables
     * @return true si la condition est validée, false sinon
     */
    boolean evaluateCondition(String condition, Map<String, Object> context);
}

