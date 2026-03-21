import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { StaleApp } from '../../api/stats'
import { formatDate } from '../../utils/formatDate'

const GHOST_PAGE_SIZE = 8

interface GhostingTrackerProps {
  staleApps: StaleApp[] | undefined
}

export default function GhostingTracker({ staleApps }: GhostingTrackerProps) {
  const [ghostPage, setGhostPage] = useState(0)

  if (!staleApps || staleApps.length === 0) return null

  const ghostTotalPages = Math.ceil(staleApps.length / GHOST_PAGE_SIZE)
  const ghostPagedApps = staleApps.slice(ghostPage * GHOST_PAGE_SIZE, (ghostPage + 1) * GHOST_PAGE_SIZE)

  return (
    <article style={{ padding: '0.65rem', marginBottom: 0, borderLeft: '3px solid #f59e0b' }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
        <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>Possibly Ghosting ({staleApps.length})</span>
        <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem' }}>No updates in 14+ days</small>
      </header>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {ghostPagedApps.map(app => (
          <li key={app.appId} style={{ padding: '0.3rem 0', borderBottom: '1px solid var(--pico-muted-border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Link to={`/applications/${app.appId}`} style={{ fontSize: '0.78rem', fontWeight: 500 }}>
              {app.company} — {app.role}
            </Link>
            <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem', whiteSpace: 'nowrap', marginLeft: '0.75rem' }}>
              {app.status} · {app.appliedAt ? formatDate(app.appliedAt) : '—'} · {app.daysSinceLastEvent}d ago
            </small>
          </li>
        ))}
      </ul>
      {ghostTotalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.6rem' }}>
          <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem' }}>
            {ghostPage * GHOST_PAGE_SIZE + 1}–{Math.min((ghostPage + 1) * GHOST_PAGE_SIZE, staleApps.length)} of {staleApps.length}
          </small>
          <div style={{ display: 'flex', gap: '0.25rem' }}>
            <button
              style={{ padding: '2px 8px', fontSize: '0.75rem', marginBottom: 0 }}
              disabled={ghostPage === 0}
              onClick={() => setGhostPage(p => p - 1)}
            >‹ Prev</button>
            <button
              style={{ padding: '2px 8px', fontSize: '0.75rem', marginBottom: 0 }}
              disabled={ghostPage >= ghostTotalPages - 1}
              onClick={() => setGhostPage(p => p + 1)}
            >Next ›</button>
          </div>
        </div>
      )}
    </article>
  )
}
