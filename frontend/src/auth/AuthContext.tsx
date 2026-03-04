import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { httpApps, registerUnauthorizedHandler } from '../api/client'

type User = { id: string; email: string; name?: string }
type AuthContextType = {
  user: User | null
  token: string | null
  signIn: (email: string, password: string) => Promise<void>
  signUp: (email: string, password: string, displayName?: string) => Promise<void>
  signOut: () => void
}

interface AuthApiResponse {
  token: string
  email: string
  userId: string
  displayName: string | null
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

function getStored<T>(key: string): T | null {
  try {
    const raw = sessionStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStored<User>('jt_user'))
  const [token, setToken] = useState<string | null>(() => sessionStorage.getItem('jt_token'))

  useEffect(() => {
    registerUnauthorizedHandler(() => persist(null, null))
  }, [])

  function persist(u: User | null, t: string | null) {
    setUser(u)
    setToken(t)
    if (u) sessionStorage.setItem('jt_user', JSON.stringify(u))
    else sessionStorage.removeItem('jt_user')
    if (t) sessionStorage.setItem('jt_token', t)
    else sessionStorage.removeItem('jt_token')
  }

  function handleAuthResponse(data: AuthApiResponse) {
    const name = data.displayName ?? data.email.split('@')[0]
    persist({ id: data.userId, email: data.email, name }, data.token)
  }

  const value = useMemo<AuthContextType>(() => ({
    user,
    token,
    async signIn(email: string, password: string) {
      const res = await httpApps.post<AuthApiResponse>('/auth/token', { email, password })
      handleAuthResponse(res.data)
    },
    async signUp(email: string, password: string, displayName?: string) {
      const res = await httpApps.post<AuthApiResponse>('/auth/register', { email, password, displayName })
      handleAuthResponse(res.data)
    },
    signOut() {
      persist(null, null)
    },
  }), [user, token])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within <AuthProvider>')
  return ctx
}
