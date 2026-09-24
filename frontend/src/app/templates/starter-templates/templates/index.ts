import { StarterTemplate } from '../starter-template.model';
import { factureCommercialeTemplate } from './facture-commerciale.template';
import { bonCommandeTemplate } from './bon-commande.template';
import { bulletinPaieTemplate } from './bulletin-paie.template';
import { rapportActiviteTemplate } from './rapport-activite.template';
import { certificatPresenceTemplate } from './certificat-presence.template';
import { rapportAvancementFacturationTemplate } from './rapport-avancement-facturation.template';

export * from './facture-commerciale.template';
export * from './bon-commande.template';
export * from './bulletin-paie.template';
export * from './rapport-activite.template';
export * from './certificat-presence.template';
export * from './rapport-avancement-facturation.template';

export const STARTER_TEMPLATES: StarterTemplate[] = [
  factureCommercialeTemplate,
  bonCommandeTemplate,
  bulletinPaieTemplate,
  rapportActiviteTemplate,
  certificatPresenceTemplate,
  rapportAvancementFacturationTemplate,
];

