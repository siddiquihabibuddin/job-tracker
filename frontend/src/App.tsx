import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import './index.css'
import { useAuth } from './auth/AuthContext'

export default function App() {
  const { user, signOut } = useAuth()
  const nav = useNavigate()

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
                <li><NavLink to="/profile">Profile</NavLink></li>
                <li><small>{user.email}</small></li>
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