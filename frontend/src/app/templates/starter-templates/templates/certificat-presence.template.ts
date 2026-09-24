import { StarterTemplate } from '../starter-template.model';

export const certificatPresenceTemplate: StarterTemplate = {
  id: 'certificat-presence',
  nom: 'Certificat / Attestation de présence',
  description: 'Attestation solennelle avec bordure d’honneur, identité du bénéficiaire, volume horaire et signature officielle.',
  categorie: 'RH',
  iconName: 'award',
  couleurTag: '#d97706',
  badge: 'Formation',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'beneficiaire_nom', type: 'STRING', obligatoire: true, description: 'Nom et prénom du participant ou stagiaire' },
    { nomVariable: 'intitule_formation', type: 'STRING', obligatoire: true, description: 'Intitulé officiel de la formation / session' },
    { nomVariable: 'organisme_nom', type: 'STRING', obligatoire: true, description: 'Nom de l’organisme de formation émetteur' },
    { nomVariable: 'duree_heures', type: 'FLOAT', obligatoire: true, description: 'Volume horaire validé (heures)' },
    { nomVariable: 'date_delivrance', type: 'DATE', obligatoire: true, description: 'Date de signature et remise' },
    { nomVariable: 'lieu_delivrance', type: 'STRING', obligatoire: false, description: 'Ville / Lieu d’émission' },
    { nomVariable: 'signataire_nom', type: 'STRING', obligatoire: true, description: 'Nom du responsable signataire' },
    { nomVariable: 'signataire_titre', type: 'STRING', obligatoire: false, description: 'Fonction du signataire (ex: Directeur pédagogique)' },
  ],
  pages: [
    {
      id: 'cert-page-1',
      nom: 'Page 1',
      blocks: [
        // Cadre extérieur orné
        {
          id: 'cert-cadre-ext',
          type: 'rectangle',
          x: 38,
          y: 38,
          largeurBox: 718,
          hauteurBox: 1047,
          rotation: 0,
          style: { fill: '#ffffff', couleur: '#d97706', epaisseur: 3, borderRadius: 12 }
        },
        // Cadre intérieur fin
        {
          id: 'cert-cadre-int',
          type: 'rectangle',
          x: 48,
          y: 48,
          largeurBox: 698,
          hauteurBox: 1027,
          rotation: 0,
          style: { fill: 'transparent', couleur: '#fde68a', epaisseur: 1, borderRadius: 8 }
        },
        // Organisme de formation
        {
          id: 'cert-organisme',
          type: 'texte',
          x: 80,
          y: 90,
          largeurBox: 634,
          hauteurBox: 24,
          rotation: 0,
          contenu: '{{organisme_nom}}',
          style: { fontSize: 13, bold: true, color: '#92400e', align: 'center' }
        },
        // Titre solennel
        {
          id: 'cert-titre',
          type: 'titre',
          x: 80,
          y: 130,
          largeurBox: 634,
          hauteurBox: 50,
          rotation: 0,
          contenu: 'ATTESTATION DE FORMATION',
          style: { fontSize: 26, bold: true, color: '#78350f', align: 'center' }
        },
        {
          id: 'cert-subtitre',
          type: 'texte',
          x: 80,
          y: 185,
          largeurBox: 634,
          hauteurBox: 24,
          rotation: 0,
          contenu: 'ET DE PARTICIPATION EFFECTIVE',
          style: { fontSize: 12, bold: true, color: '#b45309', align: 'center' }
        },
        // Formule officielle
        {
          id: 'cert-formule',
          type: 'texte',
          x: 80,
          y: 260,
          largeurBox: 634,
          hauteurBox: 30,
          rotation: 0,
          contenu: 'Il est certifié par la présente que :',
          style: { fontSize: 13, italic: true, color: '#64748b', align: 'center' }
        },
        // Nom du participant
        {
          id: 'cert-beneficiaire',
          type: 'titre',
          x: 80,
          y: 300,
          largeurBox: 634,
          hauteurBox: 50,
          rotation: 0,
          contenu: '{{beneficiaire_nom}}',
          style: { fontSize: 24, bold: true, color: '#1e293b', align: 'center' }
        },
        // Ligne dorée
        {
          id: 'cert-ligne',
          type: 'ligne',
          x: 230,
          y: 360,
          largeurBox: 334,
          hauteurBox: 4,
          rotation: 0,
          style: { epaisseur: 2, couleur: '#d97706', largeur: 334 }
        },
        // Texte de complétion
        {
          id: 'cert-corps',
          type: 'texte',
          x: 100,
          y: 390,
          largeurBox: 594,
          hauteurBox: 90,
          rotation: 0,
          contenu: 'A suivi avec succès l’ensemble des modules théoriques et ateliers pratiques du programme :\n\n« {{intitule_formation}} »\n\nD’une durée totale de {{duree_heures}} heures de formation.',
          style: { fontSize: 12, color: '#334155', align: 'center' }
        },
        // Date et Lieu
        {
          id: 'cert-date-lieu',
          type: 'texte',
          x: 100,
          y: 530,
          largeurBox: 594,
          hauteurBox: 30,
          rotation: 0,
          contenu: 'Fait à {{lieu_delivrance}}, le {{date_delivrance}}',
          style: { fontSize: 11, italic: true, color: '#64748b', align: 'center' }
        },
        // Cartouche signature
        {
          id: 'cert-sign-label',
          type: 'texte',
          x: 440,
          y: 600,
          largeurBox: 240,
          hauteurBox: 35,
          rotation: 0,
          contenu: '{{signataire_nom}}\n{{signataire_titre}}',
          style: { fontSize: 11, bold: true, color: '#1e293b', align: 'center' }
        },
        {
          id: 'cert-signature',
          type: 'signature',
          x: 460,
          y: 645,
          largeurBox: 200,
          hauteurBox: 80,
          rotation: 0
        }
      ]
    }
  ]
};

