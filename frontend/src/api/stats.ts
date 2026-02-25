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
}

export async function getSummary(window: 7 | 30): Promise<StatsSummary> {
  const res = await httpStats.get<StatsSummary>(`/stats/summary`, { params: { window: `${window}d` } })
  return res.data
}

export async function getTrend(weeks = 12): Promise<TrendPoint[]> {
  const res = await httpStats.get<{ period: string; points: TrendPoint[] }>(`/stats/trend`, { params: { period: 'week', weeks } })
  return res.data.points
}