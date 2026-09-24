import { StarterTemplate } from '../starter-template.model';

export const rapportAvancementFacturationTemplate: StarterTemplate = {
  id: 'rapport-avancement-facturation',
  nom: 'Rapport d’Avancement & Facturation',
  description: 'Rapport exécutif combinant indicateurs clés (KPIs), tableau de facturation des livrables, histogramme des sprints, donut de couverture de tests, QR code et signatures.',
  categorie: 'VENTES',
  iconName: 'barChart',
  couleurTag: '#0284c7',
  badge: 'Exécutif',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'titre_rapport', type: 'STRING', obligatoire: true, description: 'Titre principal (ex: Rapport d’Avancement & Facturation - Mobile Banking 2.0)' },
    { nomVariable: 'date_rapport', type: 'DATE', obligatoire: true, description: 'Date d’émission (ex: 20 Septembre 2026)' },
    { nomVariable: 'projet_code', type: 'STRING', obligatoire: true, description: 'Code projet (ex: PROJ-MB20-14)' },
    { nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom du client (ex: Banque Atlantique Cameroun)' },
    { nomVariable: 'avancement_global', type: 'STRING', obligatoire: true, description: 'Pourcentage d’avancement global (ex: 78%)' },
    { nomVariable: 'livrables_completes', type: 'STRING', obligatoire: true, description: 'Ratio des livrables (ex: 32/45)' },
    { nomVariable: 'valeur_projet', type: 'STRING', obligatoire: true, description: 'Valeur projet accumulée (ex: FCFA 18,500,000)' },
    { nomVariable: 'periode_facturation', type: 'STRING', obligatoire: true, description: 'Période analysée (ex: Août-Septembre 2026)' },
    { nomVariable: 'sous_total', type: 'STRING', obligatoire: true, description: 'Sous-total HT (ex: 15,800,000)' },
    { nomVariable: 'total_net_payer', type: 'STRING', obligatoire: true, description: 'Montant total net à payer (ex: 15,800,000)' },
    { nomVariable: 'date_echeance', type: 'DATE', obligatoire: true, description: 'Date limite de règlement (ex: 05 Octobre 2026)' },
    { nomVariable: 'qr_verification_url', type: 'STRING', obligatoire: false, description: 'Lien de vérification en ligne du document' },
    { nomVariable: 'livrables', type: 'ARRAY', obligatoire: true, description: 'Liste des livrables facturés' },
    { nomVariable: 'sprints_progress', type: 'ARRAY', obligatoire: true, description: 'Données pour l’histogramme des sprints' },
    { nomVariable: 'tests_coverage', type: 'ARRAY', obligatoire: true, description: 'Données pour le donut de tests' },
  ],
  pages: [
    {
      id: 'page-avancement-1',
      nom: 'Page 1',
      blocks: [
        // Accent décoratif haut droit
        {
          id: 'raf-accent-top',
          type: 'rectangle',
          x: 580,
          y: 0,
          largeurBox: 214,
          hauteurBox: 24,
          rotation: 0,
          style: { fill: '#0284c7', epaisseur: 0 }
        },
        // Logo & Raison sociale
        {
          id: 'raf-logo-text',
          type: 'texte',
          x: 40,
          y: 35,
          largeurBox: 300,
          hauteurBox: 28,
          rotation: 0,
          contenu: '▲▼ NexGen Tech',
          style: { fontSize: 18, bold: true, color: '#0369a1' }
        },
        // Titre principal
        {
          id: 'raf-titre',
          type: 'titre',
          x: 40,
          y: 68,
          largeurBox: 714,
          hauteurBox: 42,
          rotation: 0,
          contenu: '{{titre_rapport}}',
          style: { fontSize: 22, bold: true, color: '#0f2744' }
        },
        // Métadonnées
        {
          id: 'raf-meta',
          type: 'texte',
          x: 40,
          y: 112,
          largeurBox: 714,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'Date: {{date_rapport}} | Projet: {{projet_code}} | Client: {{client_nom}}',
          style: { fontSize: 10, bold: true, color: '#475569' }
        },

        // ----------------------------------------------------
        // KPI 1 : Avancement Global
        // ----------------------------------------------------
        {
          id: 'raf-kpi1-bg',
          type: 'rectangle',
          x: 40,
          y: 140,
          largeurBox: 220,
          hauteurBox: 75,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#cbd5e1', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'raf-kpi1-label',
          type: 'texte',
          x: 45,
          y: 148,
          largeurBox: 210,
          hauteurBox: 16,
          rotation: 0,
          contenu: 'AVANCEMENT GLOBAL',
          style: { fontSize: 9, bold: true, color: '#64748b', align: 'center' }
        },
        {
          id: 'raf-kpi1-val',
          type: 'titre',
          x: 45,
          y: 164,
          largeurBox: 210,
          hauteurBox: 28,
          rotation: 0,
          contenu: '{{avancement_global}}',
          style: { fontSize: 22, bold: true, color: '#0f172a', align: 'center' }
        },
        {
          id: 'raf-kpi1-track',
          type: 'rectangle',
          x: 55,
          y: 198,
          largeurBox: 190,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#e2e8f0', epaisseur: 0, borderRadius: 3 }
        },
        {
          id: 'raf-kpi1-fill',
          type: 'rectangle',
          x: 55,
          y: 198,
          largeurBox: 148,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#0284c7', epaisseur: 0, borderRadius: 3 }
        },

        // ----------------------------------------------------
        // KPI 2 : Livrables Complétés
        // ----------------------------------------------------
        {
          id: 'raf-kpi2-bg',
          type: 'rectangle',
          x: 280,
          y: 140,
          largeurBox: 220,
          hauteurBox: 75,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#cbd5e1', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'raf-kpi2-label',
          type: 'texte',
          x: 285,
          y: 148,
          largeurBox: 210,
          hauteurBox: 16,
          rotation: 0,
          contenu: 'LIVRABLES COMPLÉTÉS',
          style: { fontSize: 9, bold: true, color: '#64748b', align: 'center' }
        },
        {
          id: 'raf-kpi2-val',
          type: 'titre',
          x: 285,
          y: 168,
          largeurBox: 210,
          hauteurBox: 28,
          rotation: 0,
          contenu: '{{livrables_completes}}',
          style: { fontSize: 22, bold: true, color: '#0f172a', align: 'center' }
        },

        // ----------------------------------------------------
        // KPI 3 : Valeur Projet Accumulée
        // ----------------------------------------------------
        {
          id: 'raf-kpi3-bg',
          type: 'rectangle',
          x: 520,
          y: 140,
          largeurBox: 234,
          hauteurBox: 75,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#cbd5e1', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'raf-kpi3-label',
          type: 'texte',
          x: 525,
          y: 148,
          largeurBox: 224,
          hauteurBox: 16,
          rotation: 0,
          contenu: 'VALEUR PROJET ACCUMULÉE',
          style: { fontSize: 9, bold: true, color: '#64748b', align: 'center' }
        },
        {
          id: 'raf-kpi3-val',
          type: 'titre',
          x: 525,
          y: 170,
          largeurBox: 224,
          hauteurBox: 28,
          rotation: 0,
          contenu: '{{valeur_projet}}',
          style: { fontSize: 16, bold: true, color: '#0f172a', align: 'center' }
        },

        // ----------------------------------------------------
        // Titre de section Facturation
        // ----------------------------------------------------
        {
          id: 'raf-section-titre',
          type: 'texte',
          x: 40,
          y: 228,
          largeurBox: 714,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Rapport de Facturation des Livrables - Période: {{periode_facturation}}',
          style: { fontSize: 13, bold: true, color: '#0f2744' }
        },

        // ----------------------------------------------------
        // Colonne Gauche : Tableau dynamique des Livrables
        // ----------------------------------------------------
        {
          id: 'raf-table-livrables',
          type: 'tableau',
          x: 40,
          y: 255,
          largeurBox: 440,
          hauteurBox: 180,
          rotation: 0,
          source: '{{livrables}}',
          colonnes: [
            { titre: 'Livrable #', variable: 'id_livrable' },
            { titre: 'Description du Livrable', variable: 'description' },
            { titre: 'Sprint #', variable: 'sprint' },
            { titre: 'Avancement', variable: 'avancement' },
            { titre: 'Coût (FCFA)', variable: 'cout' }
          ],
          style: { bordureCouleur: '#e2e8f0', texteCouleurDefaut: '#1e293b' }
        },

        // Ligne de Sous-total
        {
          id: 'raf-subtotal',
          type: 'texte',
          x: 40,
          y: 445,
          largeurBox: 440,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'Subtotal : {{sous_total}} FCFA',
          style: { fontSize: 10, bold: true, color: '#475569', align: 'right' }
        },

        // Bandeau TOTAL NET À PAYER
        {
          id: 'raf-total-bg',
          type: 'rectangle',
          x: 40,
          y: 468,
          largeurBox: 440,
          hauteurBox: 32,
          rotation: 0,
          style: { fill: '#0f2744', epaisseur: 0, borderRadius: 4 }
        },
        {
          id: 'raf-total-txt',
          type: 'texte',
          x: 50,
          y: 475,
          largeurBox: 420,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'TOTAL NET À PAYER : {{total_net_payer}} FCFA',
          style: { fontSize: 12, bold: true, color: '#ffffff' }
        },
        {
          id: 'raf-echeance',
          type: 'texte',
          x: 40,
          y: 505,
          largeurBox: 440,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'Date Échéance: {{date_echeance}}',
          style: { fontSize: 10, bold: true, color: '#475569' }
        },

        // ----------------------------------------------------
        // Colonne Droite : Graphiques vectoriels
        // ----------------------------------------------------
        // 1. Bar Chart "Progrès des Sprints"
        {
          id: 'raf-chart1-titre',
          type: 'texte',
          x: 495,
          y: 255,
          largeurBox: 259,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'Progrès des Sprints',
          style: { fontSize: 11, bold: true, color: '#0f2744', align: 'center' }
        },
        {
          id: 'raf-chart-sprints',
          type: 'graphique',
          graphiqueType: 'bar',
          source: '{{sprints_progress}}',
          graphiqueLabelKey: 'sprint',
          graphiqueValueKey: 'progression',
          graphiquePalette: ['#ea580c', '#f97316', '#fb923c', '#fdba74'],
          x: 495,
          y: 275,
          largeurBox: 259,
          hauteurBox: 120,
          rotation: 0
        },

        // 2. Donut Chart "Couverture des Tests"
        {
          id: 'raf-chart2-titre',
          type: 'texte',
          x: 495,
          y: 405,
          largeurBox: 259,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'Couverture des Tests',
          style: { fontSize: 11, bold: true, color: '#0f2744', align: 'center' }
        },
        {
          id: 'raf-chart-tests',
          type: 'graphique',
          graphiqueType: 'donut',
          source: '{{tests_coverage}}',
          graphiqueLabelKey: 'type_test',
          graphiqueValueKey: 'pourcentage',
          graphiquePalette: ['#0f2744', '#ea580c', '#fbbf24'],
          x: 495,
          y: 425,
          largeurBox: 259,
          hauteurBox: 120,
          rotation: 0
        },

        // ----------------------------------------------------
        // Bas de Page : Signature Client, QR Code, Cachet
        // ----------------------------------------------------
        // Cadre Signature Client
        {
          id: 'raf-sign-client-box',
          type: 'rectangle',
          x: 40,
          y: 545,
          largeurBox: 210,
          hauteurBox: 110,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#cbd5e1', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'raf-sign-client-tab',
          type: 'rectangle',
          x: 40,
          y: 625,
          largeurBox: 210,
          hauteurBox: 30,
          rotation: 0,
          style: { fill: '#0f2744', epaisseur: 0, borderRadius: 4 }
        },
        {
          id: 'raf-sign-client-tab-txt',
          type: 'texte',
          x: 40,
          y: 632,
          largeurBox: 210,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'Signature Client',
          style: { fontSize: 11, bold: true, color: '#ffffff', align: 'center' }
        },

        // QR Code de vérification
        {
          id: 'raf-qr-label',
          type: 'texte',
          x: 265,
          y: 545,
          largeurBox: 160,
          hauteurBox: 16,
          rotation: 0,
          contenu: 'QR de Vérification - NexGen Tech',
          style: { fontSize: 9, bold: true, color: '#0f2744', align: 'center' }
        },
        {
          id: 'raf-qrcode',
          type: 'qrcode',
          x: 295,
          y: 565,
          largeurBox: 100,
          hauteurBox: 100,
          rotation: 0,
          url: 'https://nexgen-tech.com/verify?doc={{projet_code}}&client={{client_nom}}'
        },

        // Cadre Cachet & Signature Émetteur
        {
          id: 'raf-sign-provider-box',
          type: 'rectangle',
          x: 440,
          y: 545,
          largeurBox: 314,
          hauteurBox: 110,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#93c5fd', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'raf-sign-provider-label',
          type: 'texte',
          x: 450,
          y: 552,
          largeurBox: 294,
          hauteurBox: 28,
          rotation: 0,
          contenu: 'Signature · NexGen Tech\nDigital stamp signature',
          style: { fontSize: 10, bold: true, color: '#0369a1' }
        },
        {
          id: 'raf-sign-provider',
          type: 'signature',
          x: 570,
          y: 580,
          largeurBox: 170,
          hauteurBox: 65,
          rotation: 0
        },

        // Ligne décorative bas de page
        {
          id: 'raf-footer-line',
          type: 'rectangle',
          x: 40,
          y: 675,
          largeurBox: 714,
          hauteurBox: 5,
          rotation: 0,
          style: { fill: '#0f2744', epaisseur: 0 }
        }
      ]
    }
  ]
};

