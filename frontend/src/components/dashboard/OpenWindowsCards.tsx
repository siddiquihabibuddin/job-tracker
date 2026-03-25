import type { BreakdownResponse } from '../../api/stats'

interface OpenWindowsCardsProps {
  windows: BreakdownResponse['openWindows'] | undefined
  loading: boolean
}

export default function OpenWindowsCards({ windows, loading }: OpenWindowsCardsProps) {
  const cards = [
    { label: 'Today',     value: windows?.today     ?? 0 },
    { label: 'Yesterday', value: windows?.yesterday ?? 0 },
    { label: 'Last 7d',  value: windows?.last7d    ?? 0 },
    { label: 'Last 15d', value: windows?.last15d ?? 0 },
    { label: 'Last 30d', value: windows?.last30d ?? 0 },
    { label: 'Last 3mo', value: windows?.last3m  ?? 0 },
    { label: 'Last 6mo', value: windows?.last6m  ?? 0 },
    { label: 'Last 9mo', value: windows?.last9m  ?? 0 },
    { label: 'Last 1yr', value: windows?.last1y  ?? 0 },
  ]

  return (
    <div className="grid" style={{ marginBottom: '0.5rem' }}>
      {cards.map(({ label, value }) => (
        <article key={label} style={{ textAlign: 'center', padding: '0.4rem 0.35rem' }}>
          <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>{label}</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 700, lineHeight: 1 }}>{loading ? '—' : value}</div>
        </article>
      ))}
    </div>
  )
}
