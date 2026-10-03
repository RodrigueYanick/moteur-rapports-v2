package com.rapports.moteur.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Implémentation déterministe hors-ligne du client IA.
 * Utilisée pour les tests et comme solution de secours (graceful fallback)
 * lorsque l'API distante n'est pas configurée ou indisponible.
 */
@Slf4j
@Component
public class MockAiClient implements AiClient {

    @Override
    public String generateContent(String systemInstruction, String userPrompt, boolean jsonMode) {
        log.info("MockAiClient invoked with userPrompt: {}", userPrompt);
        String lower = userPrompt != null ? userPrompt.toLowerCase() : "";

        // Cas 1 : Génération de données de test (Mock Data)
        String sysLower = systemInstruction != null ? systemInstruction.toLowerCase() : "";
        if (sysLower.contains("mock data") || sysLower.contains("données de test") || sysLower.contains("donnees de test") || sysLower.contains("jeux de données") || sysLower.contains("jeu de données")) {
            return generateMockDataJson(lower);
        }

        // Cas 2 : Génération de modèle (Template Builder)
        return generateTemplateJson(lower);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    private String generateTemplateJson(String prompt) {
        String docType = "DEVIS";
        String color = "#1e3a8a"; // bleu entreprise
        if (prompt.contains("facture")) {
            docType = "FACTURE";
            color = "#047857"; // vert émeraude
        } else if (prompt.contains("bon de commande") || prompt.contains("commande")) {
            docType = "BON DE COMMANDE";
            color = "#b45309"; // ambre
        } else if (prompt.contains("rapport") || prompt.contains("audit")) {
            docType = "RAPPORT D'ACTIVITÉ";
            color = "#4338ca"; // indigo
        }

        return """
        {
          "nom": "%s PROFESSIONNEL",
          "description": "Modèle généré automatiquement par l'IA",
          "formatPapier": "A4",
          "categorie": "VENTES",
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "b_titre_%s",
                  "type": "titre",
                  "x": 40,
                  "y": 40,
                  "largeurBox": 714,
                  "contenu": "%s N° {{numero_document}}",
                  "style": {
                    "fontSize": 24,
                    "bold": true,
                    "color": "%s",
                    "align": "left"
                  }
                },
                {
                  "id": "b_emetteur_%s",
                  "type": "texte",
                  "x": 40,
                  "y": 100,
                  "largeurBox": 320,
                  "contenu": "<b>{{entreprise_nom}}</b><br/>{{entreprise_adresse}}<br/>SIRET: {{entreprise_siret}}<br/>Email: {{entreprise_email}}",
                  "style": {
                    "fontSize": 12,
                    "color": "#374151"
                  }
                },
                {
                  "id": "b_client_%s",
                  "type": "texte",
                  "x": 420,
                  "y": 100,
                  "largeurBox": 334,
                  "contenu": "<b>Facturé à :</b><br/><b>{{client_nom}}</b><br/>{{client_adresse}}<br/>Contact: {{client_contact}}",
                  "style": {
                    "fontSize": 12,
                    "color": "#1f2937"
                  }
                },
                {
                  "id": "b_meta_%s",
                  "type": "texte",
                  "x": 40,
                  "y": 200,
                  "largeurBox": 714,
                  "contenu": "Date d'émission : <b>{{date_emission}}</b> | Date d'échéance : <b>{{date_echeance}}</b>",
                  "style": {
                    "fontSize": 11,
                    "color": "#6b7280"
                  }
                },
                {
                  "id": "b_table_%s",
                  "type": "tableau",
                  "x": 40,
                  "y": 240,
                  "largeurBox": 714,
                  "source": "{{lignes}}",
                  "colonnes": [
                    { "titre": "Désignation des prestations", "variable": "designation" },
                    { "titre": "Quantité", "variable": "quantite" },
                    { "titre": "Prix Unit. HT", "variable": "prix_unitaire" },
                    { "titre": "Total HT", "variable": "total_ligne" }
                  ]
                },
                {
                  "id": "b_totaux_%s",
                  "type": "texte",
                  "x": 450,
                  "y": 500,
                  "largeurBox": 304,
                  "contenu": "Total HT : <b>{{total_ht}} €</b><br/>TVA (20%%) : <b>{{montant_tva}} €</b><br/><hr/><span style='font-size:16px;color:%s'><b>Total TTC : {{total_ttc}} €</b></span>",
                  "style": {
                    "fontSize": 13,
                    "color": "#111827",
                    "align": "right"
                  }
                },
                {
                  "id": "b_conditions_%s",
                  "type": "texte",
                  "x": 40,
                  "y": 640,
                  "largeurBox": 714,
                  "contenu": "Conditions de règlement : Paiement à réception par virement bancaire.<br/>IBAN: <b>{{entreprise_iban}}</b> | BIC: <b>{{entreprise_bic}}</b>",
                  "style": {
                    "fontSize": 10,
                    "color": "#9ca3af",
                    "align": "center"
                  }
                }
              ]
            }
          ]
        }
        """.formatted(
                docType,
                UUID.randomUUID().toString().substring(0, 8),
                docType,
                color,
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID().toString().substring(0, 8),
                color,
                UUID.randomUUID().toString().substring(0, 8)
        );
    }

    private String generateMockDataJson(String prompt) {
        String today = LocalDate.now().toString();
        String dueDate = LocalDate.now().plusDays(30).toString();

        return """
        {
          "numero_document": "DEV-2026-0042",
          "entreprise_nom": "Ateliers Modernes SARL",
          "entreprise_adresse": "12 rue de la Paix, 75002 Paris",
          "entreprise_siret": "834 912 405 00028",
          "entreprise_email": "contact@ateliers-modernes.fr",
          "entreprise_iban": "FR76 3000 4000 1234 5678 9012 345",
          "entreprise_bic": "BNPAFRPP",
          "client_nom": "Bâtiments & Rénovation SAS",
          "client_adresse": "45 avenue Jean Jaurès, 69007 Lyon",
          "client_contact": "M. Robert Lefebvre",
          "date_emission": "%s",
          "date_echeance": "%s",
          "total_ht": 3450.00,
          "montant_tva": 690.00,
          "total_ttc": 4140.00,
          "lignes": [
            {
              "designation": "Diagnostic technique initial et étude préparatoire",
              "quantite": 1,
              "prix_unitaire": 650.00,
              "total_ligne": 650.00
            },
            {
              "designation": "Mise en conformité des installations électriques",
              "quantite": 14,
              "prix_unitaire": 120.00,
              "total_ligne": 1680.00
            },
            {
              "designation": "Fourniture et pose de disjoncteurs différentiels 30mA",
              "quantite": 4,
              "prix_unitaire": 280.00,
              "total_ligne": 1120.00
            }
          ]
        }
        """.formatted(today, dueDate);
    }
}
