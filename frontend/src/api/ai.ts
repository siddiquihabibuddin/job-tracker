import { httpApps } from './client'

export interface InsightsResponse {
  insights: string[]
  generatedAt: string
}

export async function getAiInsights(windowDays = 30): Promise<InsightsResponse> {
  const res = await httpApps.post<InsightsResponse>('/ai/insights', { windowDays })
  return res.data
}
