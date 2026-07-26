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

    public String build(String contenuDesignJson, Map<String, Object> data) {
        StringBuilder html = new StringBuilder("<html><head><meta charset='UTF-8'/></head><body>");
        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            if (root.has("blocs")) {
                for (JsonNode bloc : root.path("blocs")) {
                    html.append(renderBloc(bloc, data));
                }
            } else {
                // Fallback : afficher toutes les données reçues
                html.append("<h1>Rapport généré</h1>");
                for (Map.Entry<String, Object> e : data.entrySet()) {
                    html.append("<p><strong>").append(e.getKey()).append("</strong> : ")
                        .append(e.getValue()).append("</p>");
                }
            }
        } catch (Exception e) {
            html.append("<p>Erreur de design : ").append(e.getMessage()).append("</p>");
        }
        return html.append("</body></html>").toString();
    }

    private String renderBloc(JsonNode bloc, Map<String, Object> data) {
        return switch (bloc.path("type").asText()) {
            case "titre" -> "<h1>" + replaceVars(bloc.path("contenu").asText(""), data) + "</h1>";
            case "texte" -> "<p>" + replaceVars(bloc.path("contenu").asText(""), data) + "</p>";
            case "tableau" -> renderTableau(bloc, data);
            case "ligne" -> {
            int epaisseur = bloc.path("style").path("epaisseur").asInt(1);
            String couleur = bloc.path("style").path("couleur").asText("#000000");
            int largeur = bloc.path("style").path("largeur").asInt(100);
            yield "<hr style='border-top:" + epaisseur + "px solid " + escape(couleur) + "; width:" + largeur + "%;' />";
        }
        case "image" -> {
            String url = bloc.path("url").asText("");
            url = replaceVars(url, data);
            int largeur = bloc.path("style").path("largeur").asInt(100);
            String alignement = bloc.path("style").path("alignement").asText("left");
            String style = "width:" + largeur + "px;";
            if ("center".equals(alignement)) style += "display:block;margin:0 auto;";
            else if ("right".equals(alignement)) style += "display:block;margin-left:auto;";
            yield "<img src='" + escape(url) + "' style='" + style + "' />";
        }
            default -> "";
        };
    }

    private String renderTableau(JsonNode bloc, Map<String, Object> data) {
        String source = stripBraces(bloc.path("source").asText(""));
        Object rowsObj = data.get(source);
        StringBuilder table = new StringBuilder("<table border='1' cellpadding='4'><tr>");

        for (JsonNode col : bloc.path("colonnes")) {
            table.append("<th>").append(escape(col.path("titre").asText(""))).append("</th>");
        }
        table.append("</tr>");

        if (rowsObj instanceof List<?> rows) {
            for (Object rowObj : rows) {
                if (!(rowObj instanceof Map<?, ?> row)) continue;
                table.append("<tr>");
                for (JsonNode col : bloc.path("colonnes")) {
                    Object cellValue = row.get(col.path("variable").asText(""));
                    String cellString = (cellValue != null) ? cellValue.toString() : "";
                    table.append("<td>").append(escape(cellString)).append("</td>");
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

    private String stripBraces(String source) {
        return source.replace("{{", "").replace("}}", "").trim();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}