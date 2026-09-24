package com.rapports.moteur.service.expression;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Fonctions utilitaires sécurisées exposées dans le moteur d'expressions des rapports.
 * Toutes les méthodes sont statiques, pures et sans effets de bord.
 */
@Slf4j
public final class ReportHelperFunctions {

    private ReportHelperFunctions() {}

    /**
     * Formate un montant en devise selon une locale (ex: #formatCurrency(1250.5, 'EUR', 'fr-FR') -> 1 250,50 €)
     */
    public static String formatCurrency(Object amount, String currencyCode, String localeTag) {
        if (amount == null) return "";
        BigDecimal value = toBigDecimal(amount);
        if (value == null) return String.valueOf(amount);

        Locale locale = resolveLocale(localeTag);
        try {
            NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
            if (currencyCode != null && !currencyCode.isBlank()) {
                nf.setCurrency(Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT)));
            }
            return nf.format(value).replace("\u00a0", " ").replace("\u202f", " "); // Normalise les espaces insécables pour le HTML/PDF
        } catch (Exception e) {
            log.warn("Erreur lors du formatage devise: amount={}, code={}, locale={}", amount, currencyCode, localeTag, e);
            return value.toPlainString() + " " + (currencyCode != null ? currencyCode : "");
        }
    }

    public static String formatCurrency(Object amount, String currencyCode) {
        return formatCurrency(amount, currencyCode, "fr-FR");
    }

    public static String formatCurrency(Object amount) {
        return formatCurrency(amount, "EUR", "fr-FR");
    }

    /**
     * Formate un nombre avec un nombre de décimales donné (ex: #formatNumber(1250.555, 2, 'fr-FR') -> 1 250,56)
     */
    public static String formatNumber(Object number, int decimals, String localeTag) {
        if (number == null) return "";
        BigDecimal value = toBigDecimal(number);
        if (value == null) return String.valueOf(number);

        Locale locale = resolveLocale(localeTag);
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            nf.setMinimumFractionDigits(decimals);
            nf.setMaximumFractionDigits(decimals);
            return nf.format(value).replace("\u00a0", " ").replace("\u202f", " ");
        } catch (Exception e) {
            return value.setScale(decimals, RoundingMode.HALF_UP).toPlainString();
        }
    }

    public static String formatNumber(Object number, int decimals) {
        return formatNumber(number, decimals, "fr-FR");
    }

    /**
     * Formate une date ou un timestamp en chaîne formatée (ex: #formatDate('2026-09-15', 'dd/MM/yyyy') -> 15/09/2026)
     */
    public static String formatDate(Object dateObj, String pattern) {
        if (dateObj == null) return "";
        String fmt = (pattern != null && !pattern.isBlank()) ? pattern : "dd/MM/yyyy";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmt);

        try {
            if (dateObj instanceof LocalDate ld) {
                return ld.format(formatter);
            } else if (dateObj instanceof LocalDateTime ldt) {
                return ldt.format(formatter);
            } else if (dateObj instanceof String str) {
                String trimmed = str.trim();
                if (trimmed.contains("T")) {
                    return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_DATE_TIME).format(formatter);
                } else if (trimmed.length() == 10 && trimmed.contains("-")) {
                    return LocalDate.parse(trimmed, DateTimeFormatter.ISO_DATE).format(formatter);
                }
            }
        } catch (Exception e) {
            log.debug("Impossible de formater la date '{}' avec le motif '{}': {}", dateObj, pattern, e.getMessage());
        }
        return String.valueOf(dateObj);
    }

    public static String formatDate(Object dateObj) {
        return formatDate(dateObj, "dd/MM/yyyy");
    }

    /**
     * Arrondit un nombre à N décimales.
     */
    public static BigDecimal round(Object number, int decimals) {
        if (number == null) return BigDecimal.ZERO;
        BigDecimal val = toBigDecimal(number);
        return val != null ? val.setScale(decimals, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Retourne une valeur de repli si la valeur est null ou chaîne vide.
     */
    public static Object defaultVal(Object value, Object fallback) {
        if (value == null) return fallback;
        if (value instanceof String s && s.isBlank()) return fallback;
        return value;
    }

    /**
     * Met en majuscules.
     */
    public static String upper(Object text) {
        return text == null ? "" : text.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Met en minuscules.
     */
    public static String lower(Object text) {
        return text == null ? "" : text.toString().toLowerCase(Locale.ROOT);
    }

    private static Locale resolveLocale(String localeTag) {
        if (localeTag == null || localeTag.isBlank()) {
            return Locale.FRANCE;
        }
        return Locale.forLanguageTag(localeTag.trim());
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(val.toString().trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }
}
