import { StarterTemplate } from '../starter-template.model';

export const rapportActiviteTemplate: StarterTemplate = {
  id: 'rapport-activite',
  nom: 'Rapport d’activité corporate',
  description: 'Rapport exécutif périodique structuré avec en-tête officiel, cartouches KPI, synthèse exécutive et axes prioritaires.',
  categorie: 'ADMINISTRATION',
  iconName: 'bar-chart-3',
  couleurTag: '#0284c7',
  badge: 'Direction',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'titre_rapport', type: 'STRING', obligatoire: true, description: 'Titre principal du rapport' },
    { nomVariable: 'periode_rapport', type: 'STRING', obligatoire: true, description: 'Période analysée (ex: 1er Trimestre 2026)' },
    { nomVariable: 'auteur_nom', type: 'STRING', obligatoire: true, description: 'Auteur / Responsable de rédaction' },
    { nomVariable: 'entreprise_nom', type: 'STRING', obligatoire: true, description: 'Nom de l’entreprise' },
    { nomVariable: 'kpi_chiffre_affaires', type: 'STRING', obligatoire: false, description: 'Indicateur clé : CA ou croissance' },
    { nomVariable: 'kpi_taux_reussite', type: 'STRING', obligatoire: false, description: 'Indicateur clé : Taux d’atteinte d’objectifs' },
    { nomVariable: 'resume_executif', type: 'STRING', obligatoire: true, description: 'Synthèse exécutive et vision globale' },
    { nomVariable: 'points_cles', type: 'STRING', obligatoire: false, description: 'Principales réalisations de la période' },
    { nomVariable: 'actions_futures', type: 'STRING', obligatoire: false, description: 'Plan d’actions prioritaires et perspectives' },
  ],
  pages: [
    {
      id: 'rap-page-1',
      nom: 'Page 1',
      blocks: [
        // En-tête bandeau
        {
          id: 'rap-bandeau',
          type: 'rectangle',
          x: 38,
          y: 38,
          largeurBox: 718,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#0284c7', epaisseur: 0 }
        },
        {
          id: 'rap-entreprise',
          type: 'texte',
          x: 38,
          y: 54,
          largeurBox: 400,
          hauteurBox: 20,
          rotation: 0,
          contenu: '{{entreprise_nom}} · DIRECTION GÉNÉRALE',
          style: { fontSize: 10, bold: true, color: '#0369a1' }
        },
        {
          id: 'rap-titre',
          type: 'titre',
          x: 38,
          y: 74,
          largeurBox: 718,
          hauteurBox: 38,
          rotation: 0,
          contenu: '{{titre_rapport}} — {{periode_rapport}}',
          style: { fontSize: 22, bold: true, color: '#0f172a' }
        },
        {
          id: 'rap-auteur',
          type: 'texte',
          x: 38,
          y: 114,
          largeurBox: 500,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'Rédigé par : {{auteur_nom}}',
          style: { fontSize: 11, color: '#64748b' }
        },
        // 2 Cartouches KPI côte à côte
        {
          id: 'rap-bg-kpi1',
          type: 'rectangle',
          x: 38,
          y: 146,
          largeurBox: 345,
          hauteurBox: 75,
          rotation: 0,
          style: { fill: '#f0f9ff', couleur: '#bae6fd', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'rap-kpi1-label',
          type: 'texte',
          x: 52,
          y: 156,
          largeurBox: 315,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'PERFORMANCE COMMERCIALE',
          style: { fontSize: 10, bold: true, color: '#0284c7' }
        },
        {
          id: 'rap-kpi1-val',
          type: 'titre',
          x: 52,
          y: 176,
          largeurBox: 315,
          hauteurBox: 32,
          rotation: 0,
          contenu: '{{kpi_chiffre_affaires}}',
          style: { fontSize: 18, bold: true, color: '#0c4a6e' }
        },
        {
          id: 'rap-bg-kpi2',
          type: 'rectangle',
          x: 411,
          y: 146,
          largeurBox: 345,
          hauteurBox: 75,
          rotation: 0,
          style: { fill: '#f0fdf4', couleur: '#bbf7d0', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'rap-kpi2-label',
          type: 'texte',
          x: 425,
          y: 156,
          largeurBox: 315,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'TAUX D’ATTEINTE DES OBJECTIFS',
          style: { fontSize: 10, bold: true, color: '#059669' }
        },
        {
          id: 'rap-kpi2-val',
          type: 'titre',
          x: 425,
          y: 176,
          largeurBox: 315,
          hauteurBox: 32,
          rotation: 0,
          contenu: '{{kpi_taux_reussite}}',
          style: { fontSize: 18, bold: true, color: '#064e3b' }
        },
        // Section 1 : Synthèse exécutive
        {
          id: 'rap-sec1-titre',
          type: 'titre',
          x: 38,
          y: 245,
          largeurBox: 718,
          hauteurBox: 28,
          rotation: 0,
          contenu: '1. Synthèse Exécutive',
          style: { fontSize: 15, bold: true, color: '#1e293b' }
        },
        {
          id: 'rap-sec1-texte',
          type: 'texte',
          x: 38,
          y: 275,
          largeurBox: 718,
          hauteurBox: 90,
          rotation: 0,
          contenu: '{{resume_executif}}\n\nDurant cette période, nos équipes ont maintenu une dynamique positive en consolidant nos engagements stratégiques et en optimisant les processus opérationnels.',
          style: { fontSize: 11, color: '#334155' }
        },
        // Section 2 : Faits marquants
        {
          id: 'rap-sec2-titre',
          type: 'titre',
          x: 38,
          y: 380,
          largeurBox: 718,
          hauteurBox: 28,
          rotation: 0,
          contenu: '2. Faits Marquants & Réalisations',
          style: { fontSize: 15, bold: true, color: '#1e293b' }
        },
        {
          id: 'rap-sec2-texte',
          type: 'texte',
          x: 38,
          y: 410,
          largeurBox: 718,
          hauteurBox: 100,
          rotation: 0,
          contenu: '{{points_cles}}\n• Livraison des jalons opérationnels dans les délais impartis.\n• Renforcement de la satisfaction client et des indicateurs de conformité.',
          style: { fontSize: 11, color: '#334155' }
        },
        // Section 3 : Perspectives
        {
          id: 'rap-sec3-titre',
          type: 'titre',
          x: 38,
          y: 525,
          largeurBox: 718,
          hauteurBox: 28,
          rotation: 0,
          contenu: '3. Plan d’Actions & Perspectives',
          style: { fontSize: 15, bold: true, color: '#1e293b' }
        },
        {
          id: 'rap-sec3-texte',
          type: 'texte',
          x: 38,
          y: 555,
          largeurBox: 718,
          hauteurBox: 80,
          rotation: 0,
          contenu: '{{actions_futures}}\n\nLes priorités pour la prochaine période porteront sur l’automatisation des rapports et le développement des nouveaux modules métiers.',
          style: { fontSize: 11, color: '#334155' }
        }
      ]
    }
  ]
};

