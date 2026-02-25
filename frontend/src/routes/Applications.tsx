import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  listApplications as apiList,
  deleteApplication as apiDelete,
  type Application,
  type AppStatus,
} from '../api/applications'
import {
  fetchMockApplications as mockList,
  deleteMockApplication as mockDelete,
} from '../mocks/applications'
import { useDebounce } from '../hooks/useDebounce'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const STATUSES: (AppStatus | 'ALL')[] = ['ALL', 'APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED']

export default function Applications() {
  const [params, setParams] = useSearchParams()

  // read from URL (defaults)
  const statusParam = (params.get('status') || 'ALL').toUpperCase()
  const searchParam = params.get('search') || ''

  const [status, setStatus] = useState<AppStatus | 'ALL'>(isValidStatus(statusParam) ? (statusParam as any) : 'ALL')
  const [search, setSearch] = useState(searchParam)

  // write to URL when filters change
  useEffect(() => {
    const next = new URLSearchParams()
    if (status !== 'ALL') next.set('status', status)
    if (search.trim()) next.set('search', search.trim())
    setParams(next, { replace: true })
  }, [status, search, setParams])

  const debouncedSearch = useDebounce(search, 300)

  const [rows, setRows] = useState<Application[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [toast, setToast] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        if (USE_MOCK) {
          const data = await mockList({ status, search: debouncedSearch })
          if (alive) setRows(data)
        } else {
          const { items } = await apiList({ status, search: debouncedSearch, limit: 50 })
          if (alive) setRows(items)
        }
      } catch (e: any) {
        if (alive) setError(e?.message || 'Failed to load')
      } finally {
        if (alive) setLoading(false)
      }
    }
    load()
    return () => {
      alive = false
    }
  }, [status, debouncedSearch])

  async function handleDelete(id: string, label: string) {
    setDeletingId(id)
    try {
      if (USE_MOCK) await mockDelete(id)
      else await apiDelete(id)
      setRows(prev => prev.filter(r => r.id !== id))
      setToast(`Deleted: ${label}`)
      setTimeout(() => setToast(null), 1500)
    } catch (e: any) {
      alert(e?.message || 'Failed to delete')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <>
      <header className="grid">
        <h2>Applications</h2>
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center' }}>
          <Link to="/applications/new">
            <button className="contrast">New Application</button>
          </Link>
        </div>
      </header>

      <details open>
        <summary>Filters</summary>
        <div className="grid">
          <label>
            Status
            <select value={status} onChange={e => setStatus(e.target.value as any)}>
              {STATUSES.map(s => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </label>
          <label>
            Search
            <input
              placeholder="Company or role"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </label>
          <div style={{ display: 'flex', alignItems: 'end', gap: '.5rem' }}>
            <button
              type="button"
              onClick={() => {
                setStatus('ALL')
                setSearch('')
              }}
            >
              Clear
            </button>
            <small>
              Source: <code>{USE_MOCK ? 'mock' : 'API'}</code>
            </small>
          </div>
        </div>
      </details>

      {toast && (
        <article>
          <header>Success</header>
          <p>{toast}</p>
        </article>
      )}

      {loading && (
        <article>
          <header>Loading</header>
          <p>Please wait…</p>
        </article>
      )}
      {error && (
        <article>
          <header>Error</header>
          <p>{error}</p>
        </article>
      )}

      {!loading && !error && (
        <table>
          <thead>
            <tr>
              <th>Company</th>
              <th>Role</th>
              <th>Status</th>
              <th>Source</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map(r => (
              <tr key={r.id}>
                <td>{r.company}</td>
                <td>{r.role}</td>
                <td>{r.status}</td>
                <td>{r.source}</td>
                <td>{new Date(r.createdAt).toLocaleDateString()}</td>
                <td>
                  <Link to={`/applications/${r.id}`}>
                    <button>View</button>
                  </Link>{' '}
                  <button
                    className="secondary"
                    onClick={() => {
                      if (confirm(`Delete "${r.company} — ${r.role}"?`)) {
                        handleDelete(r.id, `${r.company} — ${r.role}`)
                      }
                    }}
                    disabled={deletingId === r.id}
                  >
                    {deletingId === r.id ? 'Deleting…' : 'Delete'}
                  </button>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={6}>
                  <em>No results</em>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </>
  )
}

function isValidStatus(s: string) {
  return STATUSES.includes(s as any)
}