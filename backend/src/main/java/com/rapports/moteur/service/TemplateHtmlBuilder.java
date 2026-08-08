package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateHtmlBuilder {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodeGeneratorService codeGenerator;

    public TemplateHtmlBuilder(CodeGeneratorService codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    public String build(String contenuDesignJson, Map<String, Object> data) {
        StringBuilder html = new StringBuilder("<html><head><meta charset='UTF-8'/><style>body{margin:0;}</style></head><body>");
        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            if (root.has("blocs")) {
                html.append("<div style='position:relative;width:794px;height:1123px;background:white;'>");
                for (JsonNode bloc : root.path("blocs")) {
                    int x = bloc.path("x").asInt(0);
                    int y = bloc.path("y").asInt(0);
                    html.append("<div style='position:absolute;left:").append(x).append("px;top:").append(y).append("px;'>");
                    html.append(renderBloc(bloc, data));
                    html.append("</div>");
                }
                html.append("</div>");
            }
        } catch (Exception e) {
            html.append("<p>Erreur de design : ").append(e.getMessage()).append("</p>");
        }
        return html.append("</body></html>").toString();
    }

    private String renderBloc(JsonNode bloc, Map<String, Object> data) {
        String type = bloc.path("type").asText();
        switch (type) {
            case "titre":
                return "<h1 style='" + buildStyle(bloc) + "'>" + replaceVars(bloc.path("contenu").asText(""), data) + "</h1>";
            case "texte":
                return "<p style='" + buildStyle(bloc) + "'>" + replaceVars(bloc.path("contenu").asText(""), data) + "</p>";
            case "tableau":
                return bloc.has("lignes") ? renderStaticTable(bloc, data) : renderTableau(bloc, data);
            case "ligne": {
                int epaisseur = bloc.path("style").path("epaisseur").asInt(1);
                String couleur = bloc.path("style").path("couleur").asText("#000000");
                int largeur = bloc.path("style").path("largeur").asInt(100);
                return "<hr style='border-top:" + epaisseur + "px solid " + escape(couleur) + "; width:" + largeur + "%;' />";
            }
            case "image": {
                String url = replaceVars(bloc.path("url").asText(""), data);
                int largeur = bloc.path("style").path("largeur").asInt(100);
                String alignement = bloc.path("style").path("alignement").asText("left");
                String style = "width:" + largeur + "px;";
                if ("center".equals(alignement)) style += "display:block;margin:0 auto;";
                else if ("right".equals(alignement)) style += "display:block;margin-left:auto;";
                return "<img src='" + escape(url) + "' style='" + style + "' />";
            }
            case "rectangle":
                return renderShape(bloc, false);
            case "cercle":
                return renderShape(bloc, true);
            case "qrcode":
                return renderQrCode(bloc, data);
            case "codebarre":
                return renderBarcode(bloc, data);
            case "signature":
                return renderSignature(bloc, data);
            case "graphique":
                return renderGraphique(bloc, data);
            default:
                return "";
        }
    }

    private String renderShape(JsonNode bloc, boolean isCircle) {
        int largeur = bloc.path("largeurBox").asInt(150);
        int hauteur = bloc.path("hauteurBox").asInt(isCircle ? largeur : 100);
        String fill = bloc.path("style").path("fill").asText("#e5e7eb");
        String couleur = bloc.path("style").path("couleur").asText("#94a3b8");
        int epaisseur = bloc.path("style").path("epaisseur").asInt(1);
        int radius = isCircle ? Math.max(largeur, hauteur) : bloc.path("style").path("borderRadius").asInt(0);

        String style = "width:" + largeur + "px;height:" + hauteur + "px;"
                + "background:" + escape(fill) + ";"
                + "border:" + epaisseur + "px solid " + escape(couleur) + ";"
                + "border-radius:" + (isCircle ? "50%" : radius + "px") + ";"
                + "box-sizing:border-box;";
        return "<div style='" + style + "'></div>";
    }

    private String renderQrCode(JsonNode bloc, Map<String, Object> data) {
        String url = replaceVarsRaw(bloc.path("url").asText(""), data);
        int size = bloc.path("largeurBox").asInt(100);
        if (url.isBlank()) return placeholderBox(size, size, "QR indisponible");
        String dataUri = codeGenerator.generateQrCodeDataUri(url, size);
        if (dataUri == null) return placeholderBox(size, size, "Erreur QR");
        return "<img src='" + dataUri + "' style='width:" + size + "px;height:" + size + "px;' />";
    }

    private String renderBarcode(JsonNode bloc, Map<String, Object> data) {
        String value = replaceVarsRaw(bloc.path("url").asText(""), data);
        int largeur = bloc.path("largeurBox").asInt(160);
        int hauteur = bloc.path("hauteurBox").asInt(60);
        if (value.isBlank()) return placeholderBox(largeur, hauteur, "Code-barres indisponible");
        String dataUri = codeGenerator.generateBarcodeDataUri(value, largeur, hauteur);
        if (dataUri == null) return placeholderBox(largeur, hauteur, "Erreur code-barres");
        return "<img src='" + dataUri + "' style='width:" + largeur + "px;height:" + hauteur + "px;' />";
    }

    private String renderSignature(JsonNode bloc, Map<String, Object> data) {
        int largeur = bloc.path("largeurBox").asInt(180);
        int hauteur = bloc.path("hauteurBox").asInt(70);
        String url = bloc.has("url") ? replaceVarsRaw(bloc.path("url").asText(""), data) : "";
        if (!url.isBlank()) {
            return "<img src='" + escape(url) + "' style='width:" + largeur + "px;height:" + hauteur + "px;object-fit:contain;' />";
        }
        return "<div style='width:" + largeur + "px;height:" + hauteur + "px;border-bottom:1px solid #333;"
                + "display:flex;align-items:flex-end;justify-content:center;padding-bottom:4px;"
                + "font-family:cursive;color:#999;font-size:12px;box-sizing:border-box;'>Signature</div>";
    }

    private String renderGraphique(JsonNode bloc, Map<String, Object> data) {
        String source = stripBraces(bloc.path("source").asText(""));
        Object dataObj = data.get(source);
        int largeur = bloc.path("largeurBox").asInt(300);
        int hauteur = bloc.path("hauteurBox").asInt(180);

        if (!(dataObj instanceof List<?> items) || items.isEmpty()) {
            return placeholderBox(largeur, hauteur, "Graphique (" + source + ")");
        }

        double max = 0;
        for (Object item : items) {
            if (item instanceof Map<?, ?> row) {
                Object val = row.get("value");
                if (val instanceof Number n) max = Math.max(max, n.doubleValue());
            }
        }
        if (max <= 0) max = 1;

        StringBuilder chart = new StringBuilder(
            "<div style='width:" + largeur + "px;height:" + hauteur + "px;"
            + "display:flex;align-items:flex-end;gap:6px;border-left:1px solid #ccc;border-bottom:1px solid #ccc;"
            + "padding:8px;box-sizing:border-box;font-family:Arial,sans-serif;'>"
        );

        int barAreaHeight = hauteur - 40;
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> row)) continue;
            String label = String.valueOf(row.get("label"));
            double value = (row.get("value") instanceof Number n) ? n.doubleValue() : 0;
            int barHeight = (int) Math.max(2, (value / max) * barAreaHeight);
            chart.append("<div style='display:flex;flex-direction:column;align-items:center;flex:1;'>")
                 .append("<div style='width:100%;background:#6d5efc;border-radius:3px 3px 0 0;height:")
                 .append(barHeight).append("px;'></div>")
                 .append("<span style='font-size:9px;color:#555;margin-top:4px;text-align:center;'>")
                 .append(escape(label)).append("</span>")
                 .append("</div>");
        }
        chart.append("</div>");
        return chart.toString();
    }

    private String placeholderBox(int w, int h, String label) {
        return "<div style='width:" + w + "px;height:" + h + "px;border:1px dashed #ccc;"
                + "display:flex;align-items:center;justify-content:center;color:#999;"
                + "font-size:11px;font-family:Arial,sans-serif;box-sizing:border-box;text-align:center;'>"
                + escape(label) + "</div>";
    }

    private String renderStaticTable(JsonNode bloc, Map<String, Object> data) {
        StringBuilder table = new StringBuilder("<table border='1' cellpadding='4'>");
        for (JsonNode row : bloc.path("lignes")) {
            table.append("<tr>");
            for (JsonNode cell : row) {
                table.append("<td>").append(escape(replaceVars(cell.asText(""), data))).append("</td>");
            }
            table.append("</tr>");
        }
        return table.append("</table>").toString();
    }

    private String buildStyle(JsonNode bloc) {
        JsonNode style = bloc.path("style");
        StringBuilder sb = new StringBuilder();
        if (style.has("fontSize")) sb.append("font-size:").append(style.get("fontSize").asInt()).append("px;");
        if (style.has("bold") && style.get("bold").asBoolean()) sb.append("font-weight:bold;");
        if (style.has("italic") && style.get("italic").asBoolean()) sb.append("font-style:italic;");
        if (style.has("underline") && style.get("underline").asBoolean()) sb.append("text-decoration:underline;");
        if (style.has("align")) sb.append("text-align:").append(style.get("align").asText()).append(";");
        if (style.has("color")) sb.append("color:").append(escape(style.get("color").asText())).append(";");
        if (style.has("fontFamily")) sb.append("font-family:").append(escape(style.get("fontFamily").asText())).append(";");
        return sb.toString();
    }

    private String renderTableau(JsonNode bloc, Map<String, Object> data) {
        String source = stripBraces(bloc.path("source").asText(""));
        Object rowsObj = data.get(source);

        StringBuilder table = new StringBuilder(
            "<table style='border-collapse:collapse;width:100%;font-family:Arial, sans-serif;"
            + "font-size:14px;color:#000;font-weight:400;'>"
        );
        String cellStyle = "border:1px solid #d9d9d9;padding:3px 5px;min-width:90px;text-align:left;";
        String headerStyle = cellStyle + "background:#f5f5f5;font-weight:600;";

        table.append("<tr>");
        for (JsonNode col : bloc.path("colonnes")) {
            table.append("<th style='").append(headerStyle).append("'>")
                 .append(escape(col.path("titre").asText(""))).append("</th>");
        }
        table.append("</tr>");

        if (rowsObj instanceof List<?> rows) {
            for (Object rowObj : rows) {
                if (!(rowObj instanceof Map<?, ?> row)) continue;
                table.append("<tr>");
                for (JsonNode col : bloc.path("colonnes")) {
                    Object cellValue = row.get(col.path("variable").asText(""));
                    table.append("<td style='").append(cellStyle).append("'>")
                         .append(escape(String.valueOf(cellValue))).append("</td>");
                }
                table.append("</tr>");
            }
        }
        return table.append("</table>").toString();
    }

    private String replaceVars(String content, Map<String, Object> data) {
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = data.get(varName);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(escape(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /** Comme replaceVars, mais sans échapper le résultat (utile pour QR/code-barres/URL brutes). */
    private String replaceVarsRaw(String content, Map<String, Object> data) {
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = data.get(varName);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String stripBraces(String source) {
        return source.replace("{{", "").replace("}}", "").trim();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}