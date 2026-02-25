import { sleep } from './applications'

type StatusKey = 'APPLIED' | 'PHONE' | 'ONSITE' | 'OFFER' | 'REJECTED'
type SourceKey = 'LinkedIn' | 'Referral' | 'Company' | 'Indeed' | 'Other'

export interface StatsSummary {
  windowDays: number
  totalApplied: number
  byStatus: Record<StatusKey, number>
  bySource: Record<SourceKey, number>
  generatedAt: string
}

export interface TrendPoint {
  start: string // yyyy-mm-dd
  end: string   // yyyy-mm-dd
  count: number
}

export async function fetchMockSummary(windowDays: 7 | 30): Promise<StatsSummary> {
  await sleep(250)
  // simple fake numbers
  const base = windowDays === 7 ? 6 : 24
  const byStatus = { APPLIED: base, PHONE: Math.floor(base/2), ONSITE: Math.floor(base/4), OFFER: 1, REJECTED: 2 }
  const bySource = { LinkedIn: Math.floor(base*0.45) as number, Referral: Math.floor(base*0.2), Company: Math.floor(base*0.2), Indeed: Math.floor(base*0.1), Other: base - (Math.floor(base*0.45)+Math.floor(base*0.2)+Math.floor(base*0.2)+Math.floor(base*0.1)) }
  return {
    windowDays,
    totalApplied: base,
    byStatus,
    bySource,
    generatedAt: new Date().toISOString(),
  }
}

export async function fetchMockTrend(weeks = 12): Promise<TrendPoint[]> {
  await sleep(250)
  const today = new Date()
  const points: TrendPoint[] = []
  for (let i = weeks - 1; i >= 0; i--) {
    const end = new Date(today)
    end.setDate(end.getDate() - (i * 7))
    const start = new Date(end)
    start.setDate(end.getDate() - 6)
    points.push({
      start: start.toISOString().slice(0,10),
      end: end.toISOString().slice(0,10),
      count: Math.max(0, Math.floor(3 + 4*Math.sin(i/2))), // a little wave
    })
  }
  return points
}