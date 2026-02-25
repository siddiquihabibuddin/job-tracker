import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { httpApps, registerUnauthorizedHandler } from '../api/client'

type User = { id: string; email: string; name?: string }
type AuthContextType = {
  user: User | null
  token: string | null
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => void
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

  const value = useMemo<AuthContextType>(() => ({
    user,
    token,
    async signIn(email: string, password: string) {
      const res = await httpApps.post<{ token: string; email: string; userId: string }>(
        '/auth/token',
        { email, password }
      )
      const { token: t, email: e, userId } = res.data
      persist({ id: userId, email: e, name: e.split('@')[0] }, t)
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
