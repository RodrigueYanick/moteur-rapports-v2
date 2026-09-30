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

export interface PdfProtectionOptions {
  userPassword?: string;
  ownerPassword?: string;
  allowPrinting?: boolean;
  allowCopying?: boolean;
  allowModification?: boolean;
}

export interface PdfSignatureOptions {
  certificateId: string;
  reason?: string;
  location?: string;
  contactInfo?: string;
}

export interface ReportGenerationPayload {
  data: Record<string, any>;
  protection?: PdfProtectionOptions;
  signature?: PdfSignatureOptions;
}

