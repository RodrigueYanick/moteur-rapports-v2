package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapports.moteur.entity.PaginationMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateHtmlBuilder {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    /**
     * Conversion px (96 DPI) → mm.
     * Le front-end stocke les positions en px à 96 DPI.
     * Pour le rendu HTML/CSS on utilise des mm afin que Flying Saucer
     * (72 DPI en interne) produise un PDF identique à l'aperçu navigateur.
     */
    private static final double MM_PER_PX = 25.4 / 96.0;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodeGeneratorService codeGenerator;

    public TemplateHtmlBuilder(CodeGeneratorService codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    // ------------------------------------------------------------------
    // Utilitaires de conversion
    // ------------------------------------------------------------------

    /** Convertit des pixels (96 DPI) en millimètres, arrondi à 2 décimales. */
    private static double pxToMm(int px) {
        return Math.round(px * MM_PER_PX * 100.0) / 100.0;
    }

    /** Formate un double pour le CSS (2 décimales, pas de virgule locale). */
    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    // ------------------------------------------------------------------
    // Dimensions standard des formats papier (largeur mm, hauteur mm)
    // ------------------------------------------------------------------

    private int[] getStandardDimensions(String format) {
        switch (format) {
            case "A0": return new int[]{841, 1189};
            case "A1": return new int[]{594, 841};
            case "A2": return new int[]{420, 594};
            case "A3": return new int[]{297, 420};
            case "A4": return new int[]{210, 297};
            case "A5": return new int[]{148, 210};
            case "A6": return new int[]{105, 148};
            case "A7": return new int[]{74, 105};
            case "A8": return new int[]{52, 74};
            case "A9": return new int[]{37, 52};
            case "A10": return new int[]{26, 37};
            case "Letter": return new int[]{216, 279};
            case "Legal": return new int[]{216, 356};
            default: return new int[]{210, 297};
        }
    }

    public String build(String contenuDesignJson, Map<String, Object> data,
                    String formatPapier, Integer largeurMm, Integer hauteurMm,
                    PaginationMode modePagination,
                    Integer margeGaucheMm, Integer margeDroiteMm,
                    Integer margeHautMm, Integer margeBasMm) {

        int widthMm, heightMm;
        if ("CUSTOM".equalsIgnoreCase(formatPapier)) {
            widthMm = largeurMm != null ? largeurMm : 210;
            heightMm = hauteurMm != null ? hauteurMm : 297;
        } else {
            int[] dims = getStandardDimensions(formatPapier);
            widthMm = dims[0];
            heightMm = dims[1];
        }

        // Marges (par défaut 10mm)
        int mLeft   = (margeGaucheMm != null && margeGaucheMm > 0) ? margeGaucheMm : 10;
        int mRight  = (margeDroiteMm != null && margeDroiteMm > 0) ? margeDroiteMm : 10;
        int mTop    = (margeHautMm != null && margeHautMm > 0) ? margeHautMm : 10;
        int mBottom = (margeBasMm != null && margeBasMm > 0) ? margeBasMm : 10;

        // Dimensions de la zone de contenu (mm)
        int contentWidthMm  = widthMm  - mLeft - mRight;
        int contentHeightMm = heightMm - mTop  - mBottom;

        // Dimensions en px (96 DPI) — uniquement pour les calculs internes de pagination
        int widthPx  = (int) Math.round(widthMm  * 96.0 / 25.4);
        int heightPx = (int) Math.round(heightMm * 96.0 / 25.4);

        // CSS @page : taille physique de la page, marges CSS à 0
        // (les marges sont garanties par le positionnement absolu millimétrique de chaque bloc)
        String pageSizeCss = "size: " + widthMm + "mm " + heightMm + "mm;";

        StringBuilder html = new StringBuilder(
            "<html><head><meta charset='UTF-8'/><style>"
            + "@page{" + pageSizeCss + "margin:0;}"
            + " html,body{margin:0;padding:0;}"
            + "</style></head><body>"
        );

        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            PageContext ctx = new PageContext(data, widthMm, heightMm,
                                             mLeft, mRight, mTop, mBottom,
                                             contentWidthMm, contentHeightMm,
                                             widthPx, heightPx);

            if (modePagination == PaginationMode.AUTO) {
                ArrayNode autoBlocs = objectMapper.createArrayNode();
                JsonNode blocs = autoBlocs;
                if (root.has("pages") && root.path("pages").isArray() && root.path("pages").size() > 0) {
                    for (int pageIndex = 0; pageIndex < root.path("pages").size(); pageIndex++) {
                        JsonNode pageBlocks = root.path("pages").get(pageIndex).path("blocs");
                        if (!pageBlocks.isArray()) continue;
                        for (JsonNode bloc : pageBlocks) {
                            ObjectNode positioned = bloc.deepCopy();
                            positioned.put("y", bloc.path("y").asInt(0) + pageIndex * heightPx);
                            autoBlocs.add(positioned);
                        }
                    }
                } else if (root.has("blocs")) {
                    blocs = root.path("blocs");
                }
                if (blocs.isArray()) {
                    html.append(renderAutoPages(blocs, ctx));
                }
            } else {
                // Mode FIXED : chaque page de conception est traitée de façon autonome et séquentielle,
                // exactement comme dans l'aperçu du designer. Si un tableau déborde, il génère des
                // pages de continuation avec respect absolu de margeHautMm avant de passer à la page conçue suivante.
                List<List<JsonNode>> allRenderedPages = new ArrayList<>();
                if (root.has("pages") && root.path("pages").isArray() && root.path("pages").size() > 0) {
                    for (JsonNode pageNode : root.path("pages")) {
                        JsonNode pageBlocs = pageNode.path("blocs");
                        allRenderedPages.addAll(paginateSinglePage(pageBlocs, ctx));
                    }
                } else if (root.has("blocs")) {
                    allRenderedPages.addAll(paginateSinglePage(root.path("blocs"), ctx));
                }

                if (allRenderedPages.isEmpty()) {
                    allRenderedPages.add(new ArrayList<>());
                }

                for (int i = 0; i < allRenderedPages.size(); i++) {
                    html.append(renderPage(allRenderedPages.get(i), ctx, i == allRenderedPages.size() - 1));
                }
            }
        } catch (Exception e) {
            html.append("<p>Erreur de design : ").append(e.getMessage()).append("</p>");
        }
        return html.append("</body></html>").toString();
    }

    // ============================================================
    // CONTEXTE DE PAGE
    // ============================================================

    private static class PageContext {
        final Map<String, Object> data;
        final int widthMm, heightMm;
        final int mLeftMm, mRightMm, mTopMm, mBottomMm;
        final int contentWidthMm, contentHeightMm;
        final int widthPx, heightPx;

        PageContext(Map<String, Object> data,
                    int widthMm, int heightMm,
                    int mLeftMm, int mRightMm, int mTopMm, int mBottomMm,
                    int contentWidthMm, int contentHeightMm,
                    int widthPx, int heightPx) {
            this.data = data;
            this.widthMm = widthMm;
            this.heightMm = heightMm;
            this.mLeftMm = mLeftMm;
            this.mRightMm = mRightMm;
            this.mTopMm = mTopMm;
            this.mBottomMm = mBottomMm;
            this.contentWidthMm = contentWidthMm;
            this.contentHeightMm = contentHeightMm;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }

        /** Hauteur de la zone de contenu en px (96 DPI), utile pour les calculs de fragmentation. */
        int contentHeightPx() {
            return (int) Math.round(contentHeightMm * 96.0 / 25.4);
        }
    }

    // ============================================================
    // PAGINATION AUTOMATIQUE
    // ============================================================

    /**
     * Découpe une liste de blocs en plusieurs pages automatiquement.
     * Les blocs sont triés par position Y, puis répartis page par page.
     * Les tableaux (statiques et dynamiques) peuvent être fragmentés en plusieurs morceaux.
     * La zone de contenu (hauteur de page moins marges verticales) détermine la limite.
     */
    private String renderAutoPages(JsonNode blocs, PageContext ctx) {
        List<JsonNode> sortedBlocks = new ArrayList<>();
        if (blocs.isArray()) {
            blocs.forEach(bloc -> {
                if (!bloc.has("visible") || bloc.path("visible").asBoolean(true)) {
                    sortedBlocks.add(bloc);
                }
            });
        }
        sortedBlocks.sort(Comparator.comparingInt(b -> b.path("y").asInt(0)));

        // Fragmentation des tableaux
        int contentHeightPx = ctx.contentHeightPx();
        int pageHeightPx = ctx.heightPx;
        int mTopPx    = (int) Math.round(ctx.mTopMm    * 96.0 / 25.4);
        int mBottomPx = (int) Math.round(ctx.mBottomMm * 96.0 / 25.4);
        int contentBottomPx = pageHeightPx - mBottomPx;

        List<JsonNode> allFragments = new ArrayList<>();
        for (JsonNode bloc : sortedBlocks) {
            if (isTableau(bloc)) {
                // Calculer l'espace restant sur la première page à partir de la position Y du tableau
                int baseY = Math.max(0, bloc.path("y").asInt(0));
                int firstPageAvailable = Math.max(0, contentBottomPx - Math.max(baseY, mTopPx));
                allFragments.addAll(splitTableIntoFragments(bloc, ctx.data, contentHeightPx, firstPageAvailable, pageHeightPx, mTopPx));
            } else {
                allFragments.add(bloc);
            }
        }

        if (allFragments.isEmpty()) return "";

        List<List<JsonNode>> pages = new ArrayList<>();
        for (JsonNode fragment : allFragments) {
            int virtualY = Math.max(0, fragment.path("y").asInt(0));
            int fragmentHeight = Math.max(1, getEstimatedBlocHeight(fragment, ctx.data, contentHeightPx));
            int pageIndex = virtualY / pageHeightPx;
            int localY = virtualY % pageHeightPx;

            // Si le bloc est dans la marge haute, le ramener au début de la zone de contenu
            if (localY < mTopPx) {
                localY = mTopPx;
            }

            // Les fragments de tableau sont déjà dimensionnés pour tenir dans la page ;
            // on ne les pousse PAS sur la page suivante.
            // Seuls les blocs non-tableau indivisibles sont déplacés.
            boolean isTableFragment = fragment.path("_tableFragment").asBoolean(false);
            if (!isTableFragment && localY > mTopPx && localY + fragmentHeight > contentBottomPx) {
                pageIndex++;
                localY = mTopPx;
            }

            while (pages.size() <= pageIndex) pages.add(new ArrayList<>());
            pages.get(pageIndex).add(adjustBlockY(fragment, localY));
        }

        StringBuilder html = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            html.append(renderPage(pages.get(i), ctx, i == pages.size() - 1));
        }
        return html.toString();
    }

    // ============================================================
    // PAGINATION INTELLIGENTE (MODE FIXED)
    // ============================================================

    private List<List<JsonNode>> paginateSinglePage(JsonNode blocs, PageContext ctx) {
        List<JsonNode> sortedBlocks = new ArrayList<>();
        if (blocs != null && blocs.isArray()) {
            blocs.forEach(bloc -> {
                if (!bloc.has("visible") || bloc.path("visible").asBoolean(true)) {
                    sortedBlocks.add(bloc);
                }
            });
        }
        sortedBlocks.sort(Comparator.comparingInt(b -> b.path("y").asInt(0)));

        int contentHeightPx = ctx.contentHeightPx();
        int pageHeightPx = ctx.heightPx;
        int mTopPx    = (int) Math.round(ctx.mTopMm    * 96.0 / 25.4);
        int mBottomPx = (int) Math.round(ctx.mBottomMm * 96.0 / 25.4);
        int contentBottomPx = pageHeightPx - mBottomPx;

        if (sortedBlocks.isEmpty()) {
            List<List<JsonNode>> empty = new ArrayList<>();
            empty.add(new ArrayList<>());
            return empty;
        }

        List<PageFragmentEntry> entries = new ArrayList<>();
        for (JsonNode bloc : sortedBlocks) {
            if (isTableau(bloc)) {
                int baseY = Math.max(mTopPx, bloc.path("y").asInt(0));
                int firstPageAvailable = Math.max(0, contentBottomPx - baseY);
                List<JsonNode> tableFragments = splitTableIntoFragments(bloc, ctx.data, contentHeightPx, firstPageAvailable, pageHeightPx, mTopPx);
                for (int i = 0; i < tableFragments.size(); i++) {
                    JsonNode frag = tableFragments.get(i);
                    int localY = (i == 0) ? baseY : mTopPx;
                    entries.add(new PageFragmentEntry(adjustBlockY(frag, localY), i));
                }
            } else {
                int y = Math.max(mTopPx, bloc.path("y").asInt(0));
                int height = Math.max(1, getEstimatedBlocHeight(bloc, ctx.data, contentHeightPx));
                if (y > mTopPx && y + height > contentBottomPx) {
                    entries.add(new PageFragmentEntry(adjustBlockY(bloc, mTopPx), 1));
                } else {
                    entries.add(new PageFragmentEntry(adjustBlockY(bloc, y), 0));
                }
            }
        }

        int maxOffset = entries.stream().mapToInt(e -> e.pageOffset).max().orElse(0);
        List<List<JsonNode>> pages = new ArrayList<>();
        for (int p = 0; p <= maxOffset; p++) {
            pages.add(new ArrayList<>());
        }
        for (PageFragmentEntry e : entries) {
            pages.get(e.pageOffset).add(e.block);
        }
        return pages;
    }

    private static class PageFragmentEntry {
        final JsonNode block;
        final int pageOffset;
        PageFragmentEntry(JsonNode block, int pageOffset) {
            this.block = block;
            this.pageOffset = pageOffset;
        }
    }

    private String renderSmartPages(JsonNode blocs, PageContext ctx) {
        return renderAutoPages(blocs, ctx);
    }

    private boolean isTableau(JsonNode bloc) {
        return "tableau".equals(bloc.path("type").asText());
    }

    /**
     * Estime la hauteur d'un bloc en pixels.
     * Pour les tableaux dynamiques, on se base sur le nombre de lignes de données.
     * Pour les autres, on utilise hauteurBox ou une valeur par défaut.
     */
    private int getEstimatedBlocHeight(JsonNode bloc, Map<String, Object> data, int pageHeightPx) {
        String type = bloc.path("type").asText();
        if ("tableau".equals(type)) {
            // Hauteur réelle d'une ligne CSS : font-size:14px ≈ 17px + padding 3+3px + border 1px ≈ 24px
            // On utilise 25px pour inclure une marge de sécurité d'1px
            int rowHeight = 25;
            int headerHeight = 25;
            if (bloc.has("lignes")) {
                int rows = bloc.path("lignes").size();
                return rows * rowHeight + headerHeight;
            } else {
                String source = stripBraces(bloc.path("source").asText(""));
                Object rowsObj = data.get(source);
                if (rowsObj instanceof List<?> rows) {
                    return rows.size() * rowHeight + headerHeight;
                }
                return 150;
            }
        }
        return bloc.path("hauteurBox").asInt(150);
    }
    /**
     * Fractionne un tableau (statique ou dynamique) en plusieurs blocs de tableau,
     * chacun tenant dans la hauteur disponible de la page courante.
     *
     * @param bloc              le bloc tableau d'origine
     * @param data              les données de remplacement
     * @param contentHeightPx   hauteur de la zone de contenu d'une page complète (px)
     * @param firstPageAvailable espace restant sur la première page à partir de la position Y du tableau (px)
     * @param pageHeightPx      hauteur totale d'une page en px (pour calculer les positions Y absolues)
     * @param mTopPx            marge haute en px (début de la zone de contenu sur chaque page)
     */
    private List<JsonNode> splitTableIntoFragments(JsonNode bloc, Map<String, Object> data,
                                                    int contentHeightPx, int firstPageAvailable,
                                                    int pageHeightPx, int mTopPx) {
        List<JsonNode> fragments = new ArrayList<>();
        // Hauteur réelle d'une ligne CSS : font-size:14px ≈ 17px + padding 3+3px + border 1px ≈ 24px
        // On utilise 25px pour inclure une marge de sécurité d'1px
        int rowHeight = 25;
        int headerHeight = 25;
        int baseY = Math.max(0, bloc.path("y").asInt(0));

        // Si l'espace sur la première page est insuffisant pour l'en-tête + 1 ligne,
        // on commence le tableau sur la page suivante
        int minRequired = headerHeight + rowHeight;
        boolean startOnNextPage = firstPageAvailable < minRequired;

        int effectiveFirstAvailable = startOnNextPage ? contentHeightPx : firstPageAvailable;
        int effectiveBaseY = startOnNextPage
            ? (baseY / pageHeightPx + 1) * pageHeightPx + mTopPx
            : baseY;

        // Première page (effective) : nombre de lignes tenant dans l'espace restant
        int firstPageRows = Math.max(1, (effectiveFirstAvailable - headerHeight) / rowHeight);
        // Pages suivantes : nombre de lignes tenant dans la hauteur complète de contenu
        int nextPageRows = Math.max(1, (contentHeightPx - headerHeight) / rowHeight);

        if (bloc.has("lignes")) {
            ArrayNode lignes = (ArrayNode) bloc.get("lignes");
            int totalRows = lignes.size();

            if (totalRows == 0) {
                ObjectNode fragment = bloc.deepCopy();
                fragment.put("y", effectiveBaseY);
                fragment.put("_tableFragment", true);
                fragments.add(fragment);
            } else {
                // La première ligne est l'en-tête du tableau
                JsonNode headerRow = lignes.get(0);
                int currentIndex = 0;
                boolean isFirstFragment = true;
                int currentY = effectiveBaseY;

                while (currentIndex < totalRows) {
                    int maxRows = isFirstFragment ? firstPageRows : nextPageRows;
                    int end = Math.min(currentIndex + maxRows, totalRows);

                    ObjectNode fragment = bloc.deepCopy();
                    fragment.put("y", currentY);
                    fragment.put("_tableFragment", true);
                    fragment.put("_isFirstFragment", isFirstFragment);
                    ArrayNode subArray = fragment.putArray("lignes");

                    if (isFirstFragment) {
                        // Premier fragment : on inclut les lignes telles quelles (header inclus)
                        for (int i = currentIndex; i < end; i++) subArray.add(lignes.get(i));
                    } else {
                        // Fragments suivants : on répète l'en-tête en première ligne
                        subArray.add(headerRow);
                        for (int i = currentIndex; i < end; i++) subArray.add(lignes.get(i));
                    }
                    fragments.add(fragment);

                    currentIndex = end;
                    isFirstFragment = false;

                    // Calculer la position Y du fragment suivant :
                    // on passe au début de la zone de contenu de la page suivante
                    int currentPageIndex = currentY / pageHeightPx;
                    currentY = (currentPageIndex + 1) * pageHeightPx + mTopPx;
                }
            }
        } else {
            String source = stripBraces(bloc.path("source").asText(""));
            Object rowsObj = data.get(source);

            if (rowsObj instanceof List<?> rows) {
                int totalRows = rows.size();

                if (totalRows == 0) {
                    ObjectNode fragment = bloc.deepCopy();
                    fragment.put("y", effectiveBaseY);
                    fragment.put("_tableFragment", true);
                    fragments.add(fragment);
                } else {
                    int currentIndex = 0;
                    boolean isFirstFragment = true;
                    int currentY = effectiveBaseY;

                    while (currentIndex < totalRows) {
                        int maxRows = isFirstFragment ? firstPageRows : nextPageRows;
                        int end = Math.min(currentIndex + maxRows, totalRows);

                        ObjectNode fragment = bloc.deepCopy();
                        fragment.put("y", currentY);
                        fragment.put("_tableFragment", true);
                        fragment.put("_data_start", currentIndex);
                        fragment.put("_data_end", end);
                        fragments.add(fragment);

                        currentIndex = end;
                        isFirstFragment = false;

                        // Calculer la position Y du fragment suivant :
                        // on passe au début de la zone de contenu de la page suivante
                        int currentPageIndex = currentY / pageHeightPx;
                        currentY = (currentPageIndex + 1) * pageHeightPx + mTopPx;
                    }
                }
            } else {
                ObjectNode fragment = bloc.deepCopy();
                fragment.put("y", effectiveBaseY);
                fragment.put("_tableFragment", true);
                fragments.add(fragment);
            }
        }
        return fragments;
    }

    /**
     * Ajuste la coordonnée Y d'un bloc (en pixels) pour qu'il soit positionné
     * dans la page courante (0 en haut).
     */
    private JsonNode adjustBlockY(JsonNode bloc, int newY) {
        ObjectNode adjusted = bloc.deepCopy();
        adjusted.put("y", newY);
        return adjusted;
    }

    // ============================================================
    // RENDU D'UNE PAGE
    // ============================================================

    private static final Map<String, int[]> DEFAULT_DIMENSIONS = Map.ofEntries(
        Map.entry("titre", new int[]{400, 40}),
        Map.entry("texte", new int[]{400, 40}),
        Map.entry("tableau", new int[]{750, 150}),
        Map.entry("ligne", new int[]{780, 10}),
        Map.entry("image", new int[]{150, 150}),
        Map.entry("rectangle", new int[]{150, 100}),
        Map.entry("cercle", new int[]{100, 100}),
        Map.entry("qrcode", new int[]{100, 100}),
        Map.entry("codebarre", new int[]{160, 60}),
        Map.entry("signature", new int[]{180, 70}),
        Map.entry("graphique", new int[]{300, 180})
    );

    /**
     * Rend une page HTML avec des dimensions en mm.
     * Le conteneur fait la taille physique complète de la page avec un padding
     * correspondant aux marges. Les blocs position:absolute sont relatifs au
     * padding edge du conteneur, ce qui garantit le respect des marges sur
     * TOUTES les pages (y compris continuation) — compatible Flying Saucer.
     * Les positions de blocs sont converties de px (96 DPI) vers mm
     * puis décalées pour être relatives au padding edge (zone de contenu).
     */
    private String renderPage(List<JsonNode> blocs, PageContext ctx, boolean isLastPage) {
        String breakStyle = isLastPage ? "page-break-after:auto;" : "page-break-after:always;";
        StringBuilder page = new StringBuilder(
            "<div class='page' style='position:relative;width:"
            + ctx.widthMm + "mm;height:" + ctx.heightMm
            + "mm;background:white;overflow:hidden;box-sizing:border-box;margin:0;padding:0;"
            + breakStyle + "'>"
        );

        for (JsonNode bloc : blocs) {
            if (bloc.has("visible") && !bloc.path("visible").asBoolean(true)) {
                continue;
            }
            int xPx = bloc.path("x").asInt(0);
            int yPx = bloc.path("y").asInt(0);
            String type = bloc.path("type").asText();

            int[] fallback = DEFAULT_DIMENSIONS.getOrDefault(type, new int[]{200, 50});
            int widthPx  = bloc.path("largeurBox").asInt(fallback[0]);
            int heightPx = bloc.path("hauteurBox").asInt(fallback[1]);

            // Conversion directe px (96 DPI) → mm : identique à l'aperçu et fidèle sur toutes les pages
            double xMm = pxToMm(xPx);
            double yMm = pxToMm(yPx);
            double wMm = pxToMm(widthPx);
            double hMm = pxToMm(heightPx);

            int rotation = bloc.path("rotation").asInt(0);
            double opacite = bloc.path("opacite").asDouble(100);
            StringBuilder transformParts = new StringBuilder();
            if (rotation != 0) {
                transformParts.append("transform:rotate(").append(rotation).append("deg);transform-origin:center center;");
            }
            if (opacite != 100) {
                transformParts.append("opacity:").append(opacite / 100).append(";");
            }

            boolean isDynamicTable = "tableau".equals(type) && !bloc.has("lignes");
            boolean isTableFragment = "tableau".equals(type) && bloc.path("_tableFragment").asBoolean(false);
            page.append("<div style='position:absolute;left:").append(fmt(xMm))
                .append("mm;top:").append(fmt(yMm))
                .append("mm;width:").append(fmt(wMm)).append("mm;");

            if (isDynamicTable || isTableFragment) {
                page.append("height:auto;overflow:visible;");
            } else {
                page.append("height:").append(fmt(hMm)).append("mm;overflow:hidden;");
            }

            page.append("box-sizing:border-box;")
                .append(transformParts)
                .append("'>");

            page.append(renderBloc(bloc, ctx.data));
            page.append("</div>");
        }
        return page.append("</div>").toString();
    }

    // Surcharge pour compatibilité : renderPage(JsonNode, ...) redirige vers List
    private String renderPage(JsonNode blocs, PageContext ctx) {
        List<JsonNode> list = new ArrayList<>();
        if (blocs != null && blocs.isArray()) {
            blocs.forEach(list::add);
        }
        return renderPage(list, ctx, false);
    }

    private String renderBloc(JsonNode bloc, Map<String, Object> data) {
        String type = bloc.path("type").asText();
        switch (type) {
            case "titre":
                return "<h1 style='" + buildStyle(bloc) + "'>" + replaceVars(bloc.path("contenu").asText(""), data) + "</h1>";
            case "texte":
                return "<p style='" + buildStyle(bloc) + "'>" + replaceVars(bloc.path("contenu").asText(""), data) + "</p>";
            case "tableau":
                if (bloc.has("lignes")) {
                    return renderStaticTable(bloc, data);
                } else {
                    return renderDynamicTable(bloc, data);
                }
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

    // ============================================================
    // TABLEAUX (RENDU FINAL)
    // ============================================================

    private String renderStaticTable(JsonNode bloc, Map<String, Object> data) {
        String bordure = bloc.path("style").path("bordureCouleur").asText("#d9d9d9");
        String texteDefaut = bloc.path("style").path("texteCouleurDefaut").asText("#000000");
        boolean isTableFragment = bloc.path("_tableFragment").asBoolean(false);

        StringBuilder table = new StringBuilder(
            "<table style='border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;'>"
        );
        int rowIndex = 0;
        for (JsonNode row : bloc.path("lignes")) {
            // Dans un tableau fragmenté, la première ligne est l'en-tête (fond gris, gras)
            boolean isHeaderRow = isTableFragment && rowIndex == 0;
            table.append("<tr>");
            for (JsonNode cell : row) {
                if (cell.path("hidden").asBoolean(false)) continue;
                String value = cell.path("value").asText("");
                String bgColor = cell.has("bgColor") && !cell.path("bgColor").isNull() ? cell.path("bgColor").asText() : null;
                String textColor = cell.has("textColor") && !cell.path("textColor").isNull() ? cell.path("textColor").asText() : texteDefaut;
                int colSpan = cell.path("colSpan").asInt(1);
                int rowSpan = cell.path("rowSpan").asInt(1);

                String style = "border:1px solid " + escape(bordure) + ";padding:3px 5px;min-width:90px;color:" + escape(textColor) + ";";
                if (isHeaderRow) {
                    style += "background:#f5f5f5;font-weight:600;";
                } else if (bgColor != null) {
                    style += "background:" + escape(bgColor) + ";";
                }

                String tag = isHeaderRow ? "th" : "td";
                table.append("<").append(tag);
                if (colSpan > 1) table.append(" colspan='").append(colSpan).append("'");
                if (rowSpan > 1) table.append(" rowspan='").append(rowSpan).append("'");
                table.append(" style='").append(style).append("'>")
                    .append(escape(replaceVars(value, data))).append("</").append(tag).append(">");
            }
            table.append("</tr>");
            rowIndex++;
        }
        return table.append("</table>").toString();
    }

    private String renderDynamicTable(JsonNode bloc, Map<String, Object> data) {
        String source = stripBraces(bloc.path("source").asText(""));
        Object rowsObj = data.get(source);
        String bordure = bloc.path("style").path("bordureCouleur").asText("#d9d9d9");
        String texteDefaut = bloc.path("style").path("texteCouleurDefaut").asText("#000000");

        StringBuilder table = new StringBuilder(
            "<table style='border-collapse:collapse;width:100%;font-family:Arial, sans-serif;"
            + "font-size:14px;color:" + escape(texteDefaut) + ";font-weight:400;'>"
        );
        String cellStyle = "border:1px solid " + escape(bordure) + ";padding:3px 5px;min-width:90px;text-align:left;";
        String headerStyle = cellStyle + "background:#f5f5f5;font-weight:600;";

        table.append("<tr>");
        for (JsonNode col : bloc.path("colonnes")) {
            table.append("<th style='").append(headerStyle).append("'>")
                .append(escape(col.path("titre").asText(""))).append("</th>");
        }
        table.append("</tr>");

        // Gestion des fragments : si bloc contient _data_start/_data_end, on ne rend qu'une partie
        if (rowsObj instanceof List<?> rows) {
            int start = bloc.has("_data_start") ? bloc.path("_data_start").asInt(0) : 0;
            int end = bloc.has("_data_end") ? bloc.path("_data_end").asInt(rows.size()) : rows.size();
            for (int i = start; i < end && i < rows.size(); i++) {
                Object rowObj = rows.get(i);
                if (!(rowObj instanceof Map<?, ?> row)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> rowMap = (Map<String, Object>) row;
                table.append("<tr>");
                for (JsonNode col : bloc.path("colonnes")) {
                    String varName = col.path("variable").asText();
                    Object cellValue = rowMap.get(varName);
                    String cellValueStr = cellValue != null ? String.valueOf(cellValue) : "";
                    table.append("<td style='").append(cellStyle).append("'>")
                        .append(escape(cellValueStr)).append("</td>");
                }
                table.append("</tr>");
            }
        }
        return table.append("</table>").toString();
    }

    // ============================================================
    // AUTRES MÉTHODES EXISTANTES (renderShape, renderQrCode, etc.)
    // ============================================================

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

    private String buildStyle(JsonNode bloc) {
        JsonNode style = bloc.path("style");
        StringBuilder sb = new StringBuilder("margin:0;box-sizing:border-box;width:100%;");
        if (style.has("fontSize")) sb.append("font-size:").append(style.get("fontSize").asInt()).append("px;");
        if (style.has("bold") && style.get("bold").asBoolean()) sb.append("font-weight:bold;");
        if (style.has("italic") && style.get("italic").asBoolean()) sb.append("font-style:italic;");
        if (style.has("underline") && style.get("underline").asBoolean()) sb.append("text-decoration:underline;");
        if (style.has("align")) sb.append("text-align:").append(style.get("align").asText()).append(";");
        if (style.has("color")) sb.append("color:").append(escape(style.get("color").asText())).append(";");
        if (style.has("fontFamily")) sb.append("font-family:").append(escape(style.get("fontFamily").asText())).append(";");
        return sb.toString();
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