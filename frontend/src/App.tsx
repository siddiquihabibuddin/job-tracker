import { useEffect } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import './index.css'
import { useAuth } from './auth/AuthContext'
import { useQuery } from '@tanstack/react-query'
import { getUnseenCount } from './api/alerts'
import { registerPaymentRequiredHandler } from './api/client'

export default function App() {
  const { user, signOut } = useAuth()
  const nav = useNavigate()

  useEffect(() => {
    registerPaymentRequiredHandler(() => nav('/upgrade'))
  }, [nav])

  const { data: unseenCount = 0 } = useQuery({
    queryKey: ['unseen-count'],
    queryFn: getUnseenCount,
    refetchInterval: 5 * 60 * 1000,
    enabled: !!user,
  })

  return (
    <div className="app">
      <header className="container-fluid">
        <nav>
          <ul>
            <li><strong>Job Tracker</strong></li>
          </ul>
          <ul>
            {user ? (
              <>
                <li><NavLink to="/dashboard">Dashboard</NavLink></li>
                <li><NavLink to="/applications">Applications</NavLink></li>
                <li>
                  <NavLink to="/alerts">
                    Alerts{unseenCount > 0 && (
                      <span style={{
                        marginLeft: '0.35rem',
                        background: '#2563eb',
                        color: '#fff',
                        borderRadius: '9999px',
                        fontSize: '0.65rem',
                        padding: '0.1rem 0.4rem',
                        fontWeight: 700,
                        verticalAlign: 'middle',
                      }}>{unseenCount}</span>
                    )}
                  </NavLink>
                </li>
                <li><NavLink to="/profile">Profile</NavLink></li>
                <li>
                  <small style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                    {user.email}
                    {user.tier === 'PREMIUM' ? (
                      <span style={{
                        background: 'var(--pico-primary)',
                        color: '#fff',
                        fontSize: '0.62rem',
                        fontWeight: 700,
                        padding: '1px 6px',
                        borderRadius: '4px',
                        letterSpacing: '0.04em',
                        textTransform: 'uppercase',
                      }}>Pro</span>
                    ) : (
                      <NavLink
                        to="/upgrade"
                        style={{
                          background: 'var(--pico-muted-border-color)',
                          color: 'var(--pico-muted-color)',
                          fontSize: '0.62rem',
                          fontWeight: 700,
                          padding: '1px 6px',
                          borderRadius: '4px',
                          letterSpacing: '0.04em',
                          textTransform: 'uppercase',
                          textDecoration: 'none',
                        }}
                        title="Upgrade to Pro"
                      >Free</NavLink>
                    )}
                  </small>
                </li>
                <li>
                  <button
                    onClick={() => { signOut(); nav('/login', { replace: true }) }}
                  >
                    Logout
                  </button>
                </li>
              </>
            ) : (
              <li><NavLink to="/login">Login</NavLink></li>
            )}
          </ul>
        </nav>
      </header>

      <main className="container">
        <Outlet />
      </main>

      <footer className="container">
        <small>© {new Date().getFullYear()} Job Tracker</small>
      </footer>
    </div>
  )
}