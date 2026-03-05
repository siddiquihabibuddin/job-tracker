import { useState } from 'react'
import { appsBase, statsBase } from '../api/client'
import { resyncStats } from '../api/applications'

export default function Profile() {
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

  return (
    <>
      <h2>Profile</h2>
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
