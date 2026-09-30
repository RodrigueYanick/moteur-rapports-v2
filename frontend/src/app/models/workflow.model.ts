export type TemplateWorkflowStatus = 'BROUILLON' | 'EN_REVUE' | 'APPROUVE' | 'PUBLIE' | 'ARCHIVE';

export interface TemplateWorkflowHistory {
  id: string;
  templateId: string;
  codeEntreprise: string;
  fromStatus: TemplateWorkflowStatus;
  toStatus: TemplateWorkflowStatus;
  performedBy: string;
  comment?: string;
  timestamp: string;
}

export interface WorkflowActionRequest {
  comment?: string;
}
