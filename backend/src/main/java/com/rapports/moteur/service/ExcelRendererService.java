package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.entity.ReportTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ExcelRendererService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public byte[] renderToExcel(ReportTemplate template, Map<String, Object> data) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Styles styles = new Styles(workbook);

            // 1. Feuille de Synthèse
            createSummarySheet(workbook, styles, template, data);

            // 2. Feuilles des Tableaux
            createTableSheets(workbook, styles, template, data);

            // 3. Export binaire
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        } catch (IOException e) {
            log.error("Erreur lors de la génération du classeur Excel pour le modèle {}", template.getId(), e);
            throw new IllegalStateException("Impossible de générer le fichier Excel : " + e.getMessage(), e);
        }
    }

    /**
     * Crée la feuille d'accueil "Synthèse" avec les informations du modèle et les variables scalaires.
     */
    private void createSummarySheet(XSSFWorkbook workbook, Styles styles, ReportTemplate template, Map<String, Object> data) {
        XSSFSheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Synthèse"));
        sheet.setDisplayGridlines(true);

        int rowIdx = 0;

        // Titre du modèle
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.setHeightInPoints(28);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(template.getNom() != null ? template.getNom() : "Rapport");
        titleCell.setCellStyle(styles.mainTitle);

        // Sous-titre / Métadonnées
        Row subRow = sheet.createRow(rowIdx++);
        Cell subCell = subRow.createCell(0);
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String subInfo = String.format("Généré le : %s  |  Statut : %s  |  Version : %s  |  Catégorie : %s",
                dateStr,
                template.getStatut() != null ? template.getStatut().name() : "N/A",
                template.getVersion() != null ? template.getVersion() : "1",
                template.getCategorie() != null ? template.getCategorie() : "AUTRES");
        subCell.setCellValue(subInfo);
        subCell.setCellStyle(styles.subTitle);

        if (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank()) {
            Row entRow = sheet.createRow(rowIdx++);
            Cell entCell = entRow.createCell(0);
            entCell.setCellValue("Entreprise : " + template.getCodeEntreprise());
            entCell.setCellStyle(styles.subTitle);
        }

        rowIdx++; // Ligne vide

        // Section Variables Clés
        Row secHeaderRow = sheet.createRow(rowIdx++);
        secHeaderRow.setHeightInPoints(22);
        Cell secCell = secHeaderRow.createCell(0);
        secCell.setCellValue("Variables & Indicateurs Clés");
        secCell.setCellStyle(styles.sectionHeader);

        // En-têtes du tableau récapitulatif
        Row tableHeadRow = sheet.createRow(rowIdx++);
        tableHeadRow.setHeightInPoints(20);
        Cell th1 = tableHeadRow.createCell(0);
        th1.setCellValue("Paramètre / Variable");
        th1.setCellStyle(styles.tableHeader);
        Cell th2 = tableHeadRow.createCell(1);
        th2.setCellValue("Valeur");
        th2.setCellStyle(styles.tableHeader);

        // Remplissage avec les variables scalaires
        boolean zebra = false;
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object val = entry.getValue();
                // On n'inclut pas les listes (qui auront leur onglet dédié) ni les maps complexes
                if (val instanceof List || val instanceof Map) {
                    continue;
                }
                Row dataRow = sheet.createRow(rowIdx++);
                Cell cKey = dataRow.createCell(0);
                cKey.setCellValue(entry.getKey());
                cKey.setCellStyle(zebra ? styles.zebraText : styles.normalText);

                Cell cVal = dataRow.createCell(1);
                setCellValueTyped(cVal, val, styles, zebra);
                zebra = !zebra;
            }
        }

        if (template.getDescription() != null && !template.getDescription().isBlank()) {
            Row descRow = sheet.createRow(rowIdx++);
            Cell cKey = descRow.createCell(0);
            cKey.setCellValue("Description");
            cKey.setCellStyle(styles.normalText);
            Cell cVal = descRow.createCell(1);
            cVal.setCellValue(template.getDescription());
            cVal.setCellStyle(styles.normalText);
        }

        // Ajustement automatique des largeurs de colonnes
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        if (sheet.getColumnWidth(0) < 25 * 256) sheet.setColumnWidth(0, 25 * 256);
        if (sheet.getColumnWidth(1) < 35 * 256) sheet.setColumnWidth(1, 35 * 256);
    }

    /**
     * Crée des feuilles Excel dédiées pour chaque tableau dynamique ou jeu de données tubulaire.
     */
    private void createTableSheets(XSSFWorkbook workbook, Styles styles, ReportTemplate template, Map<String, Object> data) {
        Set<String> processedSources = new HashSet<>();
        int tableCounter = 1;

        // 1. Analyse des blocs du design
        if (template.getContenuDesign() != null && !template.getContenuDesign().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(template.getContenuDesign());
                List<JsonNode> tableBlocks = extractTableBlocks(root);

                for (JsonNode tableBlock : tableBlocks) {
                    String source = stripBraces(tableBlock.path("source").asText(""));
                    String sheetTitle = tableBlock.path("nom").asText("").trim();
                    if (sheetTitle.isEmpty()) {
                        sheetTitle = !source.isEmpty() ? source : "Tableau " + tableCounter;
                    }

                    List<ColumnDef> columns = extractColumns(tableBlock);
                    Object tableDataObj = data != null ? data.get(source) : null;

                    if (tableDataObj instanceof List<?> list) {
                        createTableSheet(workbook, styles, sheetTitle, columns, list);
                        if (!source.isEmpty()) {
                            processedSources.add(source);
                        }
                        tableCounter++;
                    }
                }
            } catch (Exception e) {
                log.warn("Impossible d'extraire la configuration des tableaux depuis contenuDesign : {}", e.getMessage());
            }
        }

        // 2. Parcourir les listes restantes dans 'data' qui n'étaient pas liées à un bloc design
        if (data != null) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                if (!processedSources.contains(key) && entry.getValue() instanceof List<?> list && !list.isEmpty()) {
                    List<ColumnDef> inferredColumns = inferColumnsFromList(list);
                    if (!inferredColumns.isEmpty()) {
                        createTableSheet(workbook, styles, key, inferredColumns, list);
                        processedSources.add(key);
                        tableCounter++;
                    }
                }
            }
        }
    }

    /**
     * Construit une feuille dédiée pour une table avec titres, styles, typage, filtres et totaux.
     */
    private void createTableSheet(XSSFWorkbook workbook, Styles styles, String rawTitle, List<ColumnDef> columns, List<?> rows) {
        String safeName = WorkbookUtil.createSafeSheetName(rawTitle);
        // Éviter les doublons de noms d'onglets
        int suffix = 1;
        String finalName = safeName;
        while (workbook.getSheet(finalName) != null) {
            finalName = safeName.substring(0, Math.min(26, safeName.length())) + "_" + suffix++;
        }

        XSSFSheet sheet = workbook.createSheet(finalName);
        sheet.setDisplayGridlines(true);

        int rowIdx = 0;

        // Bandeau titre du tableau
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(rawTitle);
        titleCell.setCellStyle(styles.sectionHeader);

        rowIdx++; // Ligne vide

        // Ligne d'en-tête de colonnes
        int headerRowIndex = rowIdx++;
        Row headerRow = sheet.createRow(headerRowIndex);
        headerRow.setHeightInPoints(22);

        for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
            Cell cell = headerRow.createCell(colIdx);
            cell.setCellValue(columns.get(colIdx).title);
            cell.setCellStyle(styles.tableHeader);
        }

        // Lignes de données
        boolean[] isNumericCol = new boolean[columns.size()];
        Arrays.fill(isNumericCol, true);
        boolean hasRows = !rows.isEmpty();
        boolean zebra = false;

        for (Object rowItem : rows) {
            Row dataRow = sheet.createRow(rowIdx++);
            dataRow.setHeightInPoints(18);

            Map<String, Object> rowMap = toMap(rowItem);

            for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                ColumnDef col = columns.get(colIdx);
                Object val = rowMap != null ? rowMap.get(col.field) : null;
                Cell cell = dataRow.createCell(colIdx);

                if (val instanceof Number) {
                    setCellValueTyped(cell, val, styles, zebra);
                } else if (val != null && isNumericString(val.toString())) {
                    try {
                        double parsed = Double.parseDouble(val.toString().trim().replace(',', '.'));
                        setCellValueTyped(cell, parsed, styles, zebra);
                    } catch (NumberFormatException ignored) {
                        isNumericCol[colIdx] = false;
                        setCellValueTyped(cell, val, styles, zebra);
                    }
                } else {
                    if (val != null) {
                        isNumericCol[colIdx] = false;
                    }
                    setCellValueTyped(cell, val, styles, zebra);
                }
            }
            zebra = !zebra;
        }

        int lastDataRowIndex = rowIdx - 1;

        // Ligne de Totaux si au moins une colonne numérique existe et qu'il y a des données
        if (hasRows) {
            boolean hasAnyNumeric = false;
            for (boolean num : isNumericCol) {
                if (num) {
                    hasAnyNumeric = true;
                    break;
                }
            }

            if (hasAnyNumeric) {
                Row totalRow = sheet.createRow(rowIdx++);
                totalRow.setHeightInPoints(20);

                Cell labelCell = totalRow.createCell(0);
                labelCell.setCellValue("TOTAL");
                labelCell.setCellStyle(styles.totalLabel);

                for (int colIdx = 1; colIdx < columns.size(); colIdx++) {
                    Cell cell = totalRow.createCell(colIdx);
                    if (isNumericCol[colIdx]) {
                        String colLetter = CellReference.convertNumToColString(colIdx);
                        int startExcelRow = headerRowIndex + 2; // 1-based in Excel
                        int endExcelRow = lastDataRowIndex + 1;
                        cell.setCellFormula(String.format("SUM(%s%d:%s%d)", colLetter, startExcelRow, colLetter, endExcelRow));
                        cell.setCellStyle(styles.totalNumeric);
                    } else {
                        cell.setCellValue("");
                        cell.setCellStyle(styles.totalEmpty);
                    }
                }
            }
        }

        // AutoFilter sur la table
        if (hasRows) {
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastDataRowIndex, 0, columns.size() - 1));
        }

        // Figer l'en-tête (Freeze Pane)
        sheet.createFreezePane(0, headerRowIndex + 1);

        // Dimensionnement automatique des colonnes
        for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
            sheet.autoSizeColumn(colIdx);
            int currentWidth = sheet.getColumnWidth(colIdx);
            int minWidth = 14 * 256;
            int maxWidth = 50 * 256;
            if (currentWidth < minWidth) sheet.setColumnWidth(colIdx, minWidth);
            if (currentWidth > maxWidth) sheet.setColumnWidth(colIdx, maxWidth);
        }
    }

    private void setCellValueTyped(Cell cell, Object val, Styles styles, boolean zebra) {
        if (val == null) {
            cell.setCellValue("");
            cell.setCellStyle(zebra ? styles.zebraText : styles.normalText);
        } else if (val instanceof Integer || val instanceof Long || val instanceof Short) {
            cell.setCellValue(((Number) val).doubleValue());
            cell.setCellStyle(zebra ? styles.zebraInteger : styles.normalInteger);
        } else if (val instanceof Number) {
            cell.setCellValue(((Number) val).doubleValue());
            cell.setCellStyle(zebra ? styles.zebraDecimal : styles.normalDecimal);
        } else if (val instanceof Boolean b) {
            cell.setCellValue(b ? "Oui" : "Non");
            cell.setCellStyle(zebra ? styles.zebraCenter : styles.normalCenter);
        } else {
            cell.setCellValue(val.toString());
            cell.setCellStyle(zebra ? styles.zebraText : styles.normalText);
        }
    }

    private boolean isNumericString(String str) {
        if (str == null || str.isBlank()) return false;
        return str.trim().matches("^-?\\d+(\\.\\d+)?$");
    }

    private List<JsonNode> extractTableBlocks(JsonNode root) {
        List<JsonNode> tables = new ArrayList<>();
        if (root.has("pages") && root.path("pages").isArray()) {
            for (JsonNode page : root.path("pages")) {
                JsonNode blocs = page.path("blocs");
                if (blocs.isArray()) {
                    for (JsonNode b : blocs) {
                        if ("tableau".equalsIgnoreCase(b.path("type").asText())) {
                            tables.add(b);
                        }
                    }
                }
            }
        } else if (root.has("blocs") && root.path("blocs").isArray()) {
            for (JsonNode b : root.path("blocs")) {
                if ("tableau".equalsIgnoreCase(b.path("type").asText())) {
                    tables.add(b);
                }
            }
        }
        return tables;
    }

    private List<ColumnDef> extractColumns(JsonNode tableBlock) {
        List<ColumnDef> cols = new ArrayList<>();
        JsonNode colonnesNode = tableBlock.path("colonnes");
        if (colonnesNode.isArray() && colonnesNode.size() > 0) {
            for (JsonNode c : colonnesNode) {
                String var = c.path("variable").asText("").trim();
                String title = c.path("titre").asText("").trim();
                if (title.isEmpty()) title = var;
                if (!var.isEmpty()) {
                    cols.add(new ColumnDef(title, var));
                }
            }
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    private List<ColumnDef> inferColumnsFromList(List<?> list) {
        List<ColumnDef> cols = new ArrayList<>();
        if (list == null || list.isEmpty()) return cols;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                String name = key.toString();
                cols.add(new ColumnDef(capitalize(name), name));
            }
        }
        return cols;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        try {
            return objectMapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String stripBraces(String s) {
        if (s == null) return "";
        return s.replace("{{", "").replace("}}", "").trim();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace('_', ' ');
    }

    private static class ColumnDef {
        final String title;
        final String field;

        ColumnDef(String title, String field) {
            this.title = title;
            this.field = field;
        }
    }

    /**
     * Registre de styles POI réutilisables.
     */
    private static class Styles {
        final CellStyle mainTitle;
        final CellStyle subTitle;
        final CellStyle sectionHeader;
        final CellStyle tableHeader;
        final CellStyle normalText;
        final CellStyle zebraText;
        final CellStyle normalInteger;
        final CellStyle zebraInteger;
        final CellStyle normalDecimal;
        final CellStyle zebraDecimal;
        final CellStyle normalCenter;
        final CellStyle zebraCenter;
        final CellStyle totalLabel;
        final CellStyle totalNumeric;
        final CellStyle totalEmpty;

        Styles(XSSFWorkbook wb) {
            DataFormat df = wb.createDataFormat();

            // Couleurs
            byte[] primaryRgb = new byte[]{(byte) 67, (byte) 56, (byte) 202};   // Indigo #4338ca
            byte[] zebraRgb = new byte[]{(byte) 248, (byte) 250, (byte) 252};   // Slate 50 #f8fafc

            XSSFColor primaryColor = new XSSFColor(primaryRgb, new DefaultIndexedColorMap());
            XSSFColor zebraColor = new XSSFColor(zebraRgb, new DefaultIndexedColorMap());

            // Polices
            XSSFFont fTitle = wb.createFont();
            fTitle.setFontName("Segoe UI");
            fTitle.setFontHeightInPoints((short) 16);
            fTitle.setBold(true);
            fTitle.setColor(primaryColor);

            XSSFFont fSub = wb.createFont();
            fSub.setFontName("Segoe UI");
            fSub.setFontHeightInPoints((short) 9);
            fSub.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            XSSFFont fSec = wb.createFont();
            fSec.setFontName("Segoe UI");
            fSec.setFontHeightInPoints((short) 12);
            fSec.setBold(true);
            fSec.setColor(primaryColor);

            XSSFFont fHead = wb.createFont();
            fHead.setFontName("Segoe UI");
            fHead.setFontHeightInPoints((short) 10);
            fHead.setBold(true);
            fHead.setColor(IndexedColors.WHITE.getIndex());

            XSSFFont fNorm = wb.createFont();
            fNorm.setFontName("Segoe UI");
            fNorm.setFontHeightInPoints((short) 10);

            XSSFFont fBold = wb.createFont();
            fBold.setFontName("Segoe UI");
            fBold.setFontHeightInPoints((short) 10);
            fBold.setBold(true);

            // 1. Titres
            mainTitle = wb.createCellStyle();
            mainTitle.setFont(fTitle);

            subTitle = wb.createCellStyle();
            subTitle.setFont(fSub);

            sectionHeader = wb.createCellStyle();
            sectionHeader.setFont(fSec);

            // 2. En-tête de tableau
            tableHeader = wb.createCellStyle();
            tableHeader.setFont(fHead);
            tableHeader.setFillForegroundColor(primaryColor);
            tableHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeader.setAlignment(HorizontalAlignment.CENTER);
            tableHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(tableHeader, BorderStyle.THIN);

            // 3. Normal / Zebra
            normalText = createBase(wb, fNorm, null, HorizontalAlignment.LEFT);
            zebraText = createBase(wb, fNorm, zebraColor, HorizontalAlignment.LEFT);

            normalCenter = createBase(wb, fNorm, null, HorizontalAlignment.CENTER);
            zebraCenter = createBase(wb, fNorm, zebraColor, HorizontalAlignment.CENTER);

            short fmtInt = df.getFormat("#,##0");
            normalInteger = createBase(wb, fNorm, null, HorizontalAlignment.RIGHT);
            normalInteger.setDataFormat(fmtInt);
            zebraInteger = createBase(wb, fNorm, zebraColor, HorizontalAlignment.RIGHT);
            zebraInteger.setDataFormat(fmtInt);

            short fmtDec = df.getFormat("#,##0.00");
            normalDecimal = createBase(wb, fNorm, null, HorizontalAlignment.RIGHT);
            normalDecimal.setDataFormat(fmtDec);
            zebraDecimal = createBase(wb, fNorm, zebraColor, HorizontalAlignment.RIGHT);
            zebraDecimal.setDataFormat(fmtDec);

            // 4. Ligne de Totaux
            totalLabel = wb.createCellStyle();
            totalLabel.setFont(fBold);
            totalLabel.setAlignment(HorizontalAlignment.LEFT);
            totalLabel.setBorderTop(BorderStyle.THIN);
            totalLabel.setBorderBottom(BorderStyle.DOUBLE);

            totalNumeric = wb.createCellStyle();
            totalNumeric.setFont(fBold);
            totalNumeric.setDataFormat(fmtDec);
            totalNumeric.setAlignment(HorizontalAlignment.RIGHT);
            totalNumeric.setBorderTop(BorderStyle.THIN);
            totalNumeric.setBorderBottom(BorderStyle.DOUBLE);

            totalEmpty = wb.createCellStyle();
            totalEmpty.setBorderTop(BorderStyle.THIN);
            totalEmpty.setBorderBottom(BorderStyle.DOUBLE);
        }

        private CellStyle createBase(XSSFWorkbook wb, Font font, XSSFColor bg, HorizontalAlignment align) {
            CellStyle cs = wb.createCellStyle();
            cs.setFont(font);
            cs.setAlignment(align);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorders(cs, BorderStyle.THIN);
            if (bg != null) {
                cs.setFillForegroundColor(bg);
                cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            return cs;
        }

        private void applyBorders(CellStyle cs, BorderStyle b) {
            cs.setBorderTop(b);
            cs.setBorderBottom(b);
            cs.setBorderLeft(b);
            cs.setBorderRight(b);
            cs.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cs.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cs.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cs.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
    }
}
