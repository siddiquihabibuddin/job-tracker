import { httpApps } from './client'
import type { UserTier } from '../auth/AuthContext'

export interface CheckoutRequest {
  cardNumber: string
  expMonth: number
  expYear: number
  cvc: string
  nameOnCard: string
}

export interface CheckoutResponse {
  token: string
  email: string
  userId: string
  displayName: string | null
  tier: UserTier
}

export interface BillingMeResponse {
  tier: UserTier
  tierUpdatedAt: string | null
}

export async function checkout(cardData: CheckoutRequest): Promise<CheckoutResponse> {
  try {
    const res = await httpApps.post<CheckoutResponse>('/billing/checkout', cardData)
    return res.data
  } catch (err: unknown) {
    // Extract the backend's validation message so the UI can show something useful.
    const axiosErr = err as { response?: { data?: Record<string, unknown> } }
    const body = axiosErr?.response?.data
    if (body) {
      // Map-based error: { error, details? }
      if (typeof body['error'] === 'string') {
        const detail = body['details'] ? ` — ${JSON.stringify(body['details'])}` : ''
        throw new Error(`${body['error']}${detail}`)
      }
      // ProblemDetail error: { title, detail? }
      if (typeof body['title'] === 'string') {
        const detail = typeof body['detail'] === 'string' ? `: ${body['detail']}` : ''
        throw new Error(`${body['title']}${detail}`)
      }
    }
    throw err
  }
}

export async function getBillingMe(): Promise<BillingMeResponse> {
  const res = await httpApps.get<BillingMeResponse>('/billing/me')
  return res.data
}

export interface BulkDeleteResponse {
  deleted: number
  skipped: string[]
}

export async function bulkDeleteApplications(ids: string[]): Promise<BulkDeleteResponse> {
  const res = await httpApps.post<BulkDeleteResponse>('/applications/bulk-delete', { ids })
  return res.data
}
