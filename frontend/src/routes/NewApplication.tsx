import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createMockApplication, type AppStatus } from '../mocks/applications'
import { createApplication as apiCreate } from '../api/applications'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

export default function NewApplication() {
  const nav = useNavigate()
  const [company, setCompany] = useState('')
  const [role, setRole] = useState('')
  const [status, setStatus] = useState<AppStatus>('APPLIED')
  const [source, setSource] = useState('LinkedIn')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!company.trim() || !role.trim()) {
      setError('Company and Role are required.')
      return
    }
    setSubmitting(true)
    try {
      if (USE_MOCK) {
        await createMockApplication({ company: company.trim(), role: role.trim(), status, source })
      } else {
        await apiCreate({ company: company.trim(), role: role.trim(), status, source })
      }
      nav('/applications')
    } catch (err: any) {
      setError(err?.message || 'Failed to create application')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <h2>New Application</h2>
      {error && (
        <article>
          <header>Error</header>
          <p>{error}</p>
        </article>
      )}
      <form className="grid" onSubmit={onSubmit}>
        <label>
          Company
          <input value={company} onChange={e => setCompany(e.target.value)} placeholder="Acme Corp" required />
        </label>
        <label>
          Role
          <input value={role} onChange={e => setRole(e.target.value)} placeholder="Senior Java Engineer" required />
        </label>
        <label>
          Status
          <select value={status} onChange={e => setStatus(e.target.value as AppStatus)}>
            <option>APPLIED</option>
            <option>PHONE</option>
            <option>ONSITE</option>
            <option>OFFER</option>
            <option>REJECTED</option>
          </select>
        </label>
        <label>
          Source
          <select value={source} onChange={e => setSource(e.target.value)}>
            <option>LinkedIn</option>
            <option>Referral</option>
            <option>Company</option>
            <option>Indeed</option>
            <option>Other</option>
          </select>
        </label>

        <small>
          Data source: <code>{USE_MOCK ? 'mock' : 'API'}</code>
        </small>

        <div>
          <button type="button" onClick={() => nav('/applications')}>Cancel</button>
          <button className="contrast" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create'}
          </button>
        </div>
      </form>
    </>
  )
}