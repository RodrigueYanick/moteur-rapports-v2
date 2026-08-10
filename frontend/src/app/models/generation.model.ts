export interface Generation {
  id: string;
  templateId: string;
  dateGeneration: string;
  donneesRecues: any;
  statutGeneration: 'EN_COURS' | 'SUCCES' | 'ECHEC';
  urlFichierGenere?: string;
  dateCreation: string;
  dateModification: string;
}
