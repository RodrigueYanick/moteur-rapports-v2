package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.service.expression.ExpressionEvaluator;
import com.rapports.moteur.service.expression.SpelSecureExpressionEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ExpressionEvaluator expressionEvaluator;

    @Autowired
    public TemplateHtmlBuilder(CodeGeneratorService codeGenerator, ExpressionEvaluator expressionEvaluator) {
        this.codeGenerator = codeGenerator;
        this.expressionEvaluator = expressionEvaluator;
    }

    public TemplateHtmlBuilder(CodeGeneratorService codeGenerator) {
        this(codeGenerator, new SpelSecureExpressionEvaluator());
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

    public String build(com.rapports.moteur.entity.ReportTemplate template, Map<String, Object> data) {
        return build(
            template.getContenuDesign(),
            data,
            template.getFormatPapier(),
            template.getLargeurMm(),
            template.getHauteurMm(),
            template.getModePagination(),
            template.getMargeGaucheMm(),
            template.getMargeDroiteMm(),
            template.getMargeHautMm(),
            template.getMargeBasMm(),
            template.getCouleurFond(),
            template.getHeaderActif(),
            template.getHauteurHeaderMm(),
            template.getHeaderContenu(),
            template.getHeaderAlignement(),
            template.getHeaderAfficherSurPremierePage(),
            template.getHeaderLigneSeparation(),
            template.getHeaderCouleurLigne(),
            template.getFooterActif(),
            template.getHauteurFooterMm(),
            template.getFooterContenu(),
            template.getFooterAlignement(),
            template.getFooterAfficherSurPremierePage(),
            template.getFooterLigneSeparation(),
            template.getFooterCouleurLigne(),
            template.getNumerotationPage(),
            template.getFormatNumerotation()
        );
    }

    public String build(String contenuDesignJson, Map<String, Object> data,
                    String formatPapier, Integer largeurMm, Integer hauteurMm,
                    PaginationMode modePagination,
                    Integer margeGaucheMm, Integer margeDroiteMm,
                    Integer margeHautMm, Integer margeBasMm) {
        return build(contenuDesignJson, data, formatPapier, largeurMm, hauteurMm,
                     modePagination, margeGaucheMm, margeDroiteMm, margeHautMm, margeBasMm,
                     "#FFFFFF", false, 15, null, "GAUCHE", true, true, "#CCCCCC",
                     false, 12, null, "CENTRE", true, true, "#CCCCCC", true, "PAGE_X_SUR_Y");
    }

    public String build(String contenuDesignJson, Map<String, Object> data,
                    String formatPapier, Integer largeurMm, Integer hauteurMm,
                    PaginationMode modePagination,
                    Integer margeGaucheMm, Integer margeDroiteMm,
                    Integer margeHautMm, Integer margeBasMm,
                    String couleurFond,
                    Boolean headerActif, Integer hauteurHeaderMm, String headerContenu,
                    String headerAlignement, Boolean headerAfficherSurPremierePage,
                    Boolean headerLigneSeparation, String headerCouleurLigne,
                    Boolean footerActif, Integer hauteurFooterMm, String footerContenu,
                    String footerAlignement, Boolean footerAfficherSurPremierePage,
                    Boolean footerLigneSeparation, String footerCouleurLigne,
                    Boolean numerotationPage, String formatNumerotation) {

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

        // Dimensions en px (96 DPI) — uniquement pour les calculs internes de pagination
        int widthPx  = (int) Math.round(widthMm  * 96.0 / 25.4);
        int heightPx = (int) Math.round(heightMm * 96.0 / 25.4);

        // CSS @page : taille physique de la page, marges CSS à 0
        // (les marges sont garanties par le positionnement absolu millimétrique de chaque bloc)
        String pageSizeCss = "size: " + widthMm + "mm " + heightMm + "mm;";

        StringBuilder html = new StringBuilder(
            "<html><head><meta charset='UTF-8'/><style>"
            + "@page{" + pageSizeCss + "margin:0;}"
            + "@media print{body{-webkit-print-color-adjust:exact;print-color-adjust:exact;}}"
            + "*,*:before,*:after{box-sizing:border-box;}"
            + "html,body{margin:0;padding:0;width:100%;font-family:Arial,Helvetica,sans-serif;-webkit-print-color-adjust:exact;print-color-adjust:exact;}"
            + "table{border-collapse:collapse;}"
            + "thead{display:table-header-group;}"
            + "tfoot{display:table-footer-group;}"
            + "tr{break-inside:avoid;page-break-inside:avoid;}"
            + "</style></head><body>"
        );

        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            PageContext ctx = new PageContext(data, widthMm, heightMm,
                                             mLeft, mRight, mTop, mBottom,
                                             widthPx, heightPx,
                                             couleurFond,
                                             headerActif, hauteurHeaderMm, headerContenu,
                                             headerAlignement, headerAfficherSurPremierePage,
                                             headerLigneSeparation, headerCouleurLigne,
                                             footerActif, hauteurFooterMm, footerContenu,
                                             footerAlignement, footerAfficherSurPremierePage,
                                             footerLigneSeparation, footerCouleurLigne,
                                             numerotationPage, formatNumerotation);

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
                    html.append(renderPage(allRenderedPages.get(i), ctx, i == allRenderedPages.size() - 1, i + 1, allRenderedPages.size()));
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
        final int effectiveTopMm, effectiveBottomMm;
        final int contentWidthMm, contentHeightMm;
        final int widthPx, heightPx;
        final String couleurFond;

        final Boolean headerActif;
        final int hauteurHeaderMm;
        final String headerContenu;
        final String headerAlignement;
        final Boolean headerAfficherSurPremierePage;
        final Boolean headerLigneSeparation;
        final String headerCouleurLigne;

        final Boolean footerActif;
        final int hauteurFooterMm;
        final String footerContenu;
        final String footerAlignement;
        final Boolean footerAfficherSurPremierePage;
        final Boolean footerLigneSeparation;
        final String footerCouleurLigne;

        final Boolean numerotationPage;
        final String formatNumerotation;

        PageContext(Map<String, Object> data,
                    int widthMm, int heightMm,
                    int mLeftMm, int mRightMm, int mTopMm, int mBottomMm,
                    int widthPx, int heightPx,
                    String couleurFond,
                    Boolean headerActif, Integer hauteurHeaderMm, String headerContenu,
                    String headerAlignement, Boolean headerAfficherSurPremierePage,
                    Boolean headerLigneSeparation, String headerCouleurLigne,
                    Boolean footerActif, Integer hauteurFooterMm, String footerContenu,
                    String footerAlignement, Boolean footerAfficherSurPremierePage,
                    Boolean footerLigneSeparation, String footerCouleurLigne,
                    Boolean numerotationPage, String formatNumerotation) {
            this.data = data;
            this.widthMm = widthMm;
            this.heightMm = heightMm;
            this.mLeftMm = mLeftMm;
            this.mRightMm = mRightMm;
            this.mTopMm = mTopMm;
            this.mBottomMm = mBottomMm;
            this.widthPx = widthPx;
            this.heightPx = heightPx;

            this.couleurFond = (couleurFond != null && !couleurFond.isBlank()) ? couleurFond : "#FFFFFF";

            this.headerActif = headerActif;
            int hH = (Boolean.TRUE.equals(headerActif) && hauteurHeaderMm != null && hauteurHeaderMm > 0)
                    ? hauteurHeaderMm : (Boolean.TRUE.equals(headerActif) ? 15 : 0);
            this.hauteurHeaderMm = hH > 0 ? hH : (hauteurHeaderMm != null && hauteurHeaderMm > 0 ? hauteurHeaderMm : 15);
            this.headerContenu = headerContenu;
            this.headerAlignement = headerAlignement != null ? headerAlignement : "GAUCHE";
            this.headerAfficherSurPremierePage = headerAfficherSurPremierePage == null || headerAfficherSurPremierePage;
            this.headerLigneSeparation = headerLigneSeparation == null || headerLigneSeparation;
            this.headerCouleurLigne = (headerCouleurLigne != null && !headerCouleurLigne.isBlank()) ? headerCouleurLigne : "#CCCCCC";

            this.footerActif = footerActif;
            int fH = (Boolean.TRUE.equals(footerActif) && hauteurFooterMm != null && hauteurFooterMm > 0)
                    ? hauteurFooterMm : (Boolean.TRUE.equals(footerActif) ? 12 : 0);
            this.hauteurFooterMm = fH > 0 ? fH : (hauteurFooterMm != null && hauteurFooterMm > 0 ? hauteurFooterMm : 12);
            this.footerContenu = footerContenu;
            this.footerAlignement = footerAlignement != null ? footerAlignement : "CENTRE";
            this.footerAfficherSurPremierePage = footerAfficherSurPremierePage == null || footerAfficherSurPremierePage;
            this.footerLigneSeparation = footerLigneSeparation == null || footerLigneSeparation;
            this.footerCouleurLigne = (footerCouleurLigne != null && !footerCouleurLigne.isBlank()) ? footerCouleurLigne : "#CCCCCC";

            this.numerotationPage = numerotationPage == null || numerotationPage;
            this.formatNumerotation = formatNumerotation != null ? formatNumerotation : "PAGE_X_SUR_Y";

            // Zone utile (mm)
            this.effectiveTopMm = mTopMm + hH;
            this.effectiveBottomMm = mBottomMm + fH;
            this.contentWidthMm = widthMm - mLeftMm - mRightMm;
            this.contentHeightMm = heightMm - this.effectiveTopMm - this.effectiveBottomMm;
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
                if ((!bloc.has("visible") || bloc.path("visible").asBoolean(true))
                        && shouldDisplayBlock(bloc, ctx.data)) {
                    sortedBlocks.add(bloc);
                }
            });
        }
        sortedBlocks.sort(Comparator.comparingInt(b -> b.path("y").asInt(0)));

        // Fragmentation des tableaux
        int contentHeightPx = ctx.contentHeightPx();
        int pageHeightPx = ctx.heightPx;
        int effectiveTopPx = (int) Math.round(ctx.effectiveTopMm * 96.0 / 25.4);
        int effectiveBottomPx = (int) Math.round(ctx.effectiveBottomMm * 96.0 / 25.4);
        int contentBottomPx = pageHeightPx - effectiveBottomPx;

        List<JsonNode> allFragments = new ArrayList<>();
        for (JsonNode bloc : sortedBlocks) {
            if (isTableau(bloc)) {
                // Calculer l'espace restant sur la première page à partir de la position Y du tableau
                int baseY = Math.max(0, bloc.path("y").asInt(0));
                int firstPageAvailable = Math.max(0, contentBottomPx - Math.max(baseY, effectiveTopPx));
                allFragments.addAll(splitTableIntoFragments(bloc, ctx.data, contentHeightPx, firstPageAvailable, pageHeightPx, effectiveTopPx));
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

            // Si le bloc est dans la marge haute / zone en-tête, le ramener au début de la zone utile
            if (localY < effectiveTopPx) {
                localY = effectiveTopPx;
            }

            // Les fragments de tableau sont déjà dimensionnés pour tenir dans la page ;
            // on ne les pousse PAS sur la page suivante.
            // Seuls les blocs non-tableau indivisibles sont déplacés.
            boolean isTableFragment = fragment.path("_tableFragment").asBoolean(false);
            if (!isTableFragment && localY > effectiveTopPx && localY + fragmentHeight > contentBottomPx) {
                pageIndex++;
                localY = effectiveTopPx;
            }

            while (pages.size() <= pageIndex) pages.add(new ArrayList<>());
            pages.get(pageIndex).add(adjustBlockY(fragment, localY));
        }

        StringBuilder html = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            html.append(renderPage(pages.get(i), ctx, i == pages.size() - 1, i + 1, pages.size()));
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
                if ((!bloc.has("visible") || bloc.path("visible").asBoolean(true))
                        && shouldDisplayBlock(bloc, ctx.data)) {
                    sortedBlocks.add(bloc);
                }
            });
        }
        sortedBlocks.sort(Comparator.comparingInt(b -> b.path("y").asInt(0)));

        int contentHeightPx = ctx.contentHeightPx();
        int pageHeightPx = ctx.heightPx;
        int effectiveTopPx = (int) Math.round(ctx.effectiveTopMm * 96.0 / 25.4);
        int effectiveBottomPx = (int) Math.round(ctx.effectiveBottomMm * 96.0 / 25.4);
        int contentBottomPx = pageHeightPx - effectiveBottomPx;

        if (sortedBlocks.isEmpty()) {
            List<List<JsonNode>> empty = new ArrayList<>();
            empty.add(new ArrayList<>());
            return empty;
        }

        List<PageFragmentEntry> entries = new ArrayList<>();
        for (JsonNode bloc : sortedBlocks) {
            if (isTableau(bloc)) {
                int baseY = Math.max(effectiveTopPx, bloc.path("y").asInt(0));
                int firstPageAvailable = Math.max(0, contentBottomPx - baseY);
                List<JsonNode> tableFragments = splitTableIntoFragments(bloc, ctx.data, contentHeightPx, firstPageAvailable, pageHeightPx, effectiveTopPx);
                for (int i = 0; i < tableFragments.size(); i++) {
                    JsonNode frag = tableFragments.get(i);
                    int localY = (i == 0) ? baseY : effectiveTopPx;
                    entries.add(new PageFragmentEntry(adjustBlockY(frag, localY), i));
                }
            } else {
                int y = Math.max(effectiveTopPx, bloc.path("y").asInt(0));
                int height = Math.max(1, getEstimatedBlocHeight(bloc, ctx.data, contentHeightPx));
                if (y > effectiveTopPx && y + height > contentBottomPx) {
                    entries.add(new PageFragmentEntry(adjustBlockY(bloc, effectiveTopPx), 1));
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
    private String renderPage(List<JsonNode> blocs, PageContext ctx, boolean isLastPage, int pageNum, int totalPages) {
        String breakStyle = isLastPage ? "page-break-after:auto;" : "page-break-after:always;";
        StringBuilder page = new StringBuilder(
            "<div class='page' style='position:relative;width:"
            + ctx.widthMm + "mm;height:" + ctx.heightMm
            + "mm;background:" + escape(ctx.couleurFond) + ";overflow:hidden;box-sizing:border-box;margin:0;padding:0;"
            + breakStyle + "'>"
        );

        // En-tête
        boolean showHeader = Boolean.TRUE.equals(ctx.headerActif) &&
            (pageNum > 1 || !Boolean.FALSE.equals(ctx.headerAfficherSurPremierePage));
        if (showHeader) {
            String headerText = ctx.headerContenu != null ? ctx.headerContenu : "";
            headerText = replacePageTokens(headerText, pageNum, totalPages, ctx.data);
            String align = getCssTextAlign(ctx.headerAlignement);
            String borderBottom = Boolean.TRUE.equals(ctx.headerLigneSeparation)
                ? "border-bottom:1px solid " + escape(ctx.headerCouleurLigne != null ? ctx.headerCouleurLigne : "#CCCCCC") + ";"
                : "";

            page.append("<div class='page-header' style='position:absolute;left:")
                .append(ctx.mLeftMm).append("mm;top:")
                .append(ctx.mTopMm).append("mm;width:")
                .append(ctx.contentWidthMm).append("mm;height:")
                .append(ctx.hauteurHeaderMm).append("mm;overflow:hidden;box-sizing:border-box;")
                .append(borderBottom)
                .append("'>");

            page.append("<table style='width:100%;height:100%;border-collapse:collapse;border:none;margin:0;padding:0;'>")
                .append("<tr><td style='vertical-align:middle;text-align:").append(align)
                .append(";font-family:Arial,sans-serif;font-size:9pt;color:#555555;padding:0 4px;'>")
                .append(headerText)
                .append("</td></tr></table>");

            page.append("</div>");
        }

        // Pied de page
        boolean showFooter = Boolean.TRUE.equals(ctx.footerActif) &&
            (pageNum > 1 || !Boolean.FALSE.equals(ctx.footerAfficherSurPremierePage));
        if (showFooter) {
            String footerText = (ctx.footerContenu != null && !ctx.footerContenu.isBlank())
                ? ctx.footerContenu
                : (Boolean.TRUE.equals(ctx.numerotationPage)
                    ? ("PAGE_X".equalsIgnoreCase(ctx.formatNumerotation) ? "Page {page}" : "Page {page} / {pages}")
                    : "");

            footerText = replacePageTokens(footerText, pageNum, totalPages, ctx.data);
            String align = getCssTextAlign(ctx.footerAlignement);
            String borderTop = Boolean.TRUE.equals(ctx.footerLigneSeparation)
                ? "border-top:1px solid " + escape(ctx.footerCouleurLigne != null ? ctx.footerCouleurLigne : "#CCCCCC") + ";"
                : "";

            double footerTopMm = ctx.heightMm - ctx.mBottomMm - ctx.hauteurFooterMm;

            page.append("<div class='page-footer' style='position:absolute;left:")
                .append(ctx.mLeftMm).append("mm;top:")
                .append(fmt(footerTopMm)).append("mm;width:")
                .append(ctx.contentWidthMm).append("mm;height:")
                .append(ctx.hauteurFooterMm).append("mm;overflow:hidden;box-sizing:border-box;")
                .append(borderTop)
                .append("'>");

            page.append("<table style='width:100%;height:100%;border-collapse:collapse;border:none;margin:0;padding:0;'>")
                .append("<tr><td style='vertical-align:middle;text-align:").append(align)
                .append(";font-family:Arial,sans-serif;font-size:9pt;color:#555555;padding:0 4px;'>")
                .append(footerText)
                .append("</td></tr></table>");

            page.append("</div>");
        }

        for (JsonNode bloc : blocs) {
            if (bloc.has("visible") && !bloc.path("visible").asBoolean(true)) {
                continue;
            }
            if (!shouldDisplayBlock(bloc, ctx.data)) {
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

    // Surcharges pour compatibilité :
    private String renderPage(List<JsonNode> blocs, PageContext ctx, boolean isLastPage) {
        return renderPage(blocs, ctx, isLastPage, 1, 1);
    }

    private String renderPage(JsonNode blocs, PageContext ctx) {
        List<JsonNode> list = new ArrayList<>();
        if (blocs != null && blocs.isArray()) {
            blocs.forEach(list::add);
        }
        return renderPage(list, ctx, false, 1, 1);
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

        table.append("<thead><tr>");
        for (JsonNode col : bloc.path("colonnes")) {
            table.append("<th style='").append(headerStyle).append("'>")
                .append(escape(col.path("titre").asText(""))).append("</th>");
        }
        table.append("</tr></thead>");

        table.append("<tbody>");
        String groupBy = bloc.path("groupBy").asText("").trim();
        String groupHeaderTemplate = bloc.path("groupHeaderTemplate").asText("").trim();
        boolean afficherSousTotaux = bloc.path("afficherSousTotaux").asBoolean(true);
        int numCols = bloc.has("colonnes") && bloc.path("colonnes").isArray() ? Math.max(1, bloc.path("colonnes").size()) : 1;

        boolean hasAnyAggregate = false;
        if (bloc.has("colonnes") && bloc.path("colonnes").isArray()) {
            for (JsonNode col : bloc.path("colonnes")) {
                String ag = col.path("agregat").asText("NONE");
                if (!ag.equalsIgnoreCase("NONE") && !ag.isBlank()) {
                    hasAnyAggregate = true;
                    break;
                }
            }
        }

        // Gestion des fragments : si bloc contient _data_start/_data_end, on ne rend qu'une partie
        if (rowsObj instanceof List<?> rows) {
            int start = bloc.has("_data_start") ? bloc.path("_data_start").asInt(0) : 0;
            int end = bloc.has("_data_end") ? bloc.path("_data_end").asInt(rows.size()) : rows.size();

            List<Map<String, Object>> fragmentRows = new ArrayList<>();
            for (int i = start; i < end && i < rows.size(); i++) {
                Object rowObj = rows.get(i);
                if (rowObj instanceof Map<?, ?> row) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> rowMap = (Map<String, Object>) row;
                    fragmentRows.add(rowMap);
                }
            }

            if (!groupBy.isEmpty()) {
                // Regroupement ordonné par la clé groupBy
                Map<String, List<Map<String, Object>>> groups = new java.util.LinkedHashMap<>();
                for (Map<String, Object> rowMap : fragmentRows) {
                    Object gVal = rowMap.get(groupBy);
                    if (gVal == null) {
                        try {
                            gVal = expressionEvaluator.evaluate(groupBy, rowMap);
                        } catch (Exception ignored) {}
                    }
                    String groupKey = (gVal != null && !gVal.toString().isBlank()) ? gVal.toString() : "Sans " + groupBy;
                    groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(rowMap);
                }

                for (Map.Entry<String, List<Map<String, Object>>> groupEntry : groups.entrySet()) {
                    String groupKey = groupEntry.getKey();
                    List<Map<String, Object>> groupRows = groupEntry.getValue();

                    // 1. En-tête de groupe (Group Header)
                    String groupTitle;
                    if (!groupHeaderTemplate.isEmpty()) {
                        String replaced = groupHeaderTemplate.replace("{{groupKey}}", groupKey).replace("{{ groupKey }}", groupKey).replace("{{key}}", groupKey);
                        groupTitle = replaceVars(replaced, Map.of(groupBy, groupKey, "groupKey", groupKey));
                    } else {
                        groupTitle = groupKey;
                    }

                    table.append("<tr class='group-header'>")
                         .append("<td colspan='").append(numCols).append("' style='")
                         .append(cellStyle).append("background:#f1f5f9;font-weight:bold;font-size:13px;color:#1e293b;padding:6px 8px;'>")
                         .append(escape(groupTitle)).append("</td></tr>");

                    // 2. Lignes de données du groupe
                    for (Map<String, Object> rowMap : groupRows) {
                        table.append("<tr>");
                        for (JsonNode col : bloc.path("colonnes")) {
                            String varName = col.path("variable").asText();
                            String formule = col.path("formule").asText("");
                            Object cellValue;
                            if (!formule.isBlank()) {
                                cellValue = expressionEvaluator.evaluate(formule, rowMap);
                            } else {
                                cellValue = rowMap.get(varName);
                            }
                            String cellValueStr = cellValue != null ? String.valueOf(cellValue) : "";
                            table.append("<td style='").append(cellStyle).append("'>")
                                .append(escape(cellValueStr)).append("</td>");
                        }
                        table.append("</tr>");
                    }

                    // 3. Pied de groupe (Sous-totaux / Group Footer)
                    if (afficherSousTotaux && hasAnyAggregate) {
                        table.append("<tr class='group-footer' style='background:#f8fafc;'>");
                        boolean firstCol = true;
                        for (JsonNode col : bloc.path("colonnes")) {
                            String varName = col.path("variable").asText();
                            String formule = col.path("formule").asText("");
                            String agregat = col.path("agregat").asText("NONE").toUpperCase(java.util.Locale.ROOT);

                            if ("SUM".equals(agregat) || "AVG".equals(agregat) || "MIN".equals(agregat) || "MAX".equals(agregat) || "COUNT".equals(agregat)) {
                                double sum = 0;
                                double min = Double.MAX_VALUE;
                                double max = -Double.MAX_VALUE;
                                int count = 0;

                                for (Map<String, Object> map : groupRows) {
                                    Object val = !formule.isBlank() ? expressionEvaluator.evaluate(formule, map) : map.get(varName);
                                    if (val != null) {
                                        count++;
                                        try {
                                            double d = Double.parseDouble(val.toString().replace(",", ".").replace(" ", ""));
                                            sum += d;
                                            if (d < min) min = d;
                                            if (d > max) max = d;
                                        } catch (Exception ignored) {}
                                    }
                                }

                                String agResult;
                                if ("SUM".equals(agregat)) {
                                    agResult = String.format(java.util.Locale.US, "%.2f", sum);
                                } else if ("AVG".equals(agregat)) {
                                    agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", sum / count) : "0.00";
                                } else if ("COUNT".equals(agregat)) {
                                    agResult = String.valueOf(count);
                                } else if ("MIN".equals(agregat)) {
                                    agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", min) : "0.00";
                                } else {
                                    agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", max) : "0.00";
                                }

                                table.append("<td style='").append(cellStyle).append("font-weight:600;background:#f8fafc;'>")
                                        .append(escape(agResult)).append("</td>");
                            } else {
                                String label = firstCol ? "Sous-total (" + groupKey + ")" : "";
                                table.append("<td style='").append(cellStyle).append("font-weight:600;background:#f8fafc;'>")
                                        .append(escape(label)).append("</td>");
                            }
                            firstCol = false;
                        }
                        table.append("</tr>");
                    }
                }
            } else {
                // Rendu standard à plat (sans regroupement)
                for (Map<String, Object> rowMap : fragmentRows) {
                    table.append("<tr>");
                    for (JsonNode col : bloc.path("colonnes")) {
                        String varName = col.path("variable").asText();
                        String formule = col.path("formule").asText("");
                        Object cellValue;
                        if (!formule.isBlank()) {
                            cellValue = expressionEvaluator.evaluate(formule, rowMap);
                        } else {
                            cellValue = rowMap.get(varName);
                        }
                        String cellValueStr = cellValue != null ? String.valueOf(cellValue) : "";
                        table.append("<td style='").append(cellStyle).append("'>")
                            .append(escape(cellValueStr)).append("</td>");
                    }
                    table.append("</tr>");
                }
            }
        }
        table.append("</tbody>");

        // On n'affiche le tfoot que sur le dernier fragment ou si non-fragmenté
        boolean isLastTableFragment = !bloc.has("_data_end")
                || (rowsObj instanceof List<?> rList && bloc.path("_data_end").asInt(0) >= rList.size());

        if (hasAnyAggregate && isLastTableFragment && rowsObj instanceof List<?> allRows) {
            table.append("<tfoot><tr style='font-weight:bold;background:#f9fafb;'>");
            boolean firstCol = true;
            for (JsonNode col : bloc.path("colonnes")) {
                String varName = col.path("variable").asText();
                String formule = col.path("formule").asText("");
                String agregat = col.path("agregat").asText("NONE").toUpperCase(java.util.Locale.ROOT);

                if ("SUM".equals(agregat) || "AVG".equals(agregat) || "MIN".equals(agregat) || "MAX".equals(agregat) || "COUNT".equals(agregat)) {
                    double sum = 0;
                    double min = Double.MAX_VALUE;
                    double max = -Double.MAX_VALUE;
                    int count = 0;

                    for (Object row : allRows) {
                        if (!(row instanceof Map<?, ?> rMap)) continue;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) rMap;
                        Object val;
                        if (!formule.isBlank()) {
                            val = expressionEvaluator.evaluate(formule, map);
                        } else {
                            val = map.get(varName);
                        }
                        if (val != null) {
                            count++;
                            try {
                                double d = Double.parseDouble(val.toString().replace(",", ".").replace(" ", ""));
                                sum += d;
                                if (d < min) min = d;
                                if (d > max) max = d;
                            } catch (Exception ignored) {}
                        }
                    }

                    String agResult;
                    if ("SUM".equals(agregat)) {
                        agResult = String.format(java.util.Locale.US, "%.2f", sum);
                    } else if ("AVG".equals(agregat)) {
                        agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", sum / count) : "0.00";
                    } else if ("COUNT".equals(agregat)) {
                        agResult = String.valueOf(count);
                    } else if ("MIN".equals(agregat)) {
                        agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", min) : "0.00";
                    } else {
                        agResult = count > 0 ? String.format(java.util.Locale.US, "%.2f", max) : "0.00";
                    }

                    table.append("<td style='").append(cellStyle).append("font-weight:bold;'>")
                            .append(escape(agResult)).append("</td>");
                } else {
                    String label = firstCol ? "Total Général" : "";
                    table.append("<td style='").append(cellStyle).append("font-weight:bold;'>")
                            .append(escape(label)).append("</td>");
                }
                firstCol = false;
            }
            table.append("</tr></tfoot>");
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
        return "<table style='width:" + largeur + "px;height:" + hauteur + "px;border-bottom:1px solid #333;"
                + "border-collapse:collapse;margin:0;padding:0;box-sizing:border-box;'>"
                + "<tr><td style='vertical-align:bottom;text-align:center;padding-bottom:4px;"
                + "font-family:cursive;color:#999;font-size:12px;'>Signature</td></tr></table>";
    }

    private String renderGraphique(JsonNode bloc, Map<String, Object> data) {
        String source = stripBraces(bloc.path("source").asText(""));
        Object dataObj = data.get(source);
        int largeur = bloc.path("largeurBox").asInt(300);
        int hauteur = bloc.path("hauteurBox").asInt(180);

        if (!(dataObj instanceof List<?> items) || items.isEmpty()) {
            return placeholderBox(largeur, hauteur, "Graphique (" + source + ")");
        }

        String type = bloc.path("graphiqueType").asText("").trim().toLowerCase(java.util.Locale.ROOT);
        if (type.isEmpty()) {
            type = bloc.path("chartType").asText("").trim().toLowerCase(java.util.Locale.ROOT);
        }

        // Si explicitement configuré en vectoriel SVG (bar, line, pie, donut)
        if ("bar".equals(type) || "line".equals(type) || "pie".equals(type) || "donut".equals(type)) {
            return renderSvgChart(bloc, items, largeur, hauteur, type);
        }

        // Rendu de secours rétrocompatible en table CSS 2.1
        return renderTableBarChart(bloc, items, largeur, hauteur);
    }

    private static class ChartPoint {
        final String label;
        final double value;
        ChartPoint(String label, double value) {
            this.label = label != null ? label : "";
            this.value = value;
        }
    }

    private List<ChartPoint> extractChartPoints(JsonNode bloc, List<?> items) {
        String labelKey = bloc.path("graphiqueLabelKey").asText("").trim();
        String valueKey = bloc.path("graphiqueValueKey").asText("").trim();
        List<ChartPoint> points = new ArrayList<>();

        for (Object item : items) {
            if (!(item instanceof Map<?, ?> row)) continue;
            String label = null;
            if (!labelKey.isEmpty() && row.get(labelKey) != null) {
                label = String.valueOf(row.get(labelKey));
            } else if (row.get("label") != null) {
                label = String.valueOf(row.get("label"));
            } else if (row.get("nom") != null) {
                label = String.valueOf(row.get("nom"));
            } else if (row.get("name") != null) {
                label = String.valueOf(row.get("name"));
            } else {
                label = row.keySet().stream().filter(k -> !(row.get(k) instanceof Number))
                           .findFirst().map(k -> String.valueOf(row.get(k))).orElse("—");
            }

            double val = 0;
            if (!valueKey.isEmpty() && row.get(valueKey) instanceof Number n) {
                val = n.doubleValue();
            } else if (row.get("value") instanceof Number n) {
                val = n.doubleValue();
            } else if (row.get("valeur") instanceof Number n) {
                val = n.doubleValue();
            } else if (row.get("total") instanceof Number n) {
                val = n.doubleValue();
            } else if (row.get("montant") instanceof Number n) {
                val = n.doubleValue();
            } else {
                for (Object v : row.values()) {
                    if (v instanceof Number n) {
                        val = n.doubleValue();
                        break;
                    }
                }
            }
            points.add(new ChartPoint(label, val));
        }
        return points;
    }

    private String renderSvgChart(JsonNode bloc, List<?> items, int largeur, int hauteur, String type) {
        List<ChartPoint> points = extractChartPoints(bloc, items);
        if (points.isEmpty()) {
            return placeholderBox(largeur, hauteur, "Aucune donnée");
        }

        String[] palette = {"#6366f1", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#06b6d4", "#ec4899", "#3b82f6"};
        String primaryColor = "#6366f1";
        if (bloc.has("style")) {
            JsonNode style = bloc.get("style");
            if (style.has("fill") && !style.get("fill").asText("").isBlank()) primaryColor = style.get("fill").asText();
            else if (style.has("couleur") && !style.get("couleur").asText("").isBlank()) primaryColor = style.get("couleur").asText();
            else if (style.has("color") && !style.get("color").asText("").isBlank()) primaryColor = style.get("color").asText();
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<div style='width:").append(largeur).append("px;height:").append(hauteur)
           .append("px;box-sizing:border-box;overflow:hidden;'>");
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ").append(largeur).append(" ").append(hauteur)
           .append("' width='100%' height='100%' style='font-family:Arial,sans-serif;'>");

        if ("pie".equals(type) || "donut".equals(type)) {
            double total = points.stream().mapToDouble(p -> p.value).sum();
            if (total <= 0) total = 1;
            boolean isDonut = "donut".equals(type);
            int legendWidth = Math.min(120, largeur / 3);
            int chartAreaW = largeur - legendWidth;
            int R = Math.max(10, Math.min(chartAreaW, hauteur) / 2 - 14);
            int cx = chartAreaW / 2;
            int cy = hauteur / 2;
            double innerR = isDonut ? R * 0.55 : 0;

            double currentAngle = -Math.PI / 2.0;
            for (int i = 0; i < points.size(); i++) {
                ChartPoint pt = points.get(i);
                double sliceAngle = (pt.value / total) * 2.0 * Math.PI;
                double nextAngle = currentAngle + sliceAngle;
                String color = palette[i % palette.length];

                double x1 = cx + R * Math.cos(currentAngle);
                double y1 = cy + R * Math.sin(currentAngle);
                double x2 = cx + R * Math.cos(nextAngle);
                double y2 = cy + R * Math.sin(nextAngle);
                int largeArc = sliceAngle > Math.PI ? 1 : 0;

                if (isDonut) {
                    double xi1 = cx + innerR * Math.cos(currentAngle);
                    double yi1 = cy + innerR * Math.sin(currentAngle);
                    double xi2 = cx + innerR * Math.cos(nextAngle);
                    double yi2 = cy + innerR * Math.sin(nextAngle);
                    svg.append("<path d='M ").append(fmt(x1)).append(" ").append(fmt(y1))
                       .append(" A ").append(R).append(" ").append(R).append(" 0 ").append(largeArc).append(" 1 ").append(fmt(x2)).append(" ").append(fmt(y2))
                       .append(" L ").append(fmt(xi2)).append(" ").append(fmt(yi2))
                       .append(" A ").append(fmt(innerR)).append(" ").append(fmt(innerR)).append(" 0 ").append(largeArc).append(" 0 ").append(fmt(xi1)).append(" ").append(fmt(yi1))
                       .append(" Z' fill='").append(color).append("' stroke='#ffffff' stroke-width='1.5' />");
                } else {
                    svg.append("<path d='M ").append(cx).append(" ").append(cy)
                       .append(" L ").append(fmt(x1)).append(" ").append(fmt(y1))
                       .append(" A ").append(R).append(" ").append(R).append(" 0 ").append(largeArc).append(" 1 ").append(fmt(x2)).append(" ").append(fmt(y2))
                       .append(" Z' fill='").append(color).append("' stroke='#ffffff' stroke-width='1.5' />");
                }
                currentAngle = nextAngle;
            }

            // Légende latérale
            int legX = chartAreaW + 4;
            int legItemH = Math.min(22, (hauteur - 10) / Math.max(1, points.size()));
            for (int i = 0; i < points.size() && i < 8; i++) {
                ChartPoint pt = points.get(i);
                int ly = 16 + i * legItemH;
                String color = palette[i % palette.length];
                double pct = (pt.value / total) * 100.0;
                svg.append("<rect x='").append(legX).append("' y='").append(ly - 8).append("' width='10' height='10' rx='2' fill='").append(color).append("' />");
                svg.append("<text x='").append(legX + 14).append("' y='").append(ly).append("' font-size='9' fill='#334155'>")
                   .append(escape(pt.label)).append(" (").append((int)Math.round(pct)).append("%)</text>");
            }
        } else if ("line".equals(type)) {
            double maxVal = points.stream().mapToDouble(p -> p.value).max().orElse(1);
            if (maxVal <= 0) maxVal = 1;
            int mLeft = 35;
            int mBottom = 25;
            int mTop = 20;
            int mRight = 15;
            int plotW = Math.max(10, largeur - mLeft - mRight);
            int plotH = Math.max(10, hauteur - mTop - mBottom);

            // Grille horizontale
            for (int i = 0; i <= 3; i++) {
                int y = mTop + plotH - (i * plotH / 3);
                double tickVal = (maxVal * i) / 3.0;
                String tickStr = tickVal == (long) tickVal ? String.valueOf((long) tickVal) : String.format(java.util.Locale.US, "%.1f", tickVal);
                svg.append("<line x1='").append(mLeft).append("' y1='").append(y).append("' x2='").append(mLeft + plotW)
                   .append("' y2='").append(y).append("' stroke='#e2e8f0' stroke-dasharray='3,3' />");
                svg.append("<text x='").append(mLeft - 4).append("' y='").append(y + 3)
                   .append("' text-anchor='end' font-size='9' fill='#64748b'>").append(tickStr).append("</text>");
            }
            svg.append("<line x1='").append(mLeft).append("' y1='").append(mTop + plotH)
               .append("' x2='").append(mLeft + plotW).append("' y2='").append(mTop + plotH).append("' stroke='#cbd5e1' />");

            int n = points.size();
            StringBuilder pointsStr = new StringBuilder();
            StringBuilder areaPoints = new StringBuilder();
            areaPoints.append(mLeft).append(",").append(mTop + plotH).append(" ");
            for (int i = 0; i < n; i++) {
                ChartPoint pt = points.get(i);
                int x = mLeft + (n > 1 ? (i * plotW / (n - 1)) : plotW / 2);
                int y = (int) (mTop + plotH - (pt.value / maxVal) * plotH);
                pointsStr.append(x).append(",").append(y).append(" ");
                areaPoints.append(x).append(",").append(y).append(" ");
            }
            areaPoints.append(mLeft + plotW).append(",").append(mTop + plotH);

            svg.append("<polygon points='").append(areaPoints).append("' fill='").append(primaryColor).append("' fill-opacity='0.12' />");
            svg.append("<polyline points='").append(pointsStr).append("' fill='none' stroke='").append(primaryColor).append("' stroke-width='2.5' stroke-linecap='round' stroke-linejoin='round' />");

            for (int i = 0; i < n; i++) {
                ChartPoint pt = points.get(i);
                int x = mLeft + (n > 1 ? (i * plotW / (n - 1)) : plotW / 2);
                int y = (int) (mTop + plotH - (pt.value / maxVal) * plotH);
                svg.append("<circle cx='").append(x).append("' cy='").append(y).append("' r='3.5' fill='#ffffff' stroke='").append(primaryColor).append("' stroke-width='2' />");
                String vStr = pt.value == (long) pt.value ? String.valueOf((long) pt.value) : String.format(java.util.Locale.US, "%.1f", pt.value);
                svg.append("<text x='").append(x).append("' y='").append(y - 5).append("' text-anchor='middle' font-size='8' fill='#334155'>").append(vStr).append("</text>");
                svg.append("<text x='").append(x).append("' y='").append(hauteur - 6).append("' text-anchor='middle' font-size='9' fill='#64748b'>").append(escape(pt.label)).append("</text>");
            }
        } else {
            // Type "bar" (SVG)
            double maxVal = points.stream().mapToDouble(p -> p.value).max().orElse(1);
            if (maxVal <= 0) maxVal = 1;
            int mLeft = 35;
            int mBottom = 25;
            int mTop = 20;
            int mRight = 15;
            int plotW = Math.max(10, largeur - mLeft - mRight);
            int plotH = Math.max(10, hauteur - mTop - mBottom);

            // Grille horizontale
            for (int i = 0; i <= 3; i++) {
                int y = mTop + plotH - (i * plotH / 3);
                double tickVal = (maxVal * i) / 3.0;
                String tickStr = tickVal == (long) tickVal ? String.valueOf((long) tickVal) : String.format(java.util.Locale.US, "%.1f", tickVal);
                svg.append("<line x1='").append(mLeft).append("' y1='").append(y).append("' x2='").append(mLeft + plotW)
                   .append("' y2='").append(y).append("' stroke='#e2e8f0' stroke-dasharray='3,3' />");
                svg.append("<text x='").append(mLeft - 4).append("' y='").append(y + 3)
                   .append("' text-anchor='end' font-size='9' fill='#64748b'>").append(tickStr).append("</text>");
            }
            svg.append("<line x1='").append(mLeft).append("' y1='").append(mTop + plotH)
               .append("' x2='").append(mLeft + plotW).append("' y2='").append(mTop + plotH).append("' stroke='#cbd5e1' />");

            int n = points.size();
            int barSlot = n > 0 ? plotW / n : plotW;
            int barW = Math.max(6, (int) (barSlot * 0.65));
            for (int i = 0; i < n; i++) {
                ChartPoint pt = points.get(i);
                int barH = (int) Math.max(2, (pt.value / maxVal) * plotH);
                int x = mLeft + i * barSlot + (barSlot - barW) / 2;
                int y = mTop + plotH - barH;
                String col = primaryColor.equals("#6366f1") ? palette[i % palette.length] : primaryColor;
                svg.append("<rect x='").append(x).append("' y='").append(y).append("' width='").append(barW)
                   .append("' height='").append(barH).append("' fill='").append(escape(col)).append("' rx='3' />");
                String vStr = pt.value == (long) pt.value ? String.valueOf((long) pt.value) : String.format(java.util.Locale.US, "%.1f", pt.value);
                svg.append("<text x='").append(x + barW / 2).append("' y='").append(y - 4)
                   .append("' text-anchor='middle' font-size='8' fill='#475569'>").append(vStr).append("</text>");
                svg.append("<text x='").append(x + barW / 2).append("' y='").append(hauteur - 6)
                   .append("' text-anchor='middle' font-size='9' fill='#64748b'>").append(escape(pt.label)).append("</text>");
            }
        }

        svg.append("</svg></div>");
        return svg.toString();
    }

    private String renderTableBarChart(JsonNode bloc, List<?> items, int largeur, int hauteur) {
        double max = 0;
        for (Object item : items) {
            if (item instanceof Map<?, ?> row) {
                Object val = row.get("value");
                if (val instanceof Number n) max = Math.max(max, n.doubleValue());
            }
        }
        if (max <= 0) max = 1;

        String barColor = "#6d5efc";
        if (bloc.has("style")) {
            JsonNode style = bloc.get("style");
            if (style.has("fill") && !style.get("fill").asText("").isBlank()) {
                barColor = style.get("fill").asText();
            } else if (style.has("couleur") && !style.get("couleur").asText("").isBlank()) {
                barColor = style.get("couleur").asText();
            } else if (style.has("color") && !style.get("color").asText("").isBlank()) {
                barColor = style.get("color").asText();
            }
        }

        int paddingTotalH = 16;
        int labelRowHeight = 22;
        int barAreaHeight = Math.max(20, hauteur - paddingTotalH - labelRowHeight);

        StringBuilder chart = new StringBuilder();
        chart.append("<div style='width:").append(largeur).append("px;height:").append(hauteur)
             .append("px;padding:8px;box-sizing:border-box;border-left:1px solid #ccc;border-bottom:1px solid #ccc;font-family:Arial,sans-serif;'>");
        chart.append("<table style='width:100%;height:100%;table-layout:fixed;border-collapse:collapse;border:none;margin:0;padding:0;'>");

        // 1ère ligne : Barres du graphique (alignées à la base)
        chart.append("<tr style='height:").append(barAreaHeight).append("px;'>");
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> row)) continue;
            double value = (row.get("value") instanceof Number n) ? n.doubleValue() : 0;
            int barHeight = (int) Math.max(2, (value / max) * barAreaHeight);
            String valStr = (value == (long) value) ? String.valueOf((long) value) : String.format(java.util.Locale.US, "%.1f", value);

            chart.append("<td style='vertical-align:bottom;text-align:center;padding:0 2px;'>")
                 .append("<div style='font-size:8px;color:#777;margin-bottom:2px;'>")
                 .append(valStr).append("</div>")
                 .append("<div style='width:70%;margin:0 auto;background:").append(escape(barColor))
                 .append(";height:").append(barHeight).append("px;border-radius:3px 3px 0 0;'></div>")
                 .append("</td>");
        }
        chart.append("</tr>");

        // 2ème ligne : Libellés sous chaque barre
        chart.append("<tr style='height:").append(labelRowHeight).append("px;'>");
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> row)) continue;
            String label = String.valueOf(row.get("label"));
            chart.append("<td style='vertical-align:top;text-align:center;font-size:9px;color:#555;padding-top:4px;white-space:nowrap;overflow:hidden;'>")
                 .append(escape(label))
                 .append("</td>");
        }
        chart.append("</tr>");

        chart.append("</table></div>");
        return chart.toString();
    }

    private String placeholderBox(int w, int h, String label) {
        return "<table style='width:" + w + "px;height:" + h + "px;border:1px dashed #ccc;"
                + "border-collapse:collapse;margin:0;padding:0;box-sizing:border-box;'>"
                + "<tr><td style='vertical-align:middle;text-align:center;color:#999;"
                + "font-size:11px;font-family:Arial,sans-serif;padding:4px;'>"
                + escape(label) + "</td></tr></table>";
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

    private boolean shouldDisplayBlock(JsonNode bloc, Map<String, Object> data) {
        if (bloc == null) return false;
        if (bloc.hasNonNull("condition")) {
            String condition = bloc.path("condition").asText("");
            if (!condition.isBlank()) {
                return expressionEvaluator.evaluateCondition(condition, data);
            }
        }
        if (bloc.hasNonNull("showIf")) {
            String showIf = bloc.path("showIf").asText("");
            if (!showIf.isBlank()) {
                return expressionEvaluator.evaluateCondition(showIf, data);
            }
        }
        return true;
    }

    private String replaceVars(String content, Map<String, Object> data) {
        if (content == null || content.isBlank()) return "";
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            Object value = expressionEvaluator.evaluate(expr, data);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(escape(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String replaceVarsRaw(String content, Map<String, Object> data) {
        if (content == null || content.isBlank()) return "";
        Matcher matcher = VAR_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String expr = matcher.group(1).trim();
            Object value = expressionEvaluator.evaluate(expr, data);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String stripBraces(String source) {
        return source.replace("{{", "").replace("}}", "").trim();
    }

    private String replacePageTokens(String text, int pageNum, int totalPages, Map<String, Object> data) {
        if (text == null || text.isBlank()) return "";
        String result = text
            .replace("{page}", String.valueOf(pageNum))
            .replace("{pages}", String.valueOf(totalPages))
            .replace("{date}", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        return replaceVars(result, data);
    }

    private String getCssTextAlign(String align) {
        if (align == null) return "center";
        return switch (align.toUpperCase()) {
            case "GAUCHE", "LEFT" -> "left";
            case "DROITE", "RIGHT" -> "right";
            default -> "center";
        };
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}