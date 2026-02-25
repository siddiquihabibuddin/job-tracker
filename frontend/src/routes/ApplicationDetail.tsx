import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { fetchMockApplicationById, updateMockApplication, type Application } from '../mocks/applications'
import { getApplicationById as apiGet, patchApplication as apiPatch, type AppStatus } from '../api/applications'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const STATUSES: AppStatus[] = ['APPLIED','PHONE','ONSITE','OFFER','REJECTED']

export default function ApplicationDetail() {
  const { id } = useParams()
  const [app, setApp] = useState<Application | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    async function load() {
      if (!id) return
      setLoading(true); setError(null)
      try {
        const data = USE_MOCK ? await fetchMockApplicationById(id) : await apiGet(id)
        if (alive) setApp(data)
      } catch (e: any) {
        if (alive) setError(e?.message || 'Failed to load')
      } finally {
        if (alive) setLoading(false)
      }
    }
    load()
    return () => { alive = false }
  }, [id])

  async function onChangeStatus(next: AppStatus) {
    if (!app || !id) return
    setSaving(true)
    const prev = app
    // optimistic update
    setApp({ ...app, status: next })
    try {
      const updated = USE_MOCK ? await updateMockApplication(id, { status: next }) : await apiPatch(id, { status: next })
      setApp(updated)
      setToast('Status updated')
      setTimeout(() => setToast(null), 1500)
    } catch (e: any) {
      // rollback
      setApp(prev)
      setError(e?.message || 'Failed to update status')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <article><header>Loading</header><p>Fetching application…</p></article>
  if (error) return (
    <article>
      <header>Error</header>
      <p>{error}</p>
      <p><Link to="/applications"><button>Back</button></Link></p>
    </article>
  )
  if (!app) return (
    <article>
      <header>Not found</header>
      <p>No application with id <code>{id}</code>.</p>
      <p><Link to="/applications"><button>Back</button></Link></p>
    </article>
  )

  return (
    <>
      <header className="grid">
        <h2>{app.company} — {app.role}</h2>
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
          <Link to="/applications"><button>Back</button></Link>
        </div>
      </header>

      {toast && <article><header>Success</header><p>{toast}</p></article>}

      <div className="grid">
        <article>
          <header>Overview</header>
          <p><strong>Status:</strong>{' '}
            <select value={app.status} onChange={e => onChangeStatus(e.target.value as AppStatus)} disabled={saving}>
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </p>
          <p><strong>Source:</strong> {app.source}</p>
          <p><strong>Created:</strong> {new Date(app.createdAt).toLocaleString()}</p>
          {saving && <small>Saving…</small>}
        </article>
        <article>
          <header>Notes</header>
          <p>Notes placeholder</p>
        </article>
      </div>
    </>
  )
}