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

