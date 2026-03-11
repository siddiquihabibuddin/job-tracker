import type { RoleCountsResponse } from '../../api/stats'

interface RoleKpiCardsProps {
  roleData: RoleCountsResponse | null
  groupBy: 'month' | 'year'
  year: number
  loading: boolean
  error: string | null
}

export default function RoleKpiCards({ roleData, groupBy, year, loading, error }: RoleKpiCardsProps) {
  if (!roleData || loading || error) return null

  const roleRows = roleData.rows
  const totals = roleRows.reduce(
    (acc, r) => ({ eng: acc.eng + r.engineerDev, mgr: acc.mgr + r.manager, arc: acc.arc + r.architect, oth: acc.oth + r.other }),
    { eng: 0, mgr: 0, arc: 0, oth: 0 }
  )

  return (
    <>
      <header style={{ fontSize: '0.78rem', fontWeight: 600, margin: 0 }}>
        Role Breakdown {groupBy === 'month' ? `(${year})` : '(all years)'}
      </header>
      <div className="grid" style={{ marginBottom: 0 }}>
        {[
          { label: 'Eng / Dev',  value: totals.eng, color: '#6366f1' },
          { label: 'Manager',    value: totals.mgr, color: '#f59e0b' },
          { label: 'Architect',  value: totals.arc, color: '#10b981' },
          { label: 'Other',      value: totals.oth, color: '#6b7280' },
        ].map(({ label, value, color }) => (
          <article key={label} style={{ textAlign: 'center', padding: '0.4rem 0.35rem', marginBottom: 0 }}>
            <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>{label}</div>
            <div style={{ fontSize: '1.4rem', fontWeight: 700, lineHeight: 1, color }}>{value}</div>
          </article>
        ))}
      </div>
    </>
  )
}
