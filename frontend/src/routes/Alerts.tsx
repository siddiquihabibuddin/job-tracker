import { useState, type FormEvent } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  listAlerts,
  createAlert,
  deleteAlert,
  updateAlert,
  listMatchesForAlert,
  listUnseenMatches,
  markSeen,
  markAllSeen,
  pollNow,
  type CompanyInput,
  type CreateAlertInput,
  type JobAlertMatch,
} from '../api/alerts'

function relativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diffMs / 60_000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

const PLATFORMS = ['GREENHOUSE', 'LEVER', 'WORKDAY']

const PLATFORM_COLORS: Record<string, { background: string; color: string }> = {
  GREENHOUSE: { background: '#dcfce7', color: '#15803d' },
  LEVER:      { background: '#dbeafe', color: '#1d4ed8' },
  WORKDAY:    { background: '#ffedd5', color: '#c2410c' },
}

function PlatformBadge({ platform }: { platform: string }) {
  const style = PLATFORM_COLORS[platform] ?? { background: '#f3f4f6', color: '#374151' }
  return (
    <span style={{
      ...style,
      borderRadius: '9999px',
      fontSize: '0.68rem',
      fontWeight: 600,
      padding: '0.15rem 0.5rem',
      letterSpacing: '0.03em',
      whiteSpace: 'nowrap',
    }}>
      {platform}
    </span>
  )
}

interface MatchRowProps {
  match: JobAlertMatch
  onMarkSeen: (id: string) => void
  markingId: string | null
}

