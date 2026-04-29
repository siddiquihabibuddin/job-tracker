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
  const res = await httpApps.post<CheckoutResponse>('/billing/checkout', cardData)
  return res.data
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
