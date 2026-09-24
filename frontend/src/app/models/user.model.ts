export type Role = 'SUPER_ADMIN' | 'ADMIN_ENTREPRISE' | 'DESIGNER' | 'OPERATOR';

export interface Entreprise {
  id: string;
  code: string;
  nom: string;
  emailContact?: string;
  actif: boolean;
}

export interface User {
  id: string;
  email: string;
  nom: string;
  prenom?: string;
  nomComplet: string;
  role: Role;
  codeEntreprise?: string;
  nomEntreprise?: string;
}

