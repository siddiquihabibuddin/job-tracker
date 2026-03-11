import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import type { TrendPoint } from '../../api/stats'

interface WeeklyTrendChartProps {
  trendData: TrendPoint[] | undefined
  trendLoading: boolean
}

function fmtWeek(s: string) {
  return new Date(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export default function WeeklyTrendChart({ trendData, trendLoading }: WeeklyTrendChartProps) {
  return (
    <article style={{ padding: '0.65rem', marginBottom: 0 }}>
      <header style={{ fontSize: '0.78rem', fontWeight: 600, marginBottom: '0.5rem' }}>
        Applications &amp; Calls per Week (last 12 weeks)
      </header>
      {trendLoading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', padding: '1rem 0' }}>
          {[100, 80, 90].map(w => (
            <div
              key={w}
              aria-hidden="true"
              style={{
                height: '0.85rem',
                borderRadius: '4px',
                background: 'var(--pico-muted-border-color)',
                width: `${w}%`,
                animation: 'pulse 1.5s ease-in-out infinite',
              }}
            />
          ))}
        </div>
      )}
      {!trendLoading && trendData && trendData.length === 0 && (
        <p style={{ fontSize: '0.78rem', color: 'var(--pico-muted-color)', margin: '1rem 0' }}>
          No trend data available yet.
        </p>
      )}
      {!trendLoading && trendData && trendData.length > 0 && (
        <div style={{ width: '100%', height: 200 }}>
          <ResponsiveContainer>
            <LineChart
              data={trendData.map(p => ({
                week: fmtWeek(p.start),
                Applications: p.count,
                'Calls Received': p.callsCount ?? 0,
              }))}
              margin={{ top: 4, right: 8, left: -16, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="week" tick={{ fontSize: 10 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: '0.78rem' }} />
              <Line
                type="monotone"
                dataKey="Applications"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
              <Line
                type="monotone"
                dataKey="Calls Received"
                stroke="#22c55e"
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </article>
  )
}
