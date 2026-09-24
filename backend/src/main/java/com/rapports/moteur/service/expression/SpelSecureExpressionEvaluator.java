package com.rapports.moteur.service.expression;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implémentation haute performance et sécurisée (Anti-RCE) de l'évaluateur d'expressions.
 * Utilise SpEL restreint via SimpleEvaluationContext (aucune réflexion, pas de T(...), pas de constructeurs).
 */
@Slf4j
@Service
public class SpelSecureExpressionEvaluator implements ExpressionEvaluator {

    private static final Pattern INTERPOLATION_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private static final Pattern FUNCTION_PREFIX_PATTERN = Pattern.compile(
            "\\b(formatCurrency|formatDate|formatNumber|round|defaultVal|upper|lower)\\s*\\(");

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Method> helperMethods = new HashMap<>();

    public SpelSecureExpressionEvaluator() {
        registerHelpers();
    }

    private void registerHelpers() {
        try {
            helperMethods.put("formatCurrency", ReportHelperFunctions.class.getMethod("formatCurrency", Object.class, String.class, String.class));
            helperMethods.put("formatDate", ReportHelperFunctions.class.getMethod("formatDate", Object.class, String.class));
            helperMethods.put("formatNumber", ReportHelperFunctions.class.getMethod("formatNumber", Object.class, int.class, String.class));
            helperMethods.put("round", ReportHelperFunctions.class.getMethod("round", Object.class, int.class));
            helperMethods.put("defaultVal", ReportHelperFunctions.class.getMethod("defaultVal", Object.class, Object.class));
            helperMethods.put("upper", ReportHelperFunctions.class.getMethod("upper", Object.class));
            helperMethods.put("lower", ReportHelperFunctions.class.getMethod("lower", Object.class));
        } catch (NoSuchMethodException e) {
            log.error("Erreur lors de l'enregistrement des fonctions utilitaires de rapport", e);
        }
    }

    @Override
    public Object evaluate(String rawExpression, Map<String, Object> context) {
        if (rawExpression == null || rawExpression.isBlank()) {
            return null;
        }

        String cleaned = cleanExpression(rawExpression);
        Map<String, Object> safeContext = context != null ? context : Map.of();

        try {
            SimpleEvaluationContext evalContext = buildEvaluationContext(safeContext);
            Expression expr = parser.parseExpression(cleaned);
            return expr.getValue(evalContext);
        } catch (Exception e) {
            log.debug("Erreur d'évaluation SpEL pour l'expression '{}' : {}", rawExpression, e.getMessage());
            // Repli gracieux : si l'expression correspondait exactement à une clé brute du contexte
            return safeContext.get(cleaned);
        }
    }

    @Override
    public String evaluateString(String textWithInterpolations, Map<String, Object> context) {
        if (textWithInterpolations == null || textWithInterpolations.isBlank()) {
            return "";
        }

        Matcher matcher = INTERPOLATION_PATTERN.matcher(textWithInterpolations);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String exprContent = matcher.group(1).trim();
            Object result = evaluate(exprContent, context);
            String replacement = result != null ? result.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isBlank()) {
            return true;
        }

        String trimmed = condition.trim();
        // Permet aussi bien "remise > 0" que "{{ remise > 0 }}"
        if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) {
            trimmed = trimmed.substring(2, trimmed.length() - 2).trim();
        }
        if (trimmed.isBlank()) {
            return true;
        }

        Object result = evaluate(trimmed, context);
        if (result == null) return false;
        if (result instanceof Boolean b) return b;
        if (result instanceof Number n) return n.doubleValue() != 0;
        if (result instanceof String s) return !s.isBlank() && !"false".equalsIgnoreCase(s);
        return true;
    }

    private SimpleEvaluationContext buildEvaluationContext(Map<String, Object> context) {
        SimpleEvaluationContext.Builder builder = SimpleEvaluationContext
                .forPropertyAccessors(new MapAccessor())
                .withInstanceMethods()
                .withRootObject(context);

        SimpleEvaluationContext evalContext = builder.build();

        // Enregistre les fonctions utilitaires pour la syntaxe #formatCurrency(...)
        for (Map.Entry<String, Method> entry : helperMethods.entrySet()) {
            evalContext.setVariable(entry.getKey(), entry.getValue());
        }

        // Permet également l'accès aux variables par #varName
        if (context != null) {
            context.forEach(evalContext::setVariable);
        }

        return evalContext;
    }

    /**
     * Normalise l'expression pour les utilisateurs non-développeurs
     * (ex: "formatCurrency(montant, 'EUR')" -> "#formatCurrency(montant, 'EUR')")
     */
    private String cleanExpression(String raw) {
        String expr = raw.trim();
        // Retire les accolades {{ }} si encore présentes
        if (expr.startsWith("{{") && expr.endsWith("}}")) {
            expr = expr.substring(2, expr.length() - 2).trim();
        }
        // Préfixe automatiquement les fonctions d'aide sans #
        Matcher m = FUNCTION_PREFIX_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int start = m.start();
            if (start == 0 || expr.charAt(start - 1) != '#') {
                m.appendReplacement(sb, "#$1(");
            } else {
                m.appendReplacement(sb, "$1(");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
