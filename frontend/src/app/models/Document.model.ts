export interface GeneratedDocument {
  id: string;
  nom: string;
  templateId: string;
  templateNom?: string;
  donnees: any;
  dateCreation: string;
  dateModification: string;
  shareToken?: string;
  isPublicShared?: boolean;
  statut?: string;
  codeEntreprise?: string;
}