export interface CompanyCertificate {
  id: string;
  codeEntreprise: string;
  alias: string;
  filename: string;
  validFrom?: string;
  validTo?: string;
  active: boolean;
  isExpired?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CertificateUploadRequest {
  alias: string;
  password?: string;
}
