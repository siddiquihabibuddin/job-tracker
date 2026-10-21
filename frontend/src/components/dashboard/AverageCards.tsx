import type { Averages } from '../../api/stats'

interface AverageCardsProps {
  averages: Averages | undefined
  loading: boolean
}

const fmtDay = (iso: string, withYear: boolean) =>
  new Date(iso + 'T00:00:00').toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', ...(withYear ? { year: 'numeric' } : {}),
  })

function periodLabel(a: Averages): string {
  if (a.total === 0 && a.periodEnd < a.periodStart) return 'No applications yet'
  const count = `${a.total} application${a.total === 1 ? '' : 's'}`
  if (a.groupBy === 'year') {
    const since = new Date(a.periodStart + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', year: 'numeric' })
    return `All time since ${since} · ${count}`
  }
  return `${fmtDay(a.periodStart, false)} – ${fmtDay(a.periodEnd, true)} · ${count}`
}

export default function AverageCards({ averages, loading }: AverageCardsProps) {
  const sub = averages ? periodLabel(averages) : ''
  const cards = [
    { label: 'Avg / day', value: averages?.dailyAverage },
    { label: 'Avg / week', value: averages?.weeklyAverage },
    { label: 'Avg / month', value: averages?.monthlyAverage },
  ]

  return (
    <div className="grid" style={{ marginBottom: '0.5rem' }}>
      {cards.map(({ label, value }) => (
        <article key={label} style={{ textAlign: 'center', padding: '0.4rem 0.35rem' }}>
          <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>{label}</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 700, lineHeight: 1 }}>{loading || value === undefined ? '—' : value.toFixed(1)}</div>
          <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', marginTop: '0.2rem' }}>{loading ? '' : sub}</div>
        </article>
      ))}
    </div>
  )
}
