import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import type { BreakdownResponse } from '../../api/stats'
import { thStyle, tdStyle } from './styles'

interface AppliedVsRejectedChartProps {
  chartData: { name: string; Applied: number; Rejected: number }[]
  rows: BreakdownResponse['rows']
  groupBy: 'month' | 'year'
  year: number
}

export default function AppliedVsRejectedChart({ chartData, rows, groupBy, year }: AppliedVsRejectedChartProps) {
  return (
    <>
      {/* Bar chart */}
      <article style={{ padding: '0.65rem', marginBottom: 0 }}>
        <header style={{ fontSize: '0.78rem', fontWeight: 600, marginBottom: '0.5rem' }}>
          Applied vs Rejected {groupBy === 'month' ? `(${year})` : '(all years)'}
        </header>
        <div style={{ width: '100%', height: 200 }}>
          <ResponsiveContainer>
            <BarChart data={chartData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 10 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
              <Tooltip />
              <Legend wrapperStyle={{ fontSize: '0.78rem' }} />
              <Bar dataKey="Applied" fill="#3b82f6" radius={[3, 3, 0, 0]} maxBarSize={40} />
              <Bar dataKey="Rejected" fill="#ef4444" radius={[3, 3, 0, 0]} maxBarSize={40} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </article>

      {/* Summary table */}
      <article style={{ padding: '0', marginBottom: 0 }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.78rem' }}>
          <thead>
            <tr>
              <th style={thStyle}>{groupBy === 'month' ? 'Month' : 'Year'}</th>
              <th style={{ ...thStyle, textAlign: 'right', color: '#3b82f6' }}>Applied</th>
              <th style={{ ...thStyle, textAlign: 'right', color: '#ef4444' }}>Rejected</th>
              <th style={{ ...thStyle, textAlign: 'right', color: '#10b981' }}>Open</th>
            </tr>
          </thead>
          <tbody>
            {rows.filter(r => r.totalApplied > 0 || r.totalRejected > 0 || r.totalOpen > 0).map(r => (
              <tr key={r.periodNum}>
                <td style={tdStyle}>{r.label}</td>
                <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.totalApplied}</td>
                <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.totalRejected}</td>
                <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 500 }}>{r.totalOpen}</td>
              </tr>
            ))}
            {rows.every(r => r.totalApplied === 0 && r.totalRejected === 0 && r.totalOpen === 0) && (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', padding: '1.5rem', color: 'var(--pico-muted-color)' }}>
                  No data for this period
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </article>
    </>
  )
}
