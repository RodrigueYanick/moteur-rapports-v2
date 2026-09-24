import { StarterTemplate } from '../starter-template.model';

export const factureCommercialeTemplate: StarterTemplate = {
  id: 'facture-commerciale',
  nom: 'Facture commerciale',
  description: 'Facture complète avec coordonnées émetteur/client, tableau dynamique des prestations, sous-total HT, TVA et Net à payer TTC.',
  categorie: 'VENTES',
  iconName: 'receipt',
  couleurTag: '#2563eb',
  badge: 'Populaire',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'numero_facture', type: 'STRING', obligatoire: true, description: 'Numéro de facture (ex: FAC-2026-001)' },
    { nomVariable: 'date_facture', type: 'DATE', obligatoire: true, description: 'Date d’émission de la facture' },
    { nomVariable: 'date_echeance', type: 'DATE', obligatoire: true, description: 'Date limite de paiement' },
    { nomVariable: 'emetteur_nom', type: 'STRING', obligatoire: false, description: 'Raison sociale de l’émetteur' },
    { nomVariable: 'emetteur_adresse', type: 'STRING', obligatoire: false, description: 'Adresse du siège social émetteur' },
    { nomVariable: 'emetteur_siret', type: 'STRING', obligatoire: false, description: 'Numéro SIRET / RCS de l’émetteur' },
    { nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom ou raison sociale du client' },
    { nomVariable: 'client_adresse', type: 'STRING', obligatoire: true, description: 'Adresse de facturation client' },
    { nomVariable: 'client_email', type: 'STRING', obligatoire: false, description: 'Email de contact du client' },
    { nomVariable: 'total_ht', type: 'FLOAT', obligatoire: true, description: 'Montant Total Hors Taxes (€)' },
    { nomVariable: 'taux_tva', type: 'FLOAT', obligatoire: false, description: 'Taux de TVA en pourcentage (ex: 20%)' },
    { nomVariable: 'montant_tva', type: 'FLOAT', obligatoire: true, description: 'Montant total de la TVA (€)' },
    { nomVariable: 'total_ttc', type: 'FLOAT', obligatoire: true, description: 'Net à payer Toutes Taxes Comprises (€)' },
    { nomVariable: 'conditions_reglement', type: 'STRING', obligatoire: false, description: 'Mentions légales et modalités de règlement' },
  ],
  pages: [
    {
      id: 'facture-page-1',
      nom: 'Page 1',
      blocks: [
        // Bandeau haut couleur d'accent
        {
          id: 'fac-bandeau',
          type: 'rectangle',
          x: 38,
          y: 38,
          largeurBox: 718,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#2563eb', epaisseur: 0 }
        },
        // Titre principal FACTURE
        {
          id: 'fac-titre',
          type: 'titre',
          x: 38,
          y: 58,
          largeurBox: 340,
          hauteurBox: 44,
          rotation: 0,
          contenu: 'FACTURE',
          style: { fontSize: 28, bold: true, color: '#1e293b', align: 'left' }
        },
        // Métadonnées numéro et date
        {
          id: 'fac-meta',
          type: 'texte',
          x: 38,
          y: 104,
          largeurBox: 380,
          hauteurBox: 24,
          rotation: 0,
          contenu: 'N° {{numero_facture}} · Date : {{date_facture}}',
          style: { fontSize: 11, bold: false, color: '#64748b', align: 'left' }
        },
        // Cartouche Émetteur (fond léger)
        {
          id: 'fac-bg-emetteur',
          type: 'rectangle',
          x: 38,
          y: 136,
          largeurBox: 340,
          hauteurBox: 106,
          rotation: 0,
          style: { fill: '#f8fafc', couleur: '#e2e8f0', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'fac-emetteur-nom',
          type: 'titre',
          x: 50,
          y: 148,
          largeurBox: 316,
          hauteurBox: 24,
          rotation: 0,
          contenu: '{{emetteur_nom}}',
          style: { fontSize: 13, bold: true, color: '#0f172a' }
        },
        {
          id: 'fac-emetteur-details',
          type: 'texte',
          x: 50,
          y: 174,
          largeurBox: 316,
          hauteurBox: 58,
          rotation: 0,
          contenu: '{{emetteur_adresse}}\nSIRET : {{emetteur_siret}}',
          style: { fontSize: 11, color: '#475569' }
        },
        // Cartouche Client
        {
          id: 'fac-bg-client',
          type: 'rectangle',
          x: 416,
          y: 136,
          largeurBox: 340,
          hauteurBox: 106,
          rotation: 0,
          style: { fill: '#f1f5f9', couleur: '#cbd5e1', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'fac-client-label',
          type: 'texte',
          x: 430,
          y: 148,
          largeurBox: 312,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'FACTURÉ À :',
          style: { fontSize: 10, bold: true, color: '#64748b' }
        },
        {
          id: 'fac-client-nom',
          type: 'titre',
          x: 430,
          y: 168,
          largeurBox: 312,
          hauteurBox: 24,
          rotation: 0,
          contenu: '{{client_nom}}',
          style: { fontSize: 13, bold: true, color: '#0f172a' }
        },
        {
          id: 'fac-client-details',
          type: 'texte',
          x: 430,
          y: 192,
          largeurBox: 312,
          hauteurBox: 44,
          rotation: 0,
          contenu: '{{client_adresse}}\n{{client_email}}',
          style: { fontSize: 11, color: '#475569' }
        },
        // Ligne séparatrice
        {
          id: 'fac-sep',
          type: 'ligne',
          x: 38,
          y: 256,
          largeurBox: 718,
          hauteurBox: 4,
          rotation: 0,
          style: { epaisseur: 1, couleur: '#e2e8f0', largeur: 718 }
        },
        // Tableau des prestations
        {
          id: 'fac-tableau',
          type: 'tableau',
          x: 38,
          y: 272,
          largeurBox: 718,
          hauteurBox: 175,
          rotation: 0,
          lignes: [
            [
              { value: 'Désignation / Prestation', bgColor: '#f1f5f9', textColor: '#1e293b' },
              { value: 'Qté', bgColor: '#f1f5f9', textColor: '#1e293b' },
              { value: 'Prix Unitaire HT', bgColor: '#f1f5f9', textColor: '#1e293b' },
              { value: 'Total HT', bgColor: '#f1f5f9', textColor: '#1e293b' }
            ],
            [
              { value: 'Prestation de conseil et architecture logicielle' },
              { value: '5 j' },
              { value: '850,00 €' },
              { value: '4 250,00 €' }
            ],
            [
              { value: 'Développement d’interfaces et composants de rapports' },
              { value: '12 j' },
              { value: '650,00 €' },
              { value: '7 800,00 €' }
            ],
            [
              { value: 'Formation technique et transfert de compétences' },
              { value: '2 j' },
              { value: '750,00 €' },
              { value: '1 500,00 €' }
            ]
          ],
          style: { bordureCouleur: '#e2e8f0', texteCouleurDefaut: '#334155' }
        },
        // Échéance et mode de règlement (gauche)
        {
          id: 'fac-echeance',
          type: 'texte',
          x: 38,
          y: 470,
          largeurBox: 380,
          hauteurBox: 45,
          rotation: 0,
          contenu: 'Date limite de règlement : {{date_echeance}}\nMode de règlement : Virement bancaire',
          style: { fontSize: 11, bold: false, color: '#475569' }
        },
        // Cartouche Récapitulatif Totaux (droite)
        {
          id: 'fac-bg-totaux',
          type: 'rectangle',
          x: 456,
          y: 465,
          largeurBox: 300,
          hauteurBox: 120,
          rotation: 0,
          style: { fill: '#f8fafc', couleur: '#e2e8f0', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'fac-total-ht',
          type: 'texte',
          x: 470,
          y: 476,
          largeurBox: 272,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Total HT : {{total_ht}} €',
          style: { fontSize: 12, bold: false, color: '#334155', align: 'right' }
        },
        {
          id: 'fac-total-tva',
          type: 'texte',
          x: 470,
          y: 500,
          largeurBox: 272,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'TVA ({{taux_tva}}%) : {{montant_tva}} €',
          style: { fontSize: 12, bold: false, color: '#334155', align: 'right' }
        },
        // Bandeau Net à Payer
        {
          id: 'fac-bg-net',
          type: 'rectangle',
          x: 456,
          y: 532,
          largeurBox: 300,
          hauteurBox: 53,
          rotation: 0,
          style: { fill: '#2563eb', epaisseur: 0, borderRadius: 4 }
        },
        {
          id: 'fac-net-payer',
          type: 'titre',
          x: 470,
          y: 546,
          largeurBox: 272,
          hauteurBox: 26,
          rotation: 0,
          contenu: 'NET À PAYER : {{total_ttc}} €',
          style: { fontSize: 14, bold: true, color: '#ffffff', align: 'right' }
        },
        // Mentions légales en pied de page
        {
          id: 'fac-mentions',
          type: 'texte',
          x: 38,
          y: 980,
          largeurBox: 718,
          hauteurBox: 50,
          rotation: 0,
          contenu: '{{conditions_reglement}}\nPaiement à 30 jours date d’émission. Pas d’escompte pour paiement anticipé. En cas de retard de paiement, indemnité forfaitaire pour frais de recouvrement de 40 €.',
          style: { fontSize: 9, color: '#94a3b8', align: 'center' }
        }
      ]
    }
  ]
};