function MatchRow({ match, onMarkSeen, markingId }: MatchRowProps) {
  const navigate = useNavigate()
  const unseen = !match.seenAt
  return (
    <li style={{
      display: 'flex',
      alignItems: 'flex-start',
      justifyContent: 'space-between',
      gap: '0.75rem',
      padding: '0.6rem 0.5rem',
      borderRadius: '4px',
      background: unseen ? 'rgba(37,99,235,0.06)' : 'transparent',
      borderBottom: '1px solid var(--pico-muted-border-color)',
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap' }}>
          <PlatformBadge platform={match.platform} />
          {match.jobUrl ? (
            <a
              href={match.jobUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{ fontSize: '0.82rem', fontWeight: 500 }}
            >
              {match.title}
            </a>
          ) : (
            <span style={{ fontSize: '0.82rem', fontWeight: 500 }}>{match.title}</span>
          )}
          {!unseen && (
            <span style={{ fontSize: '0.68rem', color: 'var(--pico-muted-color)' }}>seen</span>
          )}
        </div>
        <p style={{ margin: '0.2rem 0 0', fontSize: '0.72rem', color: 'var(--pico-muted-color)' }}>
          {match.companyName}
          {match.location ? ` · ${match.location}` : ''}
          {match.postedAt ? ` · ${new Date(match.postedAt).toLocaleDateString()}` : ''}
        </p>
      </div>
      <div style={{ display: 'flex', gap: '0.4rem', flexShrink: 0, alignItems: 'center' }}>
        {unseen && (
          <button
            style={{ padding: '2px 8px', fontSize: '0.72rem', marginBottom: 0 }}
            className="secondary"
            onClick={() => onMarkSeen(match.id)}
            disabled={markingId === match.id}
            aria-busy={markingId === match.id}
          >
            Mark Seen
          </button>
        )}
        <button
          style={{ padding: '2px 8px', fontSize: '0.72rem', marginBottom: 0 }}
          className="contrast"
          onClick={() =>
            navigate('/applications/new', {
              state: {
                company: match.companyName,
                role: match.title,
                jobLink: match.jobUrl ?? undefined,
              },
            })
          }
        >
          Track It
        </button>
      </div>
    </li>
  )
}

const EMPTY_COMPANY: CompanyInput = {
  companyName: '',
  boardToken: '',
  workdayTenant: '',
  workdaySite: 'External_Career_Site',
  workdayWdNum: 1,
}

interface FormState {
  companies: CompanyInput[]
  roleKeywords: string
  platforms: string[]
}

const EMPTY_FORM: FormState = {
  companies: [{ ...EMPTY_COMPANY }],
  roleKeywords: '',
  platforms: [],
}

function CompanyEntry({
  company,
  index,
  total,
  showWorkday,
  onChange,
  onRemove,
}: {
  company: CompanyInput
  index: number
  total: number
  showWorkday: boolean
  onChange: (index: number, field: keyof CompanyInput, value: string | number) => void
  onRemove: (index: number) => void
}) {
  return (
    <div style={{
      border: '1px solid var(--pico-muted-border-color)',
      borderRadius: '6px',
      padding: '0.75rem',
      marginBottom: '0.75rem',
      position: 'relative',
    }}>
      {total > 1 && (
        <button
          type="button"
          onClick={() => onRemove(index)}
          style={{
            position: 'absolute',
            top: '0.5rem',
            right: '0.5rem',
            padding: '0 6px',
            fontSize: '0.85rem',
            lineHeight: '1.4',
            marginBottom: 0,
            background: 'transparent',
            border: '1px solid var(--pico-muted-border-color)',
            color: 'var(--pico-muted-color)',
            cursor: 'pointer',
            borderRadius: '4px',
          }}
          aria-label="Remove company"
        >
          ×
        </button>
      )}
      <div className="grid" style={{ paddingRight: total > 1 ? '2rem' : undefined }}>
        <label>
          Company Name
          <input
            required
            value={company.companyName}
            onChange={e => onChange(index, 'companyName', e.target.value)}
            placeholder="e.g. Stripe"
          />
        </label>
        <label>
          Board Token / Slug
          <input
            value={company.boardToken ?? ''}
            onChange={e => onChange(index, 'boardToken', e.target.value)}
            placeholder="e.g. stripe"
          />
          <small>For Greenhouse &amp; Lever</small>
        </label>
      </div>

      {showWorkday && (
        <div className="grid">
          <label>
            Workday Tenant
            <input
              value={company.workdayTenant ?? ''}
              onChange={e => onChange(index, 'workdayTenant', e.target.value)}
              placeholder="e.g. amazon"
            />
          </label>
          <label>
            Workday Site
            <input
              value={company.workdaySite ?? 'External_Career_Site'}
              onChange={e => onChange(index, 'workdaySite', e.target.value)}
            />
          </label>
          <label>
            WD#
            <input
              type="number"
              min={1}
              value={company.workdayWdNum ?? 1}
              onChange={e => onChange(index, 'workdayWdNum', parseInt(e.target.value) || 1)}
            />
          </label>
        </div>
      )}
    </div>
  )
}

export default function Alerts() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [expandedAlertId, setExpandedAlertId] = useState<string | null>(null)
  const [markingId, setMarkingId] = useState<string | null>(null)
  const [formData, setFormData] = useState<FormState>(EMPTY_FORM)

  const { data: alerts = [], isLoading: alertsLoading, isError: alertsError } = useQuery({
    queryKey: ['alerts'],
    queryFn: listAlerts,
  })

  const { data: unseenMatches = [] } = useQuery({
    queryKey: ['unseen-matches'],
    queryFn: listUnseenMatches,
    refetchInterval: 5 * 60 * 1000,
    staleTime: 60_000,
  })

  const { data: alertMatches = [], isLoading: matchesLoading } = useQuery({
    queryKey: ['alert-matches', expandedAlertId],
    queryFn: () => listMatchesForAlert(expandedAlertId!),
    enabled: !!expandedAlertId,
  })

  const createMutation = useMutation({
    mutationFn: createAlert,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['alerts'] })
      setShowForm(false)
      setFormData(EMPTY_FORM)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteAlert,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['alerts'] }),
  })

  const toggleActiveMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) => updateAlert(id, { active }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['alerts'] }),
  })

  const markSeenMutation = useMutation({
    mutationFn: (matchId: string) => {
      setMarkingId(matchId)
      return markSeen(matchId)
    },
    onSettled: () => setMarkingId(null),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['unseen-matches'] })
      qc.invalidateQueries({ queryKey: ['unseen-count'] })
      if (expandedAlertId) qc.invalidateQueries({ queryKey: ['alert-matches', expandedAlertId] })
    },
  })

  const markAllSeenMutation = useMutation({
    mutationFn: markAllSeen,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['unseen-matches'] })
      qc.invalidateQueries({ queryKey: ['unseen-count'] })
      if (expandedAlertId) qc.invalidateQueries({ queryKey: ['alert-matches', expandedAlertId] })
    },
  })

  const pollNowMutation = useMutation({
    mutationFn: pollNow,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['unseen-matches'] })
      qc.invalidateQueries({ queryKey: ['unseen-count'] })
      qc.invalidateQueries({ queryKey: ['alerts'] })
      if (expandedAlertId) qc.invalidateQueries({ queryKey: ['alert-matches', expandedAlertId] })
    },
  })

  function togglePlatform(p: string) {
    setFormData(prev => ({
      ...prev,
      platforms: prev.platforms.includes(p)
        ? prev.platforms.filter(x => x !== p)
        : [...prev.platforms, p],
    }))
  }

  function updateCompany(index: number, field: keyof CompanyInput, value: string | number) {
    setFormData(prev => {
      const companies = prev.companies.map((c, i) => {
        if (i !== index) return c
        const updated = { ...c, [field]: value }
        if (field === 'companyName' && typeof value === 'string') {
          const slug = value.toLowerCase().replace(/\s+/g, '')
          const prevSlug = c.companyName.toLowerCase().replace(/\s+/g, '')
          if (!c.boardToken || c.boardToken === prevSlug) updated.boardToken = slug
          if (!c.workdayTenant || c.workdayTenant === prevSlug) updated.workdayTenant = slug
        }
        return updated
      })
      return { ...prev, companies }
    })
  }

  function addCompany() {
    setFormData(prev => ({
      ...prev,
      companies: [...prev.companies, { ...EMPTY_COMPANY }],
    }))
  }

  function removeCompany(index: number) {
    setFormData(prev => ({
      ...prev,
      companies: prev.companies.filter((_, i) => i !== index),
    }))
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!formData.roleKeywords.trim()) return
    if (formData.companies.some(c => !c.companyName.trim())) return
    const input: CreateAlertInput = {
      companies: formData.companies.map(c => ({
        companyName: c.companyName.trim(),
        boardToken: c.boardToken?.trim() || null,
        workdayTenant: formData.platforms.includes('WORKDAY') ? (c.workdayTenant?.trim() || null) : null,
        workdaySite: formData.platforms.includes('WORKDAY') ? (c.workdaySite?.trim() || 'External_Career_Site') : null,
        workdayWdNum: formData.platforms.includes('WORKDAY') ? (c.workdayWdNum ?? 1) : null,
      })),
      roleKeywords: formData.roleKeywords.trim(),
      platforms: formData.platforms,
    }
    createMutation.mutate(input)
  }

  const unseenInExpanded = alertMatches.filter(m => !m.seenAt)
  const showWorkday = formData.platforms.includes('WORKDAY')

  return (
    <>
      {/* Page header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
        <h2 style={{ margin: 0 }}>Job Alerts</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className="secondary"
            style={{ padding: '0.4rem 1rem', fontSize: '0.85rem', marginBottom: 0 }}
            onClick={() => pollNowMutation.mutate()}
            disabled={pollNowMutation.isPending}
            aria-busy={pollNowMutation.isPending}
          >
            {pollNowMutation.isPending ? 'Polling…' : '↻ Poll Now'}
          </button>
          <button
            className={showForm ? 'secondary' : 'contrast'}
            style={{ padding: '0.4rem 1rem', fontSize: '0.85rem', marginBottom: 0 }}
            onClick={() => setShowForm(v => !v)}
          >
            {showForm ? 'Cancel' : '+ New Alert'}
          </button>
        </div>
      </div>

      {/* ── New Alert Form ───────────────────────────────────────── */}
      {showForm && (
        <article style={{ marginBottom: '1.25rem' }}>
          <header style={{ marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>New Job Alert</span>
          </header>
          <form onSubmit={handleSubmit}>
            <fieldset>
              <div style={{ marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 600 }}>Companies</span>
              </div>

              {formData.companies.map((company, idx) => (
                <CompanyEntry
                  key={idx}
                  company={company}
                  index={idx}
                  total={formData.companies.length}
                  showWorkday={showWorkday}
                  onChange={updateCompany}
                  onRemove={removeCompany}
                />
              ))}

              <button
                type="button"
                className="secondary"
                style={{ padding: '0.3rem 0.75rem', fontSize: '0.78rem', marginBottom: '1rem' }}
                onClick={addCompany}
              >
                + Add Another Company
              </button>

              <label>
                Role Keywords
                <input
                  required
                  value={formData.roleKeywords}
                  onChange={e => setFormData(p => ({ ...p, roleKeywords: e.target.value }))}
                  placeholder="e.g. backend,java,engineer"
                />
                <small>Comma-separated keywords to match against job titles</small>
              </label>

              <fieldset>
                <legend style={{ fontSize: '0.82rem' }}>Platforms</legend>
                <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
                  {PLATFORMS.map(p => (
                    <label key={p} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={formData.platforms.includes(p)}
                        onChange={() => togglePlatform(p)}
                        style={{ marginBottom: 0 }}
                      />
                      <PlatformBadge platform={p} />
                    </label>
                  ))}
                </div>
              </fieldset>
            </fieldset>

            {createMutation.isError && (
              <article style={{ background: 'rgba(239,68,68,0.08)', borderLeft: '3px solid #ef4444', padding: '0.5rem 0.75rem', marginBottom: '0.75rem' }}>
                <small style={{ color: '#b91c1c' }}>
                  {createMutation.error instanceof Error ? createMutation.error.message : 'Failed to create alert'}
                </small>
              </article>
            )}

            <button
              type="submit"
              className="contrast"
              disabled={createMutation.isPending}
              aria-busy={createMutation.isPending}
            >
              {createMutation.isPending ? 'Creating…' : 'Create Alert'}
            </button>
          </form>
        </article>
      )}

      {/* ── Unseen Matches Panel ─────────────────────────────────── */}
      {unseenMatches.length > 0 && (
        <article style={{ marginBottom: '1.25rem', borderLeft: '3px solid #2563eb' }}>
          <header style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <span style={{
              background: '#2563eb',
              color: '#fff',
              borderRadius: '9999px',
              fontSize: '0.72rem',
              fontWeight: 700,
              padding: '0.1rem 0.45rem',
            }}>
              {unseenMatches.length}
            </span>
            <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>Unseen Matches</span>
          </header>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {unseenMatches.map(match => (
              <MatchRow
                key={match.id}
                match={match}
                onMarkSeen={id => markSeenMutation.mutate(id)}
                markingId={markingId}
              />
            ))}
          </ul>
        </article>
      )}

      {/* ── Configured Alerts Table ──────────────────────────────── */}
      <article style={{ padding: '0.75rem', marginBottom: 0 }}>
        <header style={{ marginBottom: '0.75rem' }}>
          <span style={{ fontSize: '0.88rem', fontWeight: 600 }}>Configured Alerts</span>
        </header>

        {alertsLoading && (
          <p style={{ fontSize: '0.82rem', color: 'var(--pico-muted-color)' }} aria-busy="true">
            Loading alerts…
          </p>
        )}

        {!alertsLoading && alertsError && (
          <p style={{ fontSize: '0.82rem', color: 'var(--pico-del-color)' }}>
            Failed to load alerts.
          </p>
        )}

        {!alertsLoading && !alertsError && alerts.length === 0 && (
          <p style={{ fontSize: '0.82rem', color: 'var(--pico-muted-color)', margin: 0 }}>
            No alerts configured yet. Use "+ New Alert" to start tracking job postings automatically.
          </p>
        )}

        {!alertsLoading && alerts.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0' }}>
            {alerts.map((alert, idx) => {
              const isExpanded = expandedAlertId === alert.id
              const isLast = idx === alerts.length - 1
              const companyNames = alert.companies.map(c => c.companyName).join(', ')
              return (
                <div
                  key={alert.id}
                  style={{
                    borderBottom: isLast ? 'none' : '1px solid var(--pico-muted-border-color)',
                  }}
                >
                  {/* Alert row */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: '0.75rem',
                    padding: '0.65rem 0',
                  }}>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', flexWrap: 'wrap', marginBottom: '0.2rem' }}>
                        {alert.platforms.map(p => <PlatformBadge key={p} platform={p} />)}
                        <span style={{
                          fontSize: '0.68rem',
                          fontWeight: 600,
                          padding: '0.1rem 0.45rem',
                          borderRadius: '9999px',
                          background: alert.active ? '#dcfce7' : '#f3f4f6',
                          color: alert.active ? '#15803d' : '#6b7280',
                        }}>
                          {alert.active ? 'Active' : 'Paused'}
                        </span>
                      </div>
                      <div style={{ marginBottom: '0.2rem' }}>
                        {alert.companies.map((company, ci) => (
                          <div key={ci} style={{ marginBottom: company.lastErrorMessage ? '0.3rem' : '0.1rem' }}>
                            <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{company.companyName}</span>
                            {company.lastErrorMessage && company.lastErrorAt && (
                              <div style={{
                                marginTop: '0.25rem',
                                padding: '0.35rem 0.5rem',
                                background: 'rgba(217,119,6,0.08)',
                                borderRadius: '4px',
                                borderLeft: '2px solid #d97706',
                              }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', marginBottom: '0.25rem' }}>
                                  <svg aria-hidden="true" width="11" height="11" viewBox="0 0 16 16" fill="currentColor" style={{ color: '#b45309', flexShrink: 0 }}>
                                    <path d="M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1zm0 3a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 8 4zm0 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2z" />
                                  </svg>
                                  <span style={{ fontSize: '0.68rem', color: '#b45309', opacity: 0.75 }}>
                                    Poll errors · {relativeTime(company.lastErrorAt)}
                                  </span>
                                </div>
                                {company.lastErrorMessage.split('\n').map((line, li) => {
                                  const sep = line.indexOf(': ')
                                  const platform = sep !== -1 ? line.slice(0, sep) : null
                                  const message = sep !== -1 ? line.slice(sep + 2) : line
                                  const knownPlatform = platform && PLATFORMS.includes(platform)
                                  return (
                                    <div key={li} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.4rem', marginTop: li > 0 ? '0.2rem' : 0 }}>
                                      {knownPlatform && <PlatformBadge platform={platform!} />}
                                      <span style={{ fontSize: '0.72rem', color: '#92400e', wordBreak: 'break-word', flex: 1 }}>
                                        {message}
                                      </span>
                                    </div>
                                  )
                                })}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                      <p style={{ margin: 0, fontSize: '0.72rem', color: 'var(--pico-muted-color)' }}>
                        Keywords: {alert.roleKeywords}
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '0.4rem', flexShrink: 0, alignItems: 'center' }}>
                      <button
                        style={{ padding: '2px 8px', fontSize: '0.72rem', marginBottom: 0 }}
                        className="secondary"
                        onClick={() => setExpandedAlertId(isExpanded ? null : alert.id)}
                      >
                        {isExpanded ? 'Hide' : 'Matches'}
                      </button>
                      <button
                        style={{ padding: '2px 8px', fontSize: '0.72rem', marginBottom: 0 }}
                        className="secondary"
                        onClick={() => toggleActiveMutation.mutate({ id: alert.id, active: !alert.active })}
                        disabled={toggleActiveMutation.isPending}
                      >
                        {alert.active ? 'Pause' : 'Resume'}
                      </button>
                      <button
                        style={{ padding: '2px 8px', fontSize: '0.72rem', marginBottom: 0, color: '#dc2626' }}
                        className="secondary"
                        onClick={() => {
                          if (confirm(`Delete alert for ${companyNames}?`)) {
                            deleteMutation.mutate(alert.id)
                          }
                        }}
                        disabled={deleteMutation.isPending}
                      >
                        Delete
                      </button>
                    </div>
                  </div>

                  {/* Expanded matches for this alert */}
                  {isExpanded && (
                    <div style={{
                      background: 'var(--pico-card-background-color)',
                      borderRadius: '4px',
                      padding: '0.6rem',
                      marginBottom: '0.5rem',
                      border: '1px solid var(--pico-muted-border-color)',
                    }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>
                          Matches {matchesLoading ? '' : `(${alertMatches.length})`}
                        </span>
                        {unseenInExpanded.length > 0 && (
                          <button
                            style={{ padding: '1px 8px', fontSize: '0.72rem', marginBottom: 0 }}
                            className="secondary"
                            onClick={() => markAllSeenMutation.mutate(alert.id)}
                            disabled={markAllSeenMutation.isPending}
                            aria-busy={markAllSeenMutation.isPending}
                          >
                            Mark all seen
                          </button>
                        )}
                      </div>

                      {matchesLoading && (
                        <p style={{ fontSize: '0.78rem', color: 'var(--pico-muted-color)', margin: 0 }} aria-busy="true">
                          Loading…
                        </p>
                      )}

                      {!matchesLoading && alertMatches.length === 0 && (
                        <p style={{ fontSize: '0.78rem', color: 'var(--pico-muted-color)', margin: 0 }}>
                          No matches yet.
                        </p>
                      )}

                      {!matchesLoading && alertMatches.length > 0 && (
                        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                          {alertMatches.map(match => (
                            <MatchRow
                              key={match.id}
                              match={match}
                              onMarkSeen={id => markSeenMutation.mutate(id)}
                              markingId={markingId}
                            />
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        )}
      </article>
    </>
  )
}
