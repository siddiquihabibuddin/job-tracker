import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import {
  listApplications as apiList,
  deleteApplication as apiDelete,
  importCsv,
  type Application,
  type AppStatus,
  type CsvImportResult,
} from '../api/applications'
import {
  fetchMockApplications as mockList,
  deleteMockApplication as mockDelete,
} from '../mocks/applications'
import { useDebounce } from '../hooks/useDebounce'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const STATUSES: (AppStatus | 'ALL')[] = ['ALL', 'APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED', 'ACCEPTED', 'WITHDRAWN']
const PAGE_SIZE = 20

const STATUS_COLORS: Record<string, string> = {
  APPLIED:   '#3b82f6',
  PHONE:     '#f59e0b',
  ONSITE:    '#8b5cf6',
  OFFER:     '#10b981',
  REJECTED:  '#ef4444',
  ACCEPTED:  '#22c55e',
  WITHDRAWN: '#6b7280',
}

function StatusBadge({ status }: { status: string }) {
  const color = STATUS_COLORS[status] ?? '#6b7280'
  return (
    <span style={{
      display: 'inline-block',
      padding: '1px 6px',
      borderRadius: '999px',
      fontSize: '0.7rem',
      fontWeight: 600,
      letterSpacing: '0.02em',
      backgroundColor: color + '22',
      color,
      border: `1px solid ${color}55`,
      whiteSpace: 'nowrap',
    }}>
      {status}
    </span>
  )
}

