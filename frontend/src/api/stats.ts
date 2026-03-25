import { httpStats } from './client'

export interface StatsSummary {
  windowDays: number
  totalApplied: number
  byStatus: Record<string, number>
  bySource: Record<string, number>
  generatedAt: string
}

export interface TrendPoint {
  start: string
  end: string
  count: number
  callsCount: number
}

export async function getSummary(window: 7 | 30): Promise<StatsSummary> {
  const res = await httpStats.get<StatsSummary>(`/stats/summary`, { params: { window: `${window}d` } })
  return res.data
}

export async function getTrend(period = 'week', weeks = 12): Promise<TrendPoint[]> {
  const res = await httpStats.get<{ period: string; points: TrendPoint[] }>(`/stats/trend`, { params: { period, weeks } })
  return res.data.points
}

export interface BreakdownRow {
  label: string
  periodNum: number
  totalApplied: number
  totalRejected: number
  totalOpen: number
}

export interface OpenWindows {
  today: number
  yesterday: number
  last7d: number
  last15d: number
  last30d: number
  last3m: number
  last6m: number
  last9m: number
  last1y: number
}

export interface BreakdownResponse {
  groupBy: 'month' | 'year'
  year: number | null
  rows: BreakdownRow[]
  openWindows: OpenWindows
}

export async function getBreakdown(groupBy: 'month' | 'year', year?: number, tz?: string): Promise<BreakdownResponse> {
  const params: Record<string, string> = { groupBy }
  if (year !== undefined) params.year = String(year)
  if (tz) params.tz = tz
  const res = await httpStats.get<BreakdownResponse>('/stats/breakdown', { params })
  return res.data
}

export interface RoleCountRow {
  label: string
  periodNum: number
  engineerDev: number
  manager: number
  architect: number
  other: number
}

export interface RoleCountsResponse {
  groupBy: 'month' | 'year'
  year: number | null
  rows: RoleCountRow[]
}

export async function getRoleCounts(groupBy: 'month' | 'year', year?: number): Promise<RoleCountsResponse> {
  const params: Record<string, string> = { groupBy }
  if (year !== undefined) params.year = String(year)
  const res = await httpStats.get<RoleCountsResponse>('/stats/roles', { params })
  return res.data
}

export interface InsightsResponse {
  insights: string[]
  generatedAt: string
}

export async function getInsights(): Promise<InsightsResponse> {
  const res = await httpStats.get<InsightsResponse>('/stats/insights')
  return res.data
}

export interface ActivityItem {
  id: string
  eventType: string
  message: string
  occurredAt: string
}

export async function getActivityFeed(appId: string): Promise<ActivityItem[]> {
  const res = await httpStats.get<ActivityItem[]>(`/stats/activity/${appId}`)
  return res.data
}

export interface StaleApp {
  appId: string
  company: string
  role: string
  status: string
  daysSinceLastEvent: number
  flaggedAt: string
  appliedAt?: string
}

export async function getStaleApps(): Promise<StaleApp[]> {
  const res = await httpStats.get<StaleApp[]>('/stats/stale')
  return res.data
}

export interface CompanyCount {
  company: string
  count: number
  lastAppliedAt?: string
}

export async function getTopCompanies(): Promise<CompanyCount[]> {
  const { data } = await httpStats.get<CompanyCount[]>('/stats/companies')
  return data
}