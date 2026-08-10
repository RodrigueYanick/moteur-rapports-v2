export interface GeneratedDocument {
  id: string;
  nom: string;
  templateId: string;
  templateNom?: string;
  donnees: any;
  dateCreation: string;
  dateModification: string;
}