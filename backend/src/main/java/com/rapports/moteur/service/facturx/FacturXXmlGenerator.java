package com.rapports.moteur.service.facturx;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Component
public class FacturXXmlGenerator {

    private static final DateTimeFormatter DATE_FORMATTER_102 = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Génère le flux XML Factur-X / ZUGFeRD conforme au profil demandé.
     */
    public String generateXml(FacturXInvoiceData invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rsm:CrossIndustryInvoice xmlns:rsm=\"urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100\"\n");
        sb.append("                          xmlns:qdt=\"urn:un:unece:uncefact:data:standard:QualifiedDataType:100\"\n");
        sb.append("                          xmlns:ram=\"urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100\"\n");
        sb.append("                          xmlns:udt=\"urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100\">\n");

        // 1. Contexte du document & Profil
        sb.append("  <rsm:ExchangedDocumentContext>\n");
        sb.append("    <ram:GuidelineSpecifiedDocumentContextParameter>\n");
        sb.append("      <ram:ID>").append(escapeXml(invoice.getProfile().getUrn())).append("</ram:ID>\n");
        sb.append("    </ram:GuidelineSpecifiedDocumentContextParameter>\n");
        sb.append("  </rsm:ExchangedDocumentContext>\n");

        // 2. Entête du document
        String dateStr = invoice.getIssueDate() != null ? invoice.getIssueDate().format(DATE_FORMATTER_102) : "20260101";
        sb.append("  <rsm:ExchangedDocument>\n");
        sb.append("    <ram:ID>").append(escapeXml(invoice.getInvoiceNumber())).append("</ram:ID>\n");
        sb.append("    <ram:TypeCode>").append(escapeXml(invoice.getTypeCode())).append("</ram:TypeCode>\n");
        sb.append("    <ram:IssueDateTime>\n");
        sb.append("      <udt:DateTimeString format=\"102\">").append(dateStr).append("</udt:DateTimeString>\n");
        sb.append("    </ram:IssueDateTime>\n");
        sb.append("  </rsm:ExchangedDocument>\n");

        // 3. Corps de la transaction commerciale
        sb.append("  <rsm:SupplyChainTradeTransaction>\n");

        // 3.1 Lignes d'articles (uniquement si profil BASIC ou EN16931)
        if (invoice.getProfile() != FacturXProfile.MINIMUM && invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            for (FacturXInvoiceData.FacturXItemData item : invoice.getItems()) {
                appendLineItem(sb, item, invoice.getCurrency());
            }
        }

        // 3.2 Partenaires commerciaux (Vendeur & Acheteur)
        sb.append("    <ram:ApplicableHeaderTradeAgreement>\n");
        
        // Vendeur
        sb.append("      <ram:SellerTradeParty>\n");
        sb.append("        <ram:Name>").append(escapeXml(invoice.getSellerName())).append("</ram:Name>\n");
        if (invoice.getSellerSiret() != null && !invoice.getSellerSiret().isBlank()) {
            sb.append("        <ram:SpecifiedLegalOrganization>\n");
            sb.append("          <ram:ID schemeID=\"0002\">").append(escapeXml(invoice.getSellerSiret())).append("</ram:ID>\n");
            sb.append("        </ram:SpecifiedLegalOrganization>\n");
        }
        sb.append("        <ram:PostalTradeAddress>\n");
        if (invoice.getSellerPostalCode() != null) sb.append("          <ram:PostcodeCode>").append(escapeXml(invoice.getSellerPostalCode())).append("</ram:PostcodeCode>\n");
        if (invoice.getSellerCity() != null) sb.append("          <ram:CityName>").append(escapeXml(invoice.getSellerCity())).append("</ram:CityName>\n");
        sb.append("          <ram:CountryID>").append(escapeXml(invoice.getSellerCountryCode())).append("</ram:CountryID>\n");
        sb.append("        </ram:PostalTradeAddress>\n");
        if (invoice.getSellerVatNumber() != null && !invoice.getSellerVatNumber().isBlank()) {
            sb.append("        <ram:SpecifiedTaxRegistration>\n");
            sb.append("          <ram:ID schemeID=\"VA\">").append(escapeXml(invoice.getSellerVatNumber())).append("</ram:ID>\n");
            sb.append("        </ram:SpecifiedTaxRegistration>\n");
        }
        sb.append("      </ram:SellerTradeParty>\n");

        // Acheteur
        sb.append("      <ram:BuyerTradeParty>\n");
        sb.append("        <ram:Name>").append(escapeXml(invoice.getBuyerName())).append("</ram:Name>\n");
        if (invoice.getBuyerSiret() != null && !invoice.getBuyerSiret().isBlank()) {
            sb.append("        <ram:SpecifiedLegalOrganization>\n");
            sb.append("          <ram:ID schemeID=\"0002\">").append(escapeXml(invoice.getBuyerSiret())).append("</ram:ID>\n");
            sb.append("        </ram:SpecifiedLegalOrganization>\n");
        }
        sb.append("        <ram:PostalTradeAddress>\n");
        if (invoice.getBuyerPostalCode() != null) sb.append("          <ram:PostcodeCode>").append(escapeXml(invoice.getBuyerPostalCode())).append("</ram:PostcodeCode>\n");
        if (invoice.getBuyerCity() != null) sb.append("          <ram:CityName>").append(escapeXml(invoice.getBuyerCity())).append("</ram:CityName>\n");
        sb.append("          <ram:CountryID>").append(escapeXml(invoice.getBuyerCountryCode())).append("</ram:CountryID>\n");
        sb.append("        </ram:PostalTradeAddress>\n");
        if (invoice.getBuyerVatNumber() != null && !invoice.getBuyerVatNumber().isBlank()) {
            sb.append("        <ram:SpecifiedTaxRegistration>\n");
            sb.append("          <ram:ID schemeID=\"VA\">").append(escapeXml(invoice.getBuyerVatNumber())).append("</ram:ID>\n");
            sb.append("        </ram:SpecifiedTaxRegistration>\n");
        }
        sb.append("      </ram:BuyerTradeParty>\n");
        sb.append("    </ram:ApplicableHeaderTradeAgreement>\n");

