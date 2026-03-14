import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { getActivityFeed, type ActivityItem } from '../api/stats'
import { fetchMockApplicationById, updateMockApplication } from '../mocks/applications'
import {
  getApplicationById as apiGet,
  patchApplication as apiPatch,
  type Application,
  type AppStatus,
} from '../api/applications'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const STATUSES: AppStatus[] = ['APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED', 'ACCEPTED', 'WITHDRAWN']

function formatActivityTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
  } catch {
    return iso
  }
}

function formatDate(iso: string | undefined): string | null {
  if (!iso) return null
  try {
    // Date-only strings (YYYY-MM-DD) are UTC midnight in JS — parse as local
    const parts = iso.split('T')[0].split('-').map(Number)
    const d = parts.length === 3 ? new Date(parts[0], parts[1] - 1, parts[2]) : new Date(iso)
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
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
  const [editingRejectDate, setEditingRejectDate] = useState(false)
  const [rejectDateInput, setRejectDateInput] = useState('')
  const [toast, setToast] = useState<string | null>(null)

  const { data: activityItems, isLoading: activityLoading, isError: activityError } = useQuery<ActivityItem[]>({
    queryKey: ['activity', id],
    queryFn: () => getActivityFeed(id!),
    enabled: !USE_MOCK && !!id,
    staleTime: 30_000,
  })

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
        ? (await updateMockApplication(id, { status: next as Parameters<typeof updateMockApplication>[1]['status'] })) as Application
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

  async function saveRejectDate() {
    if (!app || !id) return
    setSaving(true)
    try {
      const updated = USE_MOCK
        ? (await updateMockApplication(id, {})) as Application
        : await apiPatch(id, { rejectDate: rejectDateInput || undefined })
      setApp(updated)
      setEditingRejectDate(false)
      setToast('Reject date saved')
      setTimeout(() => setToast(null), 1500)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to save')
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

          <p style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <strong>Got Call:</strong>
            <input
              type="checkbox"
              style={{ margin: 0 }}
              checked={app.gotCall ?? false}
              disabled={saving}
              onChange={async e => {
                const next = e.target.checked
                const prev = app
                setApp({ ...app, gotCall: next })
                setSaving(true)
                try {
                  const updated = USE_MOCK
                    ? (await updateMockApplication(id!, {})) as Application
                    : await apiPatch(id!, { gotCall: next })
                  setApp(updated)
                  setToast(`Got Call marked ${next ? 'Yes' : 'No'}`)
                  setTimeout(() => setToast(null), 1500)
                } catch {
                  setApp(prev)
                } finally {
                  setSaving(false)
                }
              }}
            />
            <span style={{ fontSize: '0.9rem' }}>{app.gotCall ? 'Yes' : 'No'}</span>
          </p>

          <p>
            <strong>Reject Date:</strong>{' '}
            {editingRejectDate ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
                <input
                  type="date"
                  style={{ display: 'inline', width: 'auto', marginBottom: 0, padding: '2px 6px', fontSize: '0.9rem' }}
                  value={rejectDateInput}
                  onChange={e => setRejectDateInput(e.target.value)}
                  autoFocus
                />
                <button style={{ padding: '2px 10px', fontSize: '0.8rem', marginBottom: 0 }} disabled={saving} onClick={saveRejectDate}>Save</button>
                <button className="secondary" style={{ padding: '2px 10px', fontSize: '0.8rem', marginBottom: 0 }} onClick={() => setEditingRejectDate(false)}>Cancel</button>
              </span>
            ) : (
              <span>
                {app.rejectDate ? formatDate(app.rejectDate) : <em style={{ color: 'var(--pico-muted-color)' }}>Not set</em>}
                {' '}
                <button
                  className="secondary"
                  style={{ padding: '1px 8px', fontSize: '0.75rem', marginBottom: 0 }}
                  onClick={() => { setRejectDateInput(app.rejectDate ?? ''); setEditingRejectDate(true) }}
                >
                  {app.rejectDate ? 'Edit' : 'Add'}
                </button>
              </span>
            )}
          </p>

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

      {!USE_MOCK && (
        <article>
          <header>Activity</header>
          {activityLoading && <p style={{ color: 'var(--pico-muted-color)' }}>Loading activity…</p>}
          {activityError && <p style={{ color: 'var(--pico-del-color)' }}>Could not load activity feed.</p>}
          {!activityLoading && !activityError && activityItems && activityItems.length === 0 && (
            <p style={{ color: 'var(--pico-muted-color)' }}><em>No activity recorded yet.</em></p>
          )}
          {activityItems && activityItems.length > 0 && (
            <ol style={{ listStyle: 'none', padding: 0, margin: 0 }}>
              {activityItems.map(item => (
                <li key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', padding: '0.4rem 0', borderBottom: '1px solid var(--pico-muted-border-color)' }}>
                  <span>{item.message}</span>
                  <small style={{ color: 'var(--pico-muted-color)', whiteSpace: 'nowrap', marginLeft: '1rem' }}>{formatActivityTime(item.occurredAt)}</small>
                </li>
              ))}
            </ol>
          )}
        </article>
      )}
    </>
  )
}
