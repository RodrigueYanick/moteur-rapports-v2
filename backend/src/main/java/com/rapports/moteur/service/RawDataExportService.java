package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Service d'export des données brutes en formats CSV (compatible Excel FR avec UTF-8 BOM)
 * et JSON indenté enrichi de métadonnées d'export.
 */
@Slf4j
@Service
public class RawDataExportService {

    private final ObjectMapper objectMapper;

    public RawDataExportService() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Exporte les données d'un modèle sous forme de fichier CSV formaté.
     *
     * @param template  Modèle de rapport
     * @param data      Données résolues du rapport
     * @param delimiter Délimiteur de champ (défaut ';' pour compatibilité Excel FR, ou ',')
     * @return Octets du fichier CSV avec BOM UTF-8
     */
    public byte[] exportToCsv(ReportTemplate template, Map<String, Object> data, Character delimiter) {
        char sep = (delimiter != null && (delimiter == ';' || delimiter == ',' || delimiter == '\t')) ? delimiter : ';';

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Écriture du BOM UTF-8 (Byte Order Mark) pour garantir l'ouverture sans accroc dans Excel FR
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);

            try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setDelimiter(sep).build())) {

                // Recherche d'une collection tabulaire principale (ex: 'lignes', 'items', 'data', ou première liste de Maps)
                List<Map<String, Object>> mainTable = extractMainTable(data);

                if (mainTable != null && !mainTable.isEmpty()) {
                    exportTabularData(csvPrinter, data, mainTable);
                } else {
                    exportKeyValueData(csvPrinter, template, data);
                }

                csvPrinter.flush();
            }

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'export CSV pour le template {}", template.getId(), e);
            throw new IllegalStateException("Échec de la génération de l'export CSV : " + e.getMessage(), e);
        }
    }

    /**
     * Exporte les données brutes sous format JSON indenté enrichi de métadonnées.
     *
     * @param template Modèle de rapport
     * @param data     Données résolues
     * @return Octets du fichier JSON
     */
    public byte[] exportToJson(ReportTemplate template, Map<String, Object> data) {
        try {
            Map<String, Object> enrichedPayload = new LinkedHashMap<>();
            enrichedPayload.put("templateId", template.getId() != null ? template.getId().toString() : null);
            enrichedPayload.put("templateNom", template.getNom());
            enrichedPayload.put("codeEntreprise", template.getCodeEntreprise());
            enrichedPayload.put("version", template.getVersion());
            enrichedPayload.put("statut", template.getStatut() != null ? template.getStatut().name() : null);
            enrichedPayload.put("dateExport", Instant.now().toString());

            List<Map<String, Object>> mainTable = extractMainTable(data);
            if (mainTable != null) {
                enrichedPayload.put("totalRecords", mainTable.size());
            }

            enrichedPayload.put("donnees", data);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(enrichedPayload);
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'export JSON pour le template {}", template.getId(), e);
            throw new IllegalStateException("Échec de la génération de l'export JSON : " + e.getMessage(), e);
        }
    }

    // ============================================================
    // Stratégies d'export CSV
    // ============================================================

    private void exportTabularData(CSVPrinter csvPrinter, Map<String, Object> fullData, List<Map<String, Object>> table) throws Exception {
        // Collecter les colonnes du tableau
        Set<String> tableHeaders = new LinkedHashSet<>();
        for (Map<String, Object> row : table) {
            if (row != null) {
                tableHeaders.addAll(row.keySet());
            }
        }

        // Collecter les variables scalaires de contexte de niveau supérieur (ex: numero_facture, date, nom_client)
        Map<String, Object> contextScalars = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fullData.entrySet()) {
            if (!(entry.getValue() instanceof List) && !(entry.getValue() instanceof Map)) {
                contextScalars.put(entry.getKey(), entry.getValue());
            }
        }

        // En-têtes finaux : Scalaires de contexte d'abord, puis colonnes du tableau
        List<String> finalHeaders = new ArrayList<>(contextScalars.keySet());
        finalHeaders.addAll(tableHeaders);

        csvPrinter.printRecord(finalHeaders);

        // Écriture de chaque ligne de données
        for (Map<String, Object> row : table) {
            List<Object> recordValues = new ArrayList<>();
            // Ajout des scalaires de contexte
            for (String key : contextScalars.keySet()) {
                recordValues.add(formatValue(contextScalars.get(key)));
            }
            // Ajout des valeurs de la ligne
            for (String col : tableHeaders) {
                recordValues.add(formatValue(row.get(col)));
            }
            csvPrinter.printRecord(recordValues);
        }
    }

    private void exportKeyValueData(CSVPrinter csvPrinter, ReportTemplate template, Map<String, Object> data) throws Exception {
        csvPrinter.printRecord("Paramètre", "Valeur");
        csvPrinter.printRecord("Template", template.getNom());
        if (template.getCodeEntreprise() != null) {
            csvPrinter.printRecord("Entreprise", template.getCodeEntreprise());
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            csvPrinter.printRecord(entry.getKey(), formatValue(entry.getValue()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMainTable(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        // Priorité aux noms usuels
        String[] preferredKeys = {"lignes", "items", "data", "articles", "commandes", "factures", "lignes_commande"};
        for (String key : preferredKeys) {
            Object val = data.get(key);
            if (val instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
        }

        // Sinon recherche de la première clé contenant une liste d'objets
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
                return (List<Map<String, Object>>) list;
            }
        }

        return null;
    }

    private String formatValue(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof Map || val instanceof List) {
            try {
                return objectMapper.writeValueAsString(val);
            } catch (Exception e) {
                return val.toString();
            }
        }
        return val.toString();
    }
}