        // 3.3 Livraison (vide/standard)
        sb.append("    <ram:ApplicableHeaderTradeDelivery/>\n");

        // 3.4 Règlement & Totaux
        sb.append("    <ram:ApplicableHeaderTradeSettlement>\n");
        sb.append("      <ram:InvoiceCurrencyCode>").append(escapeXml(invoice.getCurrency())).append("</ram:InvoiceCurrencyCode>\n");

        // Taxes applicables
        BigDecimal ht = invoice.getTotalHt() != null ? invoice.getTotalHt() : BigDecimal.ZERO;
        BigDecimal tva = invoice.getTotalTva() != null ? invoice.getTotalTva() : BigDecimal.ZERO;
        BigDecimal ttc = invoice.getTotalTtc() != null ? invoice.getTotalTtc() : ht.add(tva);

        sb.append("      <ram:ApplicableTradeTax>\n");
        sb.append("        <ram:CalculatedAmount>").append(formatMoney(tva)).append("</ram:CalculatedAmount>\n");
        sb.append("        <ram:TypeCode>VAT</ram:TypeCode>\n");
        sb.append("        <ram:BasisAmount>").append(formatMoney(ht)).append("</ram:BasisAmount>\n");
        sb.append("        <ram:CategoryCode>S</ram:CategoryCode>\n");
        sb.append("        <ram:RateApplicablePercent>20.00</ram:RateApplicablePercent>\n");
        sb.append("      </ram:ApplicableTradeTax>\n");

        // Sommation monétaire
        sb.append("      <ram:SpecifiedTradeSettlementHeaderMonetarySummation>\n");
        sb.append("        <ram:LineTotalAmount>").append(formatMoney(ht)).append("</ram:LineTotalAmount>\n");
        sb.append("        <ram:TaxBasisTotalAmount>").append(formatMoney(ht)).append("</ram:TaxBasisTotalAmount>\n");
        sb.append("        <ram:TaxTotalAmount currencyID=\"").append(escapeXml(invoice.getCurrency())).append("\">")
                .append(formatMoney(tva)).append("</ram:TaxTotalAmount>\n");
        sb.append("        <ram:GrandTotalAmount>").append(formatMoney(ttc)).append("</ram:GrandTotalAmount>\n");
        sb.append("        <ram:DuePayableAmount>").append(formatMoney(ttc)).append("</ram:DuePayableAmount>\n");
        sb.append("      </ram:SpecifiedTradeSettlementHeaderMonetarySummation>\n");

        sb.append("    </ram:ApplicableHeaderTradeSettlement>\n");
        sb.append("  </rsm:SupplyChainTradeTransaction>\n");
        sb.append("</rsm:CrossIndustryInvoice>\n");

        return sb.toString();
    }

    private void appendLineItem(StringBuilder sb, FacturXInvoiceData.FacturXItemData item, String currency) {
        sb.append("    <ram:IncludedSupplyChainTradeLineItem>\n");
        sb.append("      <ram:AssociatedDocumentLineDocument>\n");
        sb.append("        <ram:LineID>").append(escapeXml(item.getLineId())).append("</ram:LineID>\n");
        sb.append("      </ram:AssociatedDocumentLineDocument>\n");
        sb.append("      <ram:SpecifiedTradeProduct>\n");
        sb.append("        <ram:Name>").append(escapeXml(item.getDescription())).append("</ram:Name>\n");
        sb.append("      </ram:SpecifiedTradeProduct>\n");
        sb.append("      <ram:SpecifiedLineTradeAgreement>\n");
        sb.append("        <ram:GrossPriceProductTradePrice>\n");
        sb.append("          <ram:ChargeAmount>").append(formatMoney(item.getUnitPrice())).append("</ram:ChargeAmount>\n");
        sb.append("        </ram:GrossPriceProductTradePrice>\n");
        sb.append("      </ram:SpecifiedLineTradeAgreement>\n");
        sb.append("      <ram:SpecifiedLineTradeDelivery>\n");
        sb.append("        <ram:BilledQuantity unitCode=\"C62\">").append(formatMoney(item.getQuantity())).append("</ram:BilledQuantity>\n");
        sb.append("      </ram:SpecifiedLineTradeDelivery>\n");
        sb.append("      <ram:SpecifiedLineTradeSettlement>\n");
        sb.append("        <ram:ApplicableTradeTax>\n");
        sb.append("          <ram:TypeCode>VAT</ram:TypeCode>\n");
        sb.append("          <ram:CategoryCode>S</ram:CategoryCode>\n");
        sb.append("          <ram:RateApplicablePercent>").append(formatMoney(item.getVatRate())).append("</ram:RateApplicablePercent>\n");
        sb.append("        </ram:ApplicableTradeTax>\n");
        sb.append("        <ram:SpecifiedTradeSettlementLineMonetarySummation>\n");
        sb.append("          <ram:LineTotalAmount>").append(formatMoney(item.getTotalHt())).append("</ram:LineTotalAmount>\n");
        sb.append("        </ram:SpecifiedTradeSettlementLineMonetarySummation>\n");
        sb.append("      </ram:SpecifiedLineTradeSettlement>\n");
        sb.append("    </ram:IncludedSupplyChainTradeLineItem>\n");
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

