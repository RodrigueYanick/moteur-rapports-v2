import { DesignPage } from '../../designer/models/design-block.model';
import { Variable } from '../../models/variable.model';

export interface StarterTemplate {
  id: string;
  nom: string;
  description: string;
  categorie: 'VENTES' | 'ACHATS' | 'RH' | 'ADMINISTRATION' | 'AUTRES';
  iconName: string;
  couleurTag: string;
  badge: string;
  modePagination: 'FIXED' | 'AUTO';
  variables: Omit<Variable, 'id'>[];
  pages: DesignPage[];
}

