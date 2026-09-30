export interface AuditLog {
  id: string;
  codeEntreprise: string;
  action: string;
  entityName?: string;
  entityId?: string;
  performedBy: string;
  ipAddress?: string;
  userAgent?: string;
  details?: string;
  timestamp: string;
}

export interface AuditStats {
  totalEvents: number;
  totalByAction: Record<string, number>;
  totalByEntity: Record<string, number>;
  activeUsersCount: number;
  recentAlertsCount: number;
}

export interface AuditSearchFilter {
  action?: string;
  entityName?: string;
  performedBy?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}
