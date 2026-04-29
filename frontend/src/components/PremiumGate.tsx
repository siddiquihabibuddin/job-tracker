import type { ReactNode } from 'react'
import { usePremium } from '../auth/usePremium'
import UpsellCard from './UpsellCard'

interface PremiumGateProps {
  children: ReactNode
  /** Rendered instead of the default UpsellCard when mode="upsell" and no custom fallback is provided. */
  fallback?: ReactNode
  /**
   * - "upsell" (default): free users see <UpsellCard /> (or fallback if provided)
   * - "hide": free users see nothing (or fallback if provided)
   *
   * NOTE: This is UX-only client-side gating. The server-side PremiumGuard on
   * every premium controller method is the real enforcement boundary.
   * A free user can still invoke premium API endpoints directly; the server
   * will reject with HTTP 402 premium_required.
   */
  mode?: 'hide' | 'upsell'
}

export default function PremiumGate({ children, fallback, mode = 'upsell' }: PremiumGateProps) {
  const isPremium = usePremium()

  if (isPremium) return <>{children}</>

  if (fallback !== undefined) return <>{fallback}</>

  if (mode === 'upsell') return <UpsellCard />

  return null
}