export default function Applications() {
  const [params, setParams] = useSearchParams()

  const statusParam = (params.get('status') || 'ALL').toUpperCase()
  const searchParam = params.get('search') || ''

  const [status, setStatus] = useState<AppStatus | 'ALL'>(isValidStatus(statusParam) ? (statusParam as AppStatus | 'ALL') : 'ALL')
  const [search, setSearch] = useState(searchParam)
  const [month, setMonth] = useState<string>('')
  const [year, setYear] = useState<string>('')
  const [gotCall, setGotCall] = useState<string>('')
  const [sortBy, setSortBy] = useState<string>('appliedAt')
  const [page, setPage] = useState(0)

  // sync filters → URL
  useEffect(() => {
    const next = new URLSearchParams()
    if (status !== 'ALL') next.set('status', status)
    if (search.trim()) next.set('search', search.trim())
    setParams(next, { replace: true })
  }, [status, search, setParams])

  // reset to page 0 when filters change
  useEffect(() => { setPage(0) }, [status, search, month, year, gotCall, sortBy])

  // clear selection when page or filters change
  useEffect(() => { setSelected(new Set()) }, [status, search, month, year, gotCall, sortBy, page])

  const debouncedSearch = useDebounce(search, 300)

  const [rows, setRows] = useState<Application[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadKey, setLoadKey] = useState(0)

  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [bulkDeleting, setBulkDeleting] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  // CSV import state
  const [showImport, setShowImport] = useState(false)
  const [importFile, setImportFile] = useState<File | null>(null)
  const [importing, setImporting] = useState(false)
  const [importResult, setImportResult] = useState<CsvImportResult | null>(null)
  const [importError, setImportError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    let alive = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        if (USE_MOCK) {
          const data = await mockList({ status, search: debouncedSearch })
          if (alive) {
            setRows(data as Application[])
            setTotalPages(1)
            setTotalElements((data as Application[]).length)
          }
        } else {
          const res = await apiList({
            status,
            search: debouncedSearch,
            page,
            limit: PAGE_SIZE,
            month: month ? Number(month) : undefined,
            year: year ? Number(year) : undefined,
            gotCall: gotCall !== '' ? gotCall === 'true' : undefined,
            sortBy: sortBy || undefined,
          })
          if (alive) {
            setRows(res.items)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          }
        }
      } catch (e: unknown) {
        if (alive) setError(e instanceof Error ? e.message : 'Failed to load')
      } finally {
        if (alive) setLoading(false)
      }
    }
    load()
    return () => { alive = false }
  }, [status, debouncedSearch, page, loadKey, month, year, gotCall, sortBy])

  async function handleDelete(id: string, label: string) {
    setDeletingId(id)
    try {
      if (USE_MOCK) await mockDelete(id)
      else await apiDelete(id)
      setRows(prev => prev.filter(r => r.id !== id))
      setToast(`Deleted: ${label}`)
      setTimeout(() => setToast(null), 2000)
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : 'Failed to delete')
    } finally {
      setDeletingId(null)
    }
  }

  async function handleBulkDelete() {
    const ids = Array.from(selected)
    if (!confirm(`Delete ${ids.length} application${ids.length > 1 ? 's' : ''}?`)) return
    setBulkDeleting(true)
    try {
      await Promise.all(ids.map(id => USE_MOCK ? mockDelete(id) : apiDelete(id)))
      setRows(prev => prev.filter(r => !selected.has(r.id)))
      setSelected(new Set())
      setToast(`Deleted ${ids.length} application${ids.length > 1 ? 's' : ''}`)
      setTimeout(() => setToast(null), 2500)
    } catch {
      alert('One or more deletions failed')
    } finally {
      setBulkDeleting(false)
    }
  }

  function openImportModal() {
    setImportFile(null); setImportResult(null); setImportError(null); setShowImport(true)
  }
  function closeImportModal() {
    setShowImport(false); setImportFile(null); setImportResult(null); setImportError(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  async function handleImport() {
    if (!importFile) return
    setImporting(true); setImportError(null); setImportResult(null)
    try {
      const result = await importCsv(importFile)
      setImportResult(result)
      if (result.imported > 0) { setPage(0); setLoadKey(k => k + 1) }
    } catch (e: unknown) {
      setImportError(e instanceof Error ? e.message : 'Import failed')
    } finally {
      setImporting(false)
    }
  }

  const cellStyle: React.CSSProperties = {
    fontSize: '0.78rem',
    padding: '4px 7px',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    maxWidth: '150px',
  }
  const roleCell: React.CSSProperties = {
    ...cellStyle,
    maxWidth: '280px',
  }
  const thStyle: React.CSSProperties = {
    fontSize: '0.7rem',
    fontWeight: 600,
    padding: '4px 7px',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    whiteSpace: 'nowrap',
  }
  const filterInput: React.CSSProperties = {
    fontSize: '0.78rem',
    padding: '3px 6px',
    height: 'auto',
    marginBottom: 0,
  }

  return (
    <>
      <header className="grid">
        <h2 style={{ margin: 0 }}>Applications
          {!loading && <small style={{ fontWeight: 400, fontSize: '0.85rem', marginLeft: '0.5rem', color: 'var(--pico-muted-color)' }}>
            {totalElements} total
          </small>}
        </h2>
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '.4rem' }}>
          {selected.size > 0 && (
            <button
              type="button"
              style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0, background: '#ef4444', border: 'none', color: '#fff' }}
              disabled={bulkDeleting}
              onClick={handleBulkDelete}
            >
              {bulkDeleting ? 'Deleting…' : `Delete ${selected.size} selected`}
            </button>
          )}
          <div style={{ position: 'relative', display: 'inline-flex' }} className="csv-tooltip-wrap">
            <button type="button" style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0 }} onClick={openImportModal}>Import CSV</button>
            <div className="csv-tooltip">
              Expected header (first row):<br />
              <code>Company, Role, Location, Salary Range, Apply Date, Final Status, Job Link, Resume Uploaded, Call, Reject Date, Login Details, Days pending</code>
            </div>
          </div>
          <Link to="/applications/new" style={{ display: 'inline-flex' }}>
            <button className="contrast" style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0 }}>+ New</button>
          </Link>
        </div>
        <style>{`
          .csv-tooltip-wrap .csv-tooltip {
            display: none;
            position: absolute;
            top: calc(100% + 6px);
            right: 0;
            background: var(--pico-card-background-color, #1e2535);
            border: 1px solid var(--pico-muted-border-color, #374151);
            border-radius: 6px;
            padding: 8px 10px;
            font-size: 0.72rem;
            line-height: 1.5;
            white-space: normal;
            width: 480px;
            max-width: 92vw;
            z-index: 999;
            color: var(--pico-color);
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          }
          .csv-tooltip-wrap .csv-tooltip code {
            display: block;
            margin-top: 4px;
            font-size: 0.69rem;
            color: var(--pico-primary);
            word-break: break-word;
            white-space: normal;
          }
          .csv-tooltip-wrap:hover .csv-tooltip {
            display: block;
          }
        `}</style>
      </header>

      {/* CSV Import Modal */}
      {showImport && (
        <div
          style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}
          onClick={e => { if (e.target === e.currentTarget) closeImportModal() }}
        >
          <article style={{ minWidth: '22rem', maxWidth: '32rem', width: '100%', margin: '1rem' }}>
            <header><strong>Import from CSV</strong></header>
            <label>
              Select .csv file
              <input ref={fileInputRef} type="file" accept=".csv"
                onChange={e => { setImportFile(e.target.files?.[0] ?? null); setImportResult(null); setImportError(null) }} />
            </label>
            {importError && <p><small style={{ color: '#ef4444' }}>{importError}</small></p>}
            {importResult && (
              <div>
                <p>
                  <strong style={{ color: '#22c55e' }}>✓ {importResult.imported} imported</strong>
                  {importResult.failed > 0 && <span style={{ color: '#ef4444', marginLeft: '1rem' }}>✗ {importResult.failed} failed</span>}
                </p>
                {importResult.errors.length > 0 && (
                  <ul style={{ fontSize: '0.8rem', maxHeight: '8rem', overflowY: 'auto' }}>
                    {importResult.errors.map((err, i) => <li key={i}>{err}</li>)}
                  </ul>
                )}
              </div>
            )}
            <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: '.5rem' }}>
              <button type="button" className="secondary" onClick={closeImportModal}>Close</button>
              <button type="button" className="contrast" disabled={!importFile || importing} onClick={handleImport}>
                {importing ? 'Importing…' : 'Import'}
              </button>
            </footer>
          </article>
        </div>
      )}

      {/* Filters */}
      <details open>
        <summary style={{ fontSize: '0.82rem', padding: '4px 0', marginBottom: '0.4rem' }}>Filters</summary>
        <div className="grid" style={{ fontSize: '0.78rem', gap: '0.4rem', alignItems: 'end', marginBottom: '0.5rem' }}>
          <label style={{ marginBottom: 0 }}>
            <small>Status</small>
            <select style={filterInput} value={status} onChange={e => setStatus(e.target.value as AppStatus | 'ALL')}>
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
          <label style={{ marginBottom: 0 }}>
            <small>Search</small>
            <input style={filterInput} placeholder="Company or role" value={search} onChange={e => setSearch(e.target.value)} />
          </label>
          <label style={{ marginBottom: 0 }}>
            <small>Month</small>
            <select style={filterInput} value={month} onChange={e => setMonth(e.target.value)}>
              <option value="">All</option>
              <option value="1">Jan</option>
              <option value="2">Feb</option>
              <option value="3">Mar</option>
              <option value="4">Apr</option>
              <option value="5">May</option>
              <option value="6">Jun</option>
              <option value="7">Jul</option>
              <option value="8">Aug</option>
              <option value="9">Sep</option>
              <option value="10">Oct</option>
              <option value="11">Nov</option>
              <option value="12">Dec</option>
            </select>
          </label>
          <label style={{ marginBottom: 0 }}>
            <small>Year</small>
            <select style={filterInput} value={year} onChange={e => setYear(e.target.value)}>
              <option value="">All</option>
              <option value="2023">2023</option>
              <option value="2024">2024</option>
              <option value="2025">2025</option>
              <option value="2026">2026</option>
            </select>
          </label>
          <label style={{ marginBottom: 0 }}>
            <small>Got Call</small>
            <select style={filterInput} value={gotCall} onChange={e => setGotCall(e.target.value)}>
              <option value="">All</option>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </select>
          </label>
          <label style={{ marginBottom: 0 }}>
            <small>Sort By</small>
            <select style={filterInput} value={sortBy} onChange={e => setSortBy(e.target.value)}>
              <option value="appliedAt">Apply Date</option>
              <option value="createdAt">Date Added</option>
            </select>
          </label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
            <small style={{ visibility: 'hidden' }}>_</small>
            <div style={{ display: 'flex', alignItems: 'center', gap: '.4rem' }}>
              <button type="button" style={{ padding: '3px 9px', fontSize: '0.76rem', marginBottom: 0 }} onClick={() => { setStatus('ALL'); setSearch(''); setMonth(''); setYear(''); setGotCall(''); setSortBy('appliedAt') }}>Clear</button>
              <small style={{ color: 'var(--pico-muted-color)' }}><code>{USE_MOCK ? 'mock' : 'API'}</code></small>
            </div>
          </div>
        </div>
      </details>

      {toast && <article><p>{toast}</p></article>}
      {loading && <article aria-busy="true"><p>Loading…</p></article>}
      {error && <article><header>Error</header><p>{error}</p></article>}

      {!loading && !error && (
        <>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ fontSize: '0.78rem', width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ ...thStyle, width: '2rem' }}>
                    <input
                      type="checkbox"
                      style={{ margin: 0 }}
                      checked={rows.length > 0 && rows.every(r => selected.has(r.id))}
                      onChange={e => setSelected(e.target.checked ? new Set(rows.map(r => r.id)) : new Set())}
                    />
                  </th>
                  <th style={thStyle}>Company</th>
                  <th style={thStyle}>Role</th>
                  <th style={thStyle}>Status</th>
                  <th style={thStyle}>Location</th>
                  <th style={thStyle}>Applied</th>
                  <th style={thStyle}>Salary</th>
                  <th style={thStyle}>Call</th>
                  <th style={{ ...thStyle, textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map(r => (
                  <tr key={r.id}>
                    <td style={{ ...cellStyle, maxWidth: 'none' }}>
                      <input
                        type="checkbox"
                        style={{ margin: 0 }}
                        checked={selected.has(r.id)}
                        onChange={e => setSelected(prev => {
                          const next = new Set(prev)
                          e.target.checked ? next.add(r.id) : next.delete(r.id)
                          return next
                        })}
                      />
                    </td>
                    <td style={{ ...cellStyle, fontWeight: 500 }}>{r.company}</td>
                    <td style={roleCell}>{r.role}</td>
                    <td style={{ ...cellStyle, maxWidth: 'none' }}><StatusBadge status={r.status} /></td>
                    <td style={cellStyle}>{r.location ?? '—'}</td>
                    <td style={cellStyle}>{r.appliedAt ? fmtDate(r.appliedAt) : '—'}</td>
                    <td style={cellStyle}>{fmtSalary(r.salaryMin, r.salaryMax, r.currency)}</td>
                    <td style={{ ...cellStyle, maxWidth: 'none' }}>{r.gotCall ? '✓' : '—'}</td>
                    <td style={{ ...cellStyle, textAlign: 'right', maxWidth: 'none' }}>
                      <Link to={`/applications/${r.id}`}>
                        <button style={{ padding: '2px 8px', fontSize: '0.75rem' }}>View</button>
                      </Link>{' '}
                      <button
                        className="secondary"
                        style={{ padding: '2px 8px', fontSize: '0.75rem' }}
                        disabled={deletingId === r.id}
                        onClick={() => {
                          if (confirm(`Delete "${r.company} — ${r.role}"?`))
                            handleDelete(r.id, `${r.company} — ${r.role}`)
                        }}
                      >
                        {deletingId === r.id ? '…' : 'Del'}
                      </button>
                    </td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr><td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: 'var(--pico-muted-color)' }}>No results</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem', fontSize: '0.85rem' }}>
              <button
                style={{ padding: '3px 10px' }}
                disabled={page === 0}
                onClick={() => setPage(p => Math.max(0, p - 1))}
              >
                ‹ Prev
              </button>
              {pageRange(page, totalPages).map((p, i) =>
                p === '…'
                  ? <span key={`ellipsis-${i}`} style={{ padding: '0 4px', color: 'var(--pico-muted-color)' }}>…</span>
                  : <button
                      key={p}
                      style={{ padding: '3px 9px', minWidth: '2rem', fontWeight: page === p ? 700 : 400 }}
                      className={page === p ? 'contrast' : 'secondary'}
                      onClick={() => setPage(p as number)}
                    >
                      {(p as number) + 1}
                    </button>
              )}
              <button
                style={{ padding: '3px 10px' }}
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              >
                Next ›
              </button>
            </div>
          )}
        </>
      )}
    </>
  )
}

function isValidStatus(s: string): boolean {
  return STATUSES.includes(s as AppStatus | 'ALL')
}

function fmtDate(iso: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  return isNaN(d.getTime()) ? iso : d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: '2-digit' })
}

function fmtSalary(min?: number, max?: number, currency?: string): string {
  if (!min && !max) return '—'
  const fmt = (n: number) => n >= 1000 ? `${Math.round(n / 1000)}k` : String(n)
  const cur = currency && currency !== 'USD' ? currency + ' ' : ''
  if (min && max) return `${cur}${fmt(min)}–${fmt(max)}`
  if (min) return `${cur}${fmt(min)}+`
  return `${cur}up to ${fmt(max!)}`
}

/** Produce page numbers + ellipsis for the pagination bar */
function pageRange(current: number, total: number): (number | '…')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages: (number | '…')[] = []
  const add = (n: number) => { if (!pages.includes(n)) pages.push(n) }
  add(0)
  if (current > 2) pages.push('…')
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) add(i)
  if (current < total - 3) pages.push('…')
  add(total - 1)
  return pages
}
