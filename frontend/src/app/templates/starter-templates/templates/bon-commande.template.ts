import { StarterTemplate } from '../starter-template.model';

export const bonCommandeTemplate: StarterTemplate = {
  id: 'bon-commande',
  nom: 'Reçu / Bon de commande',
  description: 'Bon de commande d’achat et reçu avec détails de livraison, liste des articles commandés, mode de paiement et signature.',
  categorie: 'ACHATS',
  iconName: 'shopping-cart',
  couleurTag: '#059669',
  badge: 'Commerce',
  modePagination: 'FIXED',
  variables: [
    { nomVariable: 'numero_commande', type: 'STRING', obligatoire: true, description: 'Numéro de commande (ex: BDC-2026-88)' },
    { nomVariable: 'date_commande', type: 'DATE', obligatoire: true, description: 'Date de passation de la commande' },
    { nomVariable: 'fournisseur_nom', type: 'STRING', obligatoire: true, description: 'Nom du fournisseur ou prestataire' },
    { nomVariable: 'fournisseur_contact', type: 'STRING', obligatoire: false, description: 'Contact et email du fournisseur' },
    { nomVariable: 'client_nom', type: 'STRING', obligatoire: true, description: 'Nom de l’acheteur / commanditaire' },
    { nomVariable: 'adresse_livraison', type: 'STRING', obligatoire: true, description: 'Adresse complète de livraison' },
    { nomVariable: 'date_livraison_souhaitee', type: 'DATE', obligatoire: false, description: 'Date de livraison souhaitée' },
    { nomVariable: 'mode_paiement', type: 'STRING', obligatoire: false, description: 'Mode de règlement (ex: Carte bancaire, Virement 30j)' },
    { nomVariable: 'total_commande', type: 'FLOAT', obligatoire: true, description: 'Montant total TTC de la commande (€)' },
    { nomVariable: 'instructions_livraison', type: 'STRING', obligatoire: false, description: 'Instructions spécifiques de réception' },
  ],
  pages: [
    {
      id: 'bdc-page-1',
      nom: 'Page 1',
      blocks: [
        // Bandeau supérieur vert émeraude
        {
          id: 'bdc-bandeau',
          type: 'rectangle',
          x: 38,
          y: 38,
          largeurBox: 718,
          hauteurBox: 6,
          rotation: 0,
          style: { fill: '#059669', epaisseur: 0 }
        },
        // Titre
        {
          id: 'bdc-titre',
          type: 'titre',
          x: 38,
          y: 58,
          largeurBox: 420,
          hauteurBox: 40,
          rotation: 0,
          contenu: 'BON DE COMMANDE',
          style: { fontSize: 26, bold: true, color: '#064e3b' }
        },
        {
          id: 'bdc-meta',
          type: 'texte',
          x: 38,
          y: 102,
          largeurBox: 420,
          hauteurBox: 22,
          rotation: 0,
          contenu: 'Commande N° {{numero_commande}} · Date : {{date_commande}}',
          style: { fontSize: 11, color: '#047857' }
        },
        // Cartouche Fournisseur
        {
          id: 'bdc-bg-fournisseur',
          type: 'rectangle',
          x: 38,
          y: 136,
          largeurBox: 340,
          hauteurBox: 96,
          rotation: 0,
          style: { fill: '#f0fdf4', couleur: '#bbf7d0', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'bdc-fournisseur-label',
          type: 'texte',
          x: 50,
          y: 146,
          largeurBox: 316,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'FOURNISSEUR :',
          style: { fontSize: 10, bold: true, color: '#15803d' }
        },
        {
          id: 'bdc-fournisseur-nom',
          type: 'titre',
          x: 50,
          y: 166,
          largeurBox: 316,
          hauteurBox: 22,
          rotation: 0,
          contenu: '{{fournisseur_nom}}',
          style: { fontSize: 13, bold: true, color: '#0f172a' }
        },
        {
          id: 'bdc-fournisseur-details',
          type: 'texte',
          x: 50,
          y: 190,
          largeurBox: 316,
          hauteurBox: 36,
          rotation: 0,
          contenu: 'Contact : {{fournisseur_contact}}',
          style: { fontSize: 11, color: '#475569' }
        },
        // Cartouche Livraison
        {
          id: 'bdc-bg-livraison',
          type: 'rectangle',
          x: 416,
          y: 136,
          largeurBox: 340,
          hauteurBox: 96,
          rotation: 0,
          style: { fill: '#f8fafc', couleur: '#e2e8f0', epaisseur: 1, borderRadius: 6 }
        },
        {
          id: 'bdc-livraison-label',
          type: 'texte',
          x: 430,
          y: 146,
          largeurBox: 312,
          hauteurBox: 18,
          rotation: 0,
          contenu: 'ADRESSE DE LIVRAISON :',
          style: { fontSize: 10, bold: true, color: '#64748b' }
        },
        {
          id: 'bdc-client-nom',
          type: 'titre',
          x: 430,
          y: 166,
          largeurBox: 312,
          hauteurBox: 22,
          rotation: 0,
          contenu: '{{client_nom}}',
          style: { fontSize: 13, bold: true, color: '#0f172a' }
        },
        {
          id: 'bdc-adresse-livraison',
          type: 'texte',
          x: 430,
          y: 190,
          largeurBox: 312,
          hauteurBox: 36,
          rotation: 0,
          contenu: '{{adresse_livraison}}',
          style: { fontSize: 11, color: '#475569' }
        },
        // Tableau articles
        {
          id: 'bdc-tableau',
          type: 'tableau',
          x: 38,
          y: 256,
          largeurBox: 718,
          hauteurBox: 170,
          rotation: 0,
          lignes: [
            [
              { value: 'Référence', bgColor: '#ecfdf5', textColor: '#065f46' },
              { value: 'Description de l’article', bgColor: '#ecfdf5', textColor: '#065f46' },
              { value: 'Quantité', bgColor: '#ecfdf5', textColor: '#065f46' },
              { value: 'Prix Unitaire', bgColor: '#ecfdf5', textColor: '#065f46' },
              { value: 'Total', bgColor: '#ecfdf5', textColor: '#065f46' }
            ],
            [
              { value: 'REF-8012' },
              { value: 'Serveur de calcul haute densité 64 cœurs' },
              { value: '2' },
              { value: '3 400,00 €' },
              { value: '6 800,00 €' }
            ],
            [
              { value: 'REF-9204' },
              { value: 'Commutateur réseau manageable 10GbE' },
              { value: '4' },
              { value: '750,00 €' },
              { value: '3 000,00 €' }
            ],
            [
              { value: 'REF-1044' },
              { value: 'Module optique SFP+ multimode 10G' },
              { value: '16' },
              { value: '45,00 €' },
              { value: '720,00 €' }
            ]
          ],
          style: { bordureCouleur: '#d1fae5', texteCouleurDefaut: '#334155' }
        },
        // Informations complémentaires
        {
          id: 'bdc-infos',
          type: 'texte',
          x: 38,
          y: 450,
          largeurBox: 400,
          hauteurBox: 60,
          rotation: 0,
          contenu: 'Date de livraison souhaitée : {{date_livraison_souhaitee}}\nMode de règlement : {{mode_paiement}}\nInstructions : {{instructions_livraison}}',
          style: { fontSize: 11, color: '#475569' }
        },
        // Total
        {
          id: 'bdc-bg-total',
          type: 'rectangle',
          x: 476,
          y: 450,
          largeurBox: 280,
          hauteurBox: 50,
          rotation: 0,
          style: { fill: '#059669', epaisseur: 0, borderRadius: 4 }
        },
        {
          id: 'bdc-total-txt',
          type: 'titre',
          x: 486,
          y: 462,
          largeurBox: 260,
          hauteurBox: 26,
          rotation: 0,
          contenu: 'TOTAL COMMANDE : {{total_commande}} €',
          style: { fontSize: 13, bold: true, color: '#ffffff', align: 'center' }
        },
        // Signature
        {
          id: 'bdc-sign-label',
          type: 'texte',
          x: 476,
          y: 530,
          largeurBox: 280,
          hauteurBox: 20,
          rotation: 0,
          contenu: 'Bon pour accord & signature client :',
          style: { fontSize: 10, bold: true, color: '#64748b' }
        },
        {
          id: 'bdc-signature',
          type: 'signature',
          x: 476,
          y: 555,
          largeurBox: 280,
          hauteurBox: 75,
          rotation: 0
        }
      ]
    }
  ]
};

