import { useAuth } from './AuthContext'

export function usePremium(): boolean {
  const { user } = useAuth()
  return user?.tier === 'PREMIUM'
}
