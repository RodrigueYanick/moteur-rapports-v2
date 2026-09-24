package com.rapports.moteur.service.facturx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacturXInvoiceData {

    @Builder.Default
    private FacturXProfile profile = FacturXProfile.BASIC;

    @Builder.Default
    private String invoiceNumber = "INV-0001";

    @Builder.Default
    private String typeCode = "380"; // 380 = Commercial Invoice, 381 = Credit Note

    @Builder.Default
    private LocalDate issueDate = LocalDate.now();

    @Builder.Default
    private String currency = "EUR";

    // Vendeur
    @Builder.Default
    private String sellerName = "Entreprise Émettrice";
    private String sellerSiret;
    private String sellerVatNumber;
    private String sellerAddress;
    private String sellerPostalCode;
    private String sellerCity;
    @Builder.Default
    private String sellerCountryCode = "FR";

    // Acheteur
    @Builder.Default
    private String buyerName = "Client Destinataire";
    private String buyerSiret;
    private String buyerVatNumber;
    private String buyerAddress;
    private String buyerPostalCode;
    private String buyerCity;
    @Builder.Default
    private String buyerCountryCode = "FR";

    // Montants
    @Builder.Default
    private BigDecimal totalHt = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalTva = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Builder.Default
    private List<FacturXItemData> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturXItemData {
        private String lineId;
        private String description;
        @Builder.Default
        private BigDecimal quantity = BigDecimal.ONE;
        @Builder.Default
        private BigDecimal unitPrice = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal totalHt = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal vatRate = BigDecimal.valueOf(20.0);
    }

    /**
     * Mappe dynamiquement un dictionnaire de données libre vers FacturXInvoiceData.
     */
    @SuppressWarnings("unchecked")
    public static FacturXInvoiceData fromMap(Map<String, Object> data, FacturXProfile requestedProfile) {
        if (data == null) {
            return FacturXInvoiceData.builder().profile(requestedProfile).build();
        }

        FacturXInvoiceDataBuilder builder = FacturXInvoiceData.builder()
                .profile(requestedProfile != null ? requestedProfile : FacturXProfile.BASIC);

        // Numéro de facture
        String num = getString(data, "numero_facture", "facture_numero", "numeroFacture", "invoiceNumber", "reference");
        if (num != null) builder.invoiceNumber(num);

        // Vendeur
        String seller = getString(data, "vendeur_nom", "emetteur_nom", "entreprise_nom", "company_name", "sellerName");
        if (seller != null) builder.sellerName(seller);
        String sellerSiret = getString(data, "vendeur_siret", "siret_emetteur", "siret");
        if (sellerSiret != null) builder.sellerSiret(sellerSiret);
        String sellerVat = getString(data, "vendeur_tva", "tva_intracommunautaire", "sellerVatNumber");
        if (sellerVat != null) builder.sellerVatNumber(sellerVat);

        // Acheteur
        String buyer = getString(data, "client_nom", "acheteur_nom", "client", "destinataire", "buyerName");
        if (buyer != null) builder.buyerName(buyer);
        String buyerSiret = getString(data, "client_siret", "buyerSiret");
        if (buyerSiret != null) builder.buyerSiret(buyerSiret);
        String buyerVat = getString(data, "client_tva", "buyerVatNumber");
        if (buyerVat != null) builder.buyerVatNumber(buyerVat);

        // Totaux
        BigDecimal ht = getBigDecimal(data, "total_ht", "totalHt", "montant_ht", "sous_total");
        BigDecimal tva = getBigDecimal(data, "total_tva", "totalTva", "montant_tva");
        BigDecimal ttc = getBigDecimal(data, "total_ttc", "totalTtc", "montant_ttc", "total");

        if (ht != null) builder.totalHt(ht);
        if (tva != null) builder.totalTva(tva);
        if (ttc != null) builder.totalTtc(ttc);
        else if (ht != null && tva != null) builder.totalTtc(ht.add(tva));

        // Articles / Lignes
        Object lignesObj = data.getOrDefault("lignes", data.getOrDefault("articles", data.get("items")));
        List<FacturXItemData> itemsList = new ArrayList<>();
        if (lignesObj instanceof List<?> list) {
            int idx = 1;
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> rowMap) {
                    Map<String, Object> row = (Map<String, Object>) rowMap;
                    String desc = getString(row, "designation", "description", "nom", "titre", "libelle");
                    BigDecimal qte = getBigDecimal(row, "quantite", "qte", "quantity");
                    BigDecimal prix = getBigDecimal(row, "prix_unitaire", "pu", "prix", "unitPrice");
                    BigDecimal totalLigne = getBigDecimal(row, "total_ht", "total", "montant");

                    if (qte == null) qte = BigDecimal.ONE;
                    if (prix == null) prix = BigDecimal.ZERO;
                    if (totalLigne == null) totalLigne = prix.multiply(qte);

                    itemsList.add(FacturXItemData.builder()
                            .lineId(String.valueOf(idx++))
                            .description(desc != null ? desc : "Article " + (idx - 1))
                            .quantity(qte)
                            .unitPrice(prix)
                            .totalHt(totalLigne)
                            .vatRate(BigDecimal.valueOf(20.0))
                            .build());
                }
            }
        }
        builder.items(itemsList);

        return builder.build();
    }

    private static String getString(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null && !v.toString().isBlank()) {
                return v.toString().trim();
            }
        }
        return null;
    }

    private static BigDecimal getBigDecimal(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v instanceof Number num) {
                return BigDecimal.valueOf(num.doubleValue());
            } else if (v instanceof String str && !str.isBlank()) {
                try {
                    return new BigDecimal(str.replace(",", ".").trim());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}

