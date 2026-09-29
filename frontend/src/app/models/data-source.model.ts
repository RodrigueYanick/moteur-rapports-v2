export type DataSourceType = 'POSTGRESQL' | 'MYSQL' | 'REST_API';
export type DataSourceAuthType = 'AUCUNE' | 'BASIC' | 'BEARER_TOKEN' | 'API_KEY';

export interface DataSourceConfig {
  id?: string;
  codeEntreprise?: string;
  nom: string;
  type: DataSourceType;
  urlOuHote: string;
  port?: number;
  nomBase?: string;
  nomUtilisateur?: string;
  motDePasse?: string;
  aMotDePasse?: boolean;
  enTetesJson?: string;
  methodeHttp?: string;
  authType?: DataSourceAuthType;
  apiKeyHeader?: string;
  timeoutSecondes?: number;
  actif: boolean;
  dateCreation?: string;
  dateModification?: string;
}

export interface DataSourceTestResult {
  succes: boolean;
  message: string;
  tempsReponseMs?: number;
}
