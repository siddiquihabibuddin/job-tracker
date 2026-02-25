import { useEffect, useState } from 'react'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, BarChart, Bar, XAxis, YAxis, CartesianGrid, LineChart, Line } from 'recharts'
import { fetchMockSummary, fetchMockTrend, type StatsSummary, type TrendPoint } from '../mocks/stats'
import { getSummary as apiGetSummary, getTrend as apiGetTrend } from '../api/stats'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

// neutral palette that works in light/dark
const COLORS = ['#89cff0','#7dd3fc','#93c5fd','#a5b4fc','#c4b5fd','#fca5a5','#fbbf24','#34d399']

export default function Dashboard() {
  const [sum7, setSum7] = useState<StatsSummary | null>(null)
  const [sum30, setSum30] = useState<StatsSummary | null>(null)
  const [trend, setTrend] = useState<TrendPoint[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    async function load() {
      setLoading(true); setError(null)
      try {
        const [s7, s30, t] = await Promise.all([
          USE_MOCK ? fetchMockSummary(7) : apiGetSummary(7),
          USE_MOCK ? fetchMockSummary(30) : apiGetSummary(30),
          USE_MOCK ? fetchMockTrend(12) : apiGetTrend(12),
        ])
        if (!alive) return
        setSum7(s7); setSum30(s30); setTrend(t)
      } catch (e: any) {
        if (alive) setError(e?.message || 'Failed to load dashboard')
      } finally {
        if (alive) setLoading(false)
      }
    }
    load()
    return () => { alive = false }
  }, [])

  if (loading) return <article><header>Loading</header><p>Loading dashboard…</p></article>
  if (error) return <article><header>Error</header><p>{error}</p></article>

  const statusData = sum30 ? Object.entries(sum30.byStatus).map(([key, value]) => ({ name: key, value })) : []
  const sourceData = sum30 ? Object.entries(sum30.bySource).map(([key, value]) => ({ name: key, value })) : []
  const trendData = trend?.map(p => ({ week: `${p.start.slice(5)}–${p.end.slice(5)}`, count: p.count })) || []

  return (
    <>
      <h2>Dashboard</h2>

      <div className="grid">
        <article>
          <header>Applied (7d)</header>
          <p style={{ fontSize: '2rem', margin: 0 }}>{sum7?.totalApplied ?? '—'}</p>
          <small>Source: <code>{USE_MOCK ? 'mock' : 'API'}</code></small>
        </article>
        <article>
          <header>Applied (30d)</header>
          <p style={{ fontSize: '2rem', margin: 0 }}>{sum30?.totalApplied ?? '—'}</p>
          <small>Updated: {sum30 ? new Date(sum30.generatedAt).toLocaleString() : '—'}</small>
        </article>
      </div>

      <div className="grid">
        <article>
          <header>By Status (30d)</header>
          <div style={{ width: '100%', height: 240 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie dataKey="value" data={statusData} outerRadius={90} label>
                  {statusData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </article>

        <article>
          <header>By Source (30d)</header>
          <div style={{ width: '100%', height: 240 }}>
            <ResponsiveContainer>
              <BarChart data={sourceData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Legend />
                <Bar dataKey="value">
                  {sourceData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </article>
      </div>

      <article style={{ marginTop: '1rem' }}>
        <header>Trend (weekly, 12 weeks)</header>
        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer>
            <LineChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="week" interval={1} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="count" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </article>
    </>
  )
}