import type { RoleCountsResponse } from '../../api/stats'
import { thStyle, tdStyle } from './styles'

interface RoleBreakdownTableProps {
  roleData: RoleCountsResponse | null
  groupBy: 'month' | 'year'
  loading: boolean
  error: string | null
}

export default function RoleBreakdownTable({ roleData, groupBy, loading, error }: RoleBreakdownTableProps) {
  if (!roleData || loading || error) return null

  const roleRows = roleData.rows
  const totals = roleRows.reduce(
    (acc, r) => ({ eng: acc.eng + r.engineerDev, mgr: acc.mgr + r.manager, arc: acc.arc + r.architect, oth: acc.oth + r.other }),
    { eng: 0, mgr: 0, arc: 0, oth: 0 }
  )
  const hasAnyRole = totals.eng + totals.mgr + totals.arc + totals.oth > 0

  return (
    <article style={{ padding: '0', marginBottom: 0 }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.78rem' }}>
        <thead>
          <tr>
            <th style={thStyle}>{groupBy === 'month' ? 'Month' : 'Year'}</th>
            <th style={{ ...thStyle, textAlign: 'right', color: '#6366f1' }}>Eng / Dev</th>
            <th style={{ ...thStyle, textAlign: 'right', color: '#f59e0b' }}>Mgr</th>
            <th style={{ ...thStyle, textAlign: 'right', color: '#10b981' }}>Arch</th>
            <th style={{ ...thStyle, textAlign: 'right', color: '#6b7280' }}>Other</th>
          </tr>
        </thead>
        <tbody>
          {roleRows.filter(r => r.engineerDev > 0 || r.manager > 0 || r.architect > 0 || r.other > 0).map(r => (
            <tr key={r.periodNum}>
              <td style={tdStyle}>{r.label}</td>
              <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.engineerDev}</td>
              <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.manager}</td>
              <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.architect}</td>
              <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.other}</td>
            </tr>
          ))}
          {!hasAnyRole && (
            <tr>
              <td colSpan={5} style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--pico-muted-color)' }}>
                No data for this period
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </article>
  )
}
