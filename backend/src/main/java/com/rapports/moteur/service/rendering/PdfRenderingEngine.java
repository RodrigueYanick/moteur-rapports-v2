package com.rapports.moteur.service.rendering;

/**
 * Interface standardisée d'abstraction pour les moteurs de rendu PDF.
 * Permet de découpler le service métier (ReportGenerationService)
 * des implémentations techniques sous-jacentes (Chromium/Gotenberg ou Flying Saucer).
 */
public interface PdfRenderingEngine {

    /**
     * Génère un flux binaire PDF à partir d'un document HTML et d'options de rendu.
     *
     * @param html    contenu HTML/CSS compilé
     * @param options paramètres de page (dimensions, marges, etc.)
     * @return flux binaire du PDF généré
     */
    byte[] render(String html, RenderOptions options);

    /**
     * Identifiant unique du moteur (ex: "gotenberg", "flying-saucer").
     */
    String getEngineName();

    /**
     * Vérifie si le moteur est disponible et opérationnel.
     */
    boolean isAvailable();
}

