import { StarterTemplate } from '../starter-template.model';

export const bulletinPaieTemplate: StarterTemplate = {
  id: 'bulletin-paie',
  nom: 'Bulletin de paie & Relevé d’heures',
  description: 'Fiche de paie synthétique et relevé d’activité avec période, salarié, rubriques salariales et Net à payer.',
  categorie: 'RH',
  iconName: 'briefcase',
  couleurTag: '#7c3aed',
  badge: 'RH & Social',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'periode', type: 'STRING', obligatoire: true, description: 'Mois et année de paie (ex: Mars 2026)' },
    { nomVariable: 'entreprise_nom', type: 'STRING', obligatoire: true, description: 'Raison sociale employeur' },
    { nomVariable: 'salarie_nom', type: 'STRING', obligatoire: true, description: 'Nom et prénom du salarié' },
    { nomVariable: 'salarie_matricule', type: 'STRING', obligatoire: true, description: 'Matricule interne du salarié' },
    { nomVariable: 'salarie_poste', type: 'STRING', obligatoire: false, description: 'Poste et qualification' },
    { nomVariable: 'heures_travaillees', type: 'FLOAT', obligatoire: true, description: 'Heures travaillées sur la période' },
    { nomVariable: 'salaire_brut', type: 'FLOAT', obligatoire: true, description: 'Salaire Brut total (€)' },
    { nomVariable: 'total_cotisations', type: 'FLOAT', obligatoire: true, description: 'Total des cotisations salariales (€)' },
    { nomVariable: 'net_imposable', type: 'FLOAT', obligatoire: false, description: 'Net fiscal imposable (€)' },
    { nomVariable: 'net_a_payer', type: 'FLOAT', obligatoire: true, description: 'Net à payer avant impôt (€)' },
  ],
  pages: [
    {
      id: 'paie-page-1',
      nom: 'Page 1',
      blocks: [
        // Bandeau supérieur violet
        {
          id: 'paie-bandeau',
          type: 'rectangle',
          x: 38,
          y: 38,
          largeurBox: 718,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#7c3aed', epaisseur: 0 }
        },
        // Titre
        {
          id: 'paie-titre',
          type: 'titre',
          x: 38,
          y: 56,
          largeurBox: 460,
          hauteurBox: 38,
          rotation: 0,
          contenu: 'BULLETIN DE PAIE SIMPLIFIÉ',
          style: { fontSize: 24, bold: true, color: '#4c1d95' }
        },
        {
          id: 'paie-periode',
          type: 'texte',
          x: 38,
          y: 98,
          largeurBox: 460,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Période d’activité : {{periode}} · Entreprise : {{entreprise_nom}}',
          style: { fontSize: 11, bold: true, color: '#6d28d9' }
        },
        // Fiche salarié
        {
          id: 'paie-bg-salarie',
          type: 'rectangle',
          x: 38,
          y: 130,
          largeurBox: 718,
          hauteurBox: 70,
          rotation: 0,
          style: { fill: '#faf5ff', couleur: '#e9d5ff', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'paie-salarie-nom',
          type: 'titre',
          x: 54,
          y: 142,
          largeurBox: 320,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Salarié : {{salarie_nom}}',
          style: { fontSize: 13, bold: true, color: '#0f172a' }
        },
        {
          id: 'paie-salarie-matricule',
          type: 'texte',
          x: 54,
          y: 168,
          largeurBox: 320,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'Matricule : {{salarie_matricule}} · Poste : {{salarie_poste}}',
          style: { fontSize: 11, color: '#475569' }
        },
        {
          id: 'paie-heures-txt',
          type: 'texte',
          x: 420,
          y: 155,
          largeurBox: 320,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Volume d’heures travaillées : {{heures_travaillees}} h',
          style: { fontSize: 12, bold: true, color: '#5b21b6', align: 'right' }
        },
        // Tableau rubriques
        {
          id: 'paie-tableau',
          type: 'tableau',
          x: 38,
          y: 220,
          largeurBox: 718,
          hauteurBox: 180,
          rotation: 0,
          lignes: [
            [
              { value: 'Rubrique', bgColor: '#f3e8ff', textColor: '#581c87' },
              { value: 'Base (€)', bgColor: '#f3e8ff', textColor: '#581c87' },
              { value: 'Taux', bgColor: '#f3e8ff', textColor: '#581c87' },
              { value: 'Part Salariale (€)', bgColor: '#f3e8ff', textColor: '#581c87' },
              { value: 'Part Patronale (€)', bgColor: '#f3e8ff', textColor: '#581c87' }
            ],
            [
              { value: 'Salaire de base (mensualisé)' },
              { value: '3 200,00' },
              { value: '-' },
              { value: '+ 3 200,00' },
              { value: '-' }
            ],
            [
              { value: 'Santé - Complémentaire & Incapacité' },
              { value: '3 200,00' },
              { value: '1,50 %' },
              { value: '- 48,00' },
              { value: '96,00' }
            ],
            [
              { value: 'Retraite complémentaire AGIRC-ARRCO' },
              { value: '3 200,00' },
              { value: '3,86 %' },
              { value: '- 123,52' },
              { value: '185,28' }
            ],
            [
              { value: 'CSG & CRDS (déductible et non-déd.)' },
              { value: '3 144,00' },
              { value: '9,70 %' },
              { value: '- 304,97' },
              { value: '-' }
            ]
          ],
          style: { bordureCouleur: '#e9d5ff', texteCouleurDefaut: '#334155' }
        },
        // Récapitulatif brut et cotisations
        {
          id: 'paie-recap',
          type: 'texte',
          x: 38,
          y: 420,
          largeurBox: 380,
          hauteurBox: 50,
          rotation: 0,
          contenu: 'Salaire Brut : {{salaire_brut}} €\nTotal cotisations : {{total_cotisations}} €\nNet fiscal imposable : {{net_imposable}} €',
          style: { fontSize: 11, color: '#475569' }
        },
        // Cartouche NET A PAYER
        {
          id: 'paie-bg-net',
          type: 'rectangle',
          x: 440,
          y: 420,
          largeurBox: 316,
          hauteurBox: 60,
          rotation: 0,
          style: { fill: '#7c3aed', epaisseur: 0, borderRadius: 6 }
        },
        {
          id: 'paie-net-titre',
          type: 'titre',
          x: 450,
          y: 435,
          largeurBox: 296,
          hauteurBox: 30,
          rotation: 0,
          contenu: 'NET À PAYER : {{net_a_payer}} €',
          style: { fontSize: 15, bold: true, color: '#ffffff', align: 'center' }
        },
        // Mention légale de conservation
        {
          id: 'paie-mention',
          type: 'texte',
          x: 38,
          y: 990,
          largeurBox: 718,
          hauteurBox: 30,
          rotation: 0,
          contenu: 'Dans votre intérêt et pour faire valoir vos droits, conservez ce bulletin de paie sans limitation de durée.',
          style: { fontSize: 9, color: '#94a3b8', align: 'center' }
        }
      ]
    }
  ]
};

