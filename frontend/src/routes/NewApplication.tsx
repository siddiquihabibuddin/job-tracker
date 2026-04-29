import { useState, useRef, type FormEvent } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { createMockApplication, type AppStatus as MockAppStatus } from '../mocks/applications'
import { createApplication as apiCreate, parseApplicationDescription, type AppStatus } from '../api/applications'
import axios from 'axios'

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

  // Smart create state
  const [description, setDescription] = useState('')
  const [parsing, setParsing] = useState(false)
  const [parseError, setParseError] = useState<string | null>(null)
  const [parseSuccessMsg, setParseSuccessMsg] = useState<string | null>(null)
  const companyRef = useRef<HTMLInputElement>(null)

  const VALID_STATUSES: AppStatus[] = ['APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED', 'ACCEPTED', 'WITHDRAWN']

  async function onParse() {
    const trimmed = description.trim()
    if (!trimmed) return
    setParsing(true)
    setParseError(null)
    setParseSuccessMsg(null)
    try {
      const parsed = await parseApplicationDescription(trimmed)
      let applied = 0

      if (parsed.company && !company.trim()) { setCompany(parsed.company); applied++ }
      if (parsed.role && !role.trim()) { setRole(parsed.role); applied++ }
      if (parsed.status && VALID_STATUSES.includes(parsed.status as AppStatus) && status === 'APPLIED') {
        setStatus(parsed.status as AppStatus)
        applied++
      }
      if (parsed.source && !source.trim()) { setSource(parsed.source); applied++ }
      if (parsed.location && !locationField.trim()) { setLocationField(parsed.location); applied++ }
      if (parsed.appliedAt && !appliedAt) { setAppliedAt(parsed.appliedAt); applied++ }
      if (parsed.salaryMin != null && !salaryMin) { setSalaryMin(String(parsed.salaryMin)); applied++ }
      if (parsed.salaryMax != null && !salaryMax) { setSalaryMax(String(parsed.salaryMax)); applied++ }
      if (parsed.currency && !currency.trim()) { setCurrency(parsed.currency); applied++ }
      if (parsed.jobLink && !jobLink.trim()) { setJobLink(parsed.jobLink); applied++ }
      if (parsed.notes && !notes.trim()) { setNotes(parsed.notes); applied++ }
      // TODO: tags — no tag input exists in the form yet; extracted tags are ignored until the form gains a tags field.

      setParseSuccessMsg(
        applied > 0
          ? `Filled in ${applied} field${applied === 1 ? '' : 's'} below — review and save.`
          : 'No new fields were extracted — fill the form manually.'
      )

      if (applied > 0) {
        companyRef.current?.focus()
      }
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        const httpStatus = err.response?.status
        if (httpStatus === 400) {
          const msg = (err.response?.data as { message?: string })?.message
          setParseError(msg ?? 'Invalid description — please check your input.')
        } else {
          setParseError("Couldn't parse — please fill the form manually.")
        }
      } else {
        setParseError("Couldn't parse — please fill the form manually.")
      }
    } finally {
      setParsing(false)
    }
  }

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
        <fieldset style={{ background: 'var(--pico-card-background-color)', borderRadius: 'var(--pico-border-radius)', padding: '1.25rem', marginBottom: '1rem' }}>
          <legend><strong>✦ Smart create with AI</strong></legend>
          <p style={{ marginBottom: '0.75rem', color: 'var(--pico-muted-color)', fontSize: '0.875rem' }}>
            Paste a sentence describing the application and we'll fill in the form below. Review and edit before saving.
          </p>
          <label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder='Today I applied to a senior engineer position at Microsoft using LinkedIn with a salary of $150,000.'
              rows={4}
              style={{ resize: 'vertical' }}
            />
          </label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', marginTop: '0.25rem' }}>
            <button
              type="button"
              className="contrast"
              style={{ marginBottom: 0, flex: '0 0 auto' }}
              disabled={!description.trim() || parsing}
              aria-busy={parsing}
              onClick={onParse}
            >
              {parsing ? 'Parsing…' : 'Parse with AI'}
            </button>
            {parsing && (
              <small aria-live="polite" style={{ color: 'var(--pico-muted-color)' }}>Parsing your description…</small>
            )}
            {!parsing && parseError && (
              <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{parseError}</small>
            )}
            {!parsing && parseSuccessMsg && !parseError && (
              <small aria-live="polite" style={{ color: 'var(--pico-ins-color, #22c55e)' }}>✓ {parseSuccessMsg}</small>
            )}
          </div>
        </fieldset>

        <fieldset>
          <legend>Required Details</legend>
          <div className="grid">
            <label>
              Company
              <input
                ref={companyRef}
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
