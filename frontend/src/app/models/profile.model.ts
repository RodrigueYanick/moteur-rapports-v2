export interface PersonaProfileDto {
  personalInfo: {
    nom?: string;
    prenom?: string;
    email?: string;
    telephone?: string;
    adresse?: string;
    ville?: string;
    codePostal?: string;
    pays?: string;
    titreProfessionnel?: string;
    bioSommaire?: string;
    siteWeb?: string;
    linkedin?: string;
    github?: string;
    [key: string]: any;
  };
  experiences: ExperienceItem[];
  formations: FormationItem[];
  competences: {
    techniques?: string[];
    outils?: string[];
    langues?: string[];
    certifications?: string[];
    [key: string]: any;
  };
  preferences?: Record<string, any>;
  dateModification?: string;
}

export interface ExperienceItem {
  id?: string;
  titre: string;
  entreprise: string;
  ville?: string;
  dateDebut: string;
  dateFin?: string;
  enCours?: boolean;
  description: string;
  realisations?: string[];
}

export interface FormationItem {
  id?: string;
  diplome: string;
  etablissement: string;
  ville?: string;
  anneeObtention: string;
  details?: string;
}

export interface PublicDocumentViewDto {
  documentId: string;
  nom: string;
  templateNom: string;
  formatPapier: string;
  dateCreation: string;
  shareToken: string;
  shareUrl: string;
  qrCodeDataUri: string;
  htmlContent: string;
}
