import { Link } from 'react-router-dom'
import type { Application } from '../../api/applications'
import StatusBadge from './StatusBadge'
import { cellStyle, roleCell, thStyle } from './styles'
import { fmtDate, fmtSalary, pageRange } from './utils'

interface Props {
  rows: Application[]
  loading: boolean
  error: string | null
  selected: Set<string>
  page: number
  totalPages: number
  deletingId: string | null
  onSelectAll: (checked: boolean) => void
  onToggleSelect: (id: string) => void
  onDelete: (id: string, label: string) => void
  onPageChange: (p: number) => void
}

export default function ApplicationsTable({
  rows,
  loading,
  error,
  selected,
  page,
  totalPages,
  deletingId,
  onSelectAll,
  onToggleSelect,
  onDelete,
  onPageChange,
}: Props) {
  if (loading || error) return null

  return (
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
                  onChange={e => onSelectAll(e.target.checked)}
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
                    onChange={() => onToggleSelect(r.id)}
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
                        onDelete(r.id, `${r.company} — ${r.role}`)
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

      {totalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', marginTop: '1rem', fontSize: '0.85rem' }}>
          <button
            style={{ padding: '3px 10px' }}
            disabled={page === 0}
            onClick={() => onPageChange(Math.max(0, page - 1))}
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
                  onClick={() => onPageChange(p as number)}
                >
                  {(p as number) + 1}
                </button>
          )}
          <button
            style={{ padding: '3px 10px' }}
            disabled={page >= totalPages - 1}
            onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          >
            Next ›
          </button>
        </div>
      )}
    </>
  )
}
