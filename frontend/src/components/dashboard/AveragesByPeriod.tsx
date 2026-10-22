import type { Averages, AveragePeriod } from '../../api/stats'
import { thStyle, tdStyle } from './styles'

interface AveragesByPeriodProps {
  averages: Averages | undefined
  loading: boolean
}

export default function AveragesByPeriod({ averages, loading }: AveragesByPeriodProps) {
  const periods = averages?.periods
  if (loading || !periods || periods.length === 0) return null

  const byYear = averages.groupBy === 'year'
  const hasPartial = periods.some(p => p.partial)
  const fmt = (p: AveragePeriod, v: number) => (p.future ? '—' : v.toFixed(1))

  const rows: { label: string; value: (p: AveragePeriod) => string }[] = [
    { label: 'Applications', value: p => (p.future ? '—' : String(p.total)) },
    { label: 'Avg / day', value: p => fmt(p, p.dailyAverage) },
    { label: 'Avg / week', value: p => fmt(p, p.weeklyAverage) },
    ...(byYear ? [{ label: 'Avg / month', value: (p: AveragePeriod) => fmt(p, p.monthlyAverage) }] : []),
  ]

  return (
    <article style={{ padding: '0.4rem 0.5rem', marginBottom: '0.5rem', overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.78rem' }}>
        <thead>
          <tr>
            <th style={thStyle}>{byYear ? 'By year' : `By month (${averages.year})`}</th>
            {periods.map(p => (
              <th key={p.periodNum} style={{ ...thStyle, textAlign: 'right', color: p.partial ? '#3b82f6' : undefined }}>
                {p.label}{p.partial ? '*' : ''}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map(r => (
            <tr key={r.label}>
              <td style={{ ...tdStyle, color: 'var(--pico-muted-color)' }}>{r.label}</td>
              {periods.map(p => (
                <td key={p.periodNum} style={{ ...tdStyle, textAlign: 'right', fontWeight: 500, color: p.future ? 'var(--pico-muted-color)' : undefined }}>
                  {r.value(p)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {hasPartial && (
        <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', marginTop: '0.2rem' }}>
          * in progress — averaged over the days elapsed so far
        </div>
      )}
    </article>
  )
}
