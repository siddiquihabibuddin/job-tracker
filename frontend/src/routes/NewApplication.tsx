import { useState, type FormEvent } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { createMockApplication, type AppStatus as MockAppStatus } from '../mocks/applications'
import { createApplication as apiCreate, type AppStatus } from '../api/applications'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

interface LocationState {
  company?: string
  role?: string
  jobLink?: string
}

export default function NewApplication() {
  const nav = useNavigate()
  const location = useLocation()
  const prefill = (location.state as LocationState) ?? {}

  // Required fields
  const [company, setCompany] = useState(prefill.company ?? '')
  const [role, setRole] = useState(prefill.role ?? '')
  const [status, setStatus] = useState<AppStatus>('APPLIED')
  const [source, setSource] = useState('LinkedIn')

  // Optional fields
  const [locationField, setLocationField] = useState('')
  const [appliedAt, setAppliedAt] = useState('')
  const [salaryMin, setSalaryMin] = useState('')
  const [salaryMax, setSalaryMax] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [jobLink, setJobLink] = useState(prefill.jobLink ?? '')
  const [resumeUploaded, setResumeUploaded] = useState('')
  const [gotCall, setGotCall] = useState(false)
  const [rejectDate, setRejectDate] = useState('')
  const [loginDetails, setLoginDetails] = useState('')
  const [notes, setNotes] = useState('')

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
        await createMockApplication({
          company: company.trim(),
          role: role.trim(),
          status: status as MockAppStatus,
          source,
        })
      } else {
        await apiCreate({
          company: company.trim(),
          role: role.trim(),
          status,
          source: source || undefined,
          location: locationField.trim() || undefined,
          appliedAt: appliedAt || undefined,
          salary: {
            min: salaryMin ? Number(salaryMin) : undefined,
            max: salaryMax ? Number(salaryMax) : undefined,
            currency: currency || 'USD',
          },
          jobLink: jobLink.trim() || undefined,
          resumeUploaded: resumeUploaded.trim() || undefined,
          gotCall,
          rejectDate: rejectDate || undefined,
          loginDetails: loginDetails.trim() || undefined,
          notes: notes.trim() || undefined,
        })
      }
      nav('/applications')
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Failed to create application'
      setError(message)
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
      <form onSubmit={onSubmit}>
        <fieldset>
          <legend>Required Details</legend>
          <div className="grid">
            <label>
              Company
              <input
                value={company}
                onChange={e => setCompany(e.target.value)}
                placeholder="Acme Corp"
                required
              />
            </label>
            <label>
              Role
              <input
                value={role}
                onChange={e => setRole(e.target.value)}
                placeholder="Senior Java Engineer"
                required
              />
            </label>
          </div>
          <div className="grid">
            <label>
              Status
              <select value={status} onChange={e => setStatus(e.target.value as AppStatus)}>
                <option value="APPLIED">Applied</option>
                <option value="PHONE">Phone Screen</option>
                <option value="ONSITE">Onsite</option>
                <option value="OFFER">Offer</option>
                <option value="REJECTED">Rejected</option>
                <option value="ACCEPTED">Accepted</option>
                <option value="WITHDRAWN">Withdrawn</option>
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
          </div>
        </fieldset>

        <fieldset>
          <legend>Optional Details</legend>
          <div className="grid">
            <label>
              Location
              <input
                value={locationField}
                onChange={e => setLocationField(e.target.value)}
                placeholder="San Francisco, CA"
              />
            </label>
            <label>
              Apply Date
              <input
                type="date"
                value={appliedAt}
                onChange={e => setAppliedAt(e.target.value)}
              />
            </label>
          </div>

          <div className="grid">
            <label>
              Salary Min
              <input
                type="number"
                value={salaryMin}
                onChange={e => setSalaryMin(e.target.value)}
                placeholder="80000"
                min="0"
              />
            </label>
            <label>
              Salary Max
              <input
                type="number"
                value={salaryMax}
                onChange={e => setSalaryMax(e.target.value)}
                placeholder="120000"
                min="0"
              />
            </label>
            <label>
              Currency
              <input
                value={currency}
                onChange={e => setCurrency(e.target.value)}
                placeholder="USD"
                maxLength={10}
              />
            </label>
          </div>

          <label>
            Job Link
            <input
              type="url"
              value={jobLink}
              onChange={e => setJobLink(e.target.value)}
              placeholder="https://company.com/jobs/12345"
            />
          </label>

          <label>
            Resume Uploaded
            <input
              value={resumeUploaded}
              onChange={e => setResumeUploaded(e.target.value)}
              placeholder="resume-v3.pdf or 'Yes'"
            />
          </label>

          <div className="grid">
            <label>
              <input
                type="checkbox"
                role="switch"
                checked={gotCall}
                onChange={e => setGotCall(e.target.checked)}
              />
              Got a call / recruiter reached out
            </label>
            <label>
              Reject Date
              <input
                type="date"
                value={rejectDate}
                onChange={e => setRejectDate(e.target.value)}
              />
            </label>
          </div>

          <label>
            Login Details
            <textarea
              value={loginDetails}
              onChange={e => setLoginDetails(e.target.value)}
              placeholder="Portal URL, username, or any login notes"
              rows={2}
            />
          </label>

          <label>
            Notes
            <textarea
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="Any additional notes about this application"
              rows={3}
            />
          </label>
        </fieldset>

        <small>
          Data source: <code>{USE_MOCK ? 'mock' : 'API'}</code>
        </small>

        <div style={{ display: 'flex', gap: '.5rem', marginTop: '1rem' }}>
          <button type="button" onClick={() => nav('/applications')}>Cancel</button>
          <button className="contrast" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create'}
          </button>
        </div>
      </form>
    </>
  )
}
