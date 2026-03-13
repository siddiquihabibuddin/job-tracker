import { httpApps } from './client';

export interface CompanyInput {
  companyName: string;
  boardToken?: string | null;
  workdayTenant?: string | null;
  workdaySite?: string | null;
  workdayWdNum?: number | null;
}

export interface AlertCompany extends CompanyInput {
  lastErrorMessage: string | null;
  lastErrorAt: string | null;
  lastSuccessAt: string | null;
}

export interface JobAlert {
  id: string;
  userId: string;
  companies: AlertCompany[];
  roleKeywords: string;
  platforms: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface JobAlertMatch {
  id: string;
  alertId: string;
  platform: string;
  title: string;
  jobUrl: string | null;
  companyName: string;
  location: string | null;
  postedAt: string | null;
  seenAt: string | null;
  createdAt: string;
}

export interface CreateAlertInput {
  companies: CompanyInput[];
  roleKeywords: string;
  platforms: string[];
}

export const listAlerts = (): Promise<JobAlert[]> =>
  httpApps.get('/alerts').then(r => r.data);

export const createAlert = (input: CreateAlertInput): Promise<JobAlert> =>
  httpApps.post('/alerts', input).then(r => r.data);

export const updateAlert = (id: string, input: Partial<CreateAlertInput & { active: boolean }>): Promise<JobAlert> =>
  httpApps.patch(`/alerts/${id}`, input).then(r => r.data);

export const deleteAlert = (id: string): Promise<void> =>
  httpApps.delete(`/alerts/${id}`).then(() => undefined);

export const listMatchesForAlert = (alertId: string): Promise<JobAlertMatch[]> =>
  httpApps.get(`/alerts/${alertId}/matches`).then(r => r.data);

export const listUnseenMatches = (): Promise<JobAlertMatch[]> =>
  httpApps.get('/alerts/matches/unseen').then(r => r.data);

export const getUnseenCount = (): Promise<number> =>
  httpApps.get('/alerts/matches/unseen/count').then(r => r.data.count);

export const markSeen = (matchId: string): Promise<void> =>
  httpApps.post(`/alerts/matches/${matchId}/seen`).then(() => undefined);

export const markAllSeen = (alertId: string): Promise<void> =>
  httpApps.post(`/alerts/${alertId}/matches/seen-all`).then(() => undefined);

export const pollNow = (): Promise<void> =>
  httpApps.post('/alerts/poll').then(() => undefined);
