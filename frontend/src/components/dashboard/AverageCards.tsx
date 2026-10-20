import type { Averages } from '../../api/stats'

interface AverageCardsProps {
  averages: Averages | undefined
  loading: boolean
}

export default function AverageCards({ averages, loading }: AverageCardsProps) {
  const cards = [
    { label: 'Avg / day', value: averages?.dailyAverage, sub: `last ${(averages?.weeks ?? 12) * 7} days (full weeks)` },
    { label: 'Avg / week', value: averages?.weeklyAverage, sub: `last ${averages?.weeks ?? 12} full weeks` },
    { label: 'Avg / month', value: averages?.monthlyAverage, sub: `last ${averages?.months ?? 12} full months` },
  ]

  return (
    <div className="grid" style={{ marginBottom: '0.5rem' }}>
      {cards.map(({ label, value, sub }) => (
        <article key={label} style={{ textAlign: 'center', padding: '0.4rem 0.35rem' }}>
          <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>{label}</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 700, lineHeight: 1 }}>{loading || value === undefined ? '—' : value.toFixed(1)}</div>
          <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', marginTop: '0.2rem' }}>{sub}</div>
        </article>
      ))}
    </div>
  )
}
