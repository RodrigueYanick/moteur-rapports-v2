import { User, AccountType, TargetPersona } from './user.model';

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface RegisterRequest {
  email: string;
  motDePasse: string;
  nom: string;
  prenom?: string;
  accountType?: AccountType;
  targetPersona?: TargetPersona;
  codeEntreprise?: string;
  nomEntreprise?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface SocialLoginRequest {
  provider: 'GOOGLE' | 'GITHUB' | 'MICROSOFT';
  email: string;
  socialId?: string;
  nom?: string;
  prenom?: string;
  avatarUrl?: string;
  idToken?: string;
}

export interface EnterpriseUpgradeRequest {
  nomEntreprise: string;
  codeEntreprise?: string;
  siret?: string;
  adresse?: string;
  telephone?: string;
  emailContact?: string;
}
