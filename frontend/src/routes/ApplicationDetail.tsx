import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { fetchMockApplicationById, updateMockApplication } from '../mocks/applications'
import {
  getApplicationById as apiGet,
  patchApplication as apiPatch,
  type Application,
  type AppStatus,
} from '../api/applications'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const STATUSES: AppStatus[] = ['APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED', 'ACCEPTED', 'WITHDRAWN']

function formatDate(iso: string | undefined): string | null {
  if (!iso) return null
  try {
    return new Date(iso).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
  } catch {
    return iso
  }
}

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
      setLoading(true)
      setError(null)
      try {
        // fetchMockApplicationById returns the mock Application shape which is a
        // structural subset of the API Application type — compatible at runtime.
        const data = USE_MOCK
          ? (await fetchMockApplicationById(id)) as Application | null
          : await apiGet(id)
        if (alive) setApp(data)
      } catch (e: unknown) {
        if (alive) setError(e instanceof Error ? e.message : 'Failed to load')
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
      const updated = USE_MOCK
        ? (await updateMockApplication(id, { status: next })) as Application
        : await apiPatch(id, { status: next })
      setApp(updated)
      setToast('Status updated')
      setTimeout(() => setToast(null), 1500)
    } catch (e: unknown) {
      // rollback
      setApp(prev)
      setError(e instanceof Error ? e.message : 'Failed to update status')
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

  const salaryDisplay = (() => {
    const hasSalary = app.salaryMin != null || app.salaryMax != null
    if (!hasSalary) return null
    const currency = app.currency || 'USD'
    if (app.salaryMin != null && app.salaryMax != null) {
      return `${app.salaryMin.toLocaleString()} – ${app.salaryMax.toLocaleString()} ${currency}`
    }
    if (app.salaryMin != null) return `From ${app.salaryMin.toLocaleString()} ${currency}`
    return `Up to ${app.salaryMax!.toLocaleString()} ${currency}`
  })()

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

          <p>
            <strong>Status:</strong>{' '}
            <select
              value={app.status}
              onChange={e => onChangeStatus(e.target.value as AppStatus)}
              disabled={saving}
            >
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </p>

          {app.source && (
            <p><strong>Source:</strong> {app.source}</p>
          )}

          <p><strong>Created:</strong> {new Date(app.createdAt).toLocaleString()}</p>

          {app.appliedAt && (
            <p><strong>Applied Date:</strong> {formatDate(app.appliedAt)}</p>
          )}

          {app.location && (
            <p><strong>Location:</strong> {app.location}</p>
          )}

          {salaryDisplay && (
            <p><strong>Salary:</strong> {salaryDisplay}</p>
          )}

          {app.jobLink && (
            <p>
              <strong>Job Link:</strong>{' '}
              <a href={app.jobLink} target="_blank" rel="noopener noreferrer">
                {app.jobLink}
              </a>
            </p>
          )}

          {app.resumeUploaded && (
            <p><strong>Resume Uploaded:</strong> {app.resumeUploaded}</p>
          )}

          {app.gotCall != null && (
            <p><strong>Got Call:</strong> {app.gotCall ? 'Yes' : 'No'}</p>
          )}

          {app.rejectDate && (
            <p><strong>Reject Date:</strong> {formatDate(app.rejectDate)}</p>
          )}

          {app.loginDetails && (
            <p><strong>Login Details:</strong> {app.loginDetails}</p>
          )}

          {app.nextFollowUpOn && (
            <p><strong>Follow-up On:</strong> {formatDate(app.nextFollowUpOn)}</p>
          )}

          {app.updatedAt && (
            <p><small>Last updated: {new Date(app.updatedAt).toLocaleString()}</small></p>
          )}

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
