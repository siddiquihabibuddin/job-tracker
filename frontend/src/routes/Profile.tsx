import { useState } from 'react'
import { Link } from 'react-router-dom'
import { appsBase, statsBase } from '../api/client'
import { resyncStats } from '../api/applications'
import { useAuth } from '../auth/AuthContext'

export default function Profile() {
  const { user } = useAuth()
  const [syncing, setSyncing] = useState(false)
  const [syncDone, setSyncDone] = useState(false)

  const handleSync = () => {
    setSyncing(true); setSyncDone(false)
    resyncStats()
      .then(() => new Promise(res => setTimeout(res, 8000)))
      .then(() => setSyncDone(true))
      .catch(() => {})
      .finally(() => setSyncing(false))
  }

  const isPremium = user?.tier === 'PREMIUM'

  return (
    <>
      <h2>Profile</h2>

      {/* Account section with tier badge */}
      <article style={{ marginBottom: '1rem' }}>
        <header style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', flexWrap: 'wrap' }}>
          <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{user?.email ?? '—'}</span>
          {isPremium ? (
            <span style={{
              background: 'var(--pico-primary)',
              color: '#fff',
              fontSize: '0.68rem',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: '4px',
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
            }}>Pro</span>
          ) : (
            <span style={{
              background: 'var(--pico-muted-border-color)',
              color: 'var(--pico-muted-color)',
              fontSize: '0.68rem',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: '4px',
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
            }}>Free</span>
          )}
        </header>
        {isPremium ? (
          <p style={{ margin: '0.5rem 0 0', fontSize: '0.82rem', color: 'var(--pico-muted-color)' }}>
            <span
              title="Coming soon"
              style={{ color: 'var(--pico-muted-color)', cursor: 'default' }}
            >
              Manage subscription
            </span>
          </p>
        ) : (
          <p style={{ margin: '0.5rem 0 0', fontSize: '0.82rem' }}>
            <Link to="/upgrade">Upgrade to Pro →</Link>
          </p>
        )}
      </article>

      <form className="grid">
        <label>
          Timezone
          <input defaultValue="America/Phoenix" />
        </label>
        <label>
          Default Range
          <select defaultValue="30d">
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
          </select>
        </label>
        <button type="button" className="contrast">Save</button>
      </form>

      <article style={{ marginTop: '1rem' }}>
        <header>API Configuration</header>
        <p><strong>Applications API:</strong> {appsBase}</p>
        <p><strong>Stats API:</strong> {statsBase}</p>
      </article>

      <article style={{ marginTop: '1rem' }}>
        <header>Maintenance</header>
        <p style={{ fontSize: '0.82rem', color: 'var(--pico-muted-color)' }}>
          Re-syncs role categorisation in the stats snapshot. Only needed after a data migration.
        </p>
        <button
          type="button"
          className="secondary"
          style={{ width: 'auto' }}
          onClick={handleSync}
          disabled={syncing}
          aria-busy={syncing}
        >
          {syncing ? 'Syncing…' : syncDone ? 'Done' : 'Sync Role Stats'}
        </button>
      </article>
    </>
  )
}
