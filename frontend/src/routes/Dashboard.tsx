import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
} from 'recharts'
import { Link } from 'react-router-dom'
import { getBreakdown, getInsights, getRoleCounts, getStaleApps, getTrend, type BreakdownResponse, type RoleCountsResponse, type StaleApp, type TrendPoint } from '../api/stats'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const CURRENT_YEAR = new Date().getFullYear()
const YEARS = [2023, 2024, 2025, 2026]

export default function Dashboard() {
  const [groupBy, setGroupBy] = useState<'month' | 'year'>('month')
  const [year, setYear] = useState(CURRENT_YEAR)
  const [data, setData] = useState<BreakdownResponse | null>(null)
  const [roleData, setRoleData] = useState<RoleCountsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchData = (signal?: AbortSignal) => {
    if (USE_MOCK) { setLoading(false); return }
    setLoading(true); setError(null)
    const yearParam = groupBy === 'month' ? year : undefined
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone
    Promise.all([
      getBreakdown(groupBy, yearParam, tz),
      getRoleCounts(groupBy, yearParam),
    ])
      .then(([d, r]) => { if (!signal?.aborted) { setData(d); setRoleData(r); setLoading(false) } })
      .catch((e: unknown) => {
        if (!signal?.aborted) {
          setError(e instanceof Error ? e.message : 'Failed to load')
          setLoading(false)
        }
      })
  }

  useEffect(() => {
    const controller = new AbortController()
    fetchData(controller.signal)
    return () => controller.abort()
  }, [groupBy, year]) // eslint-disable-line react-hooks/exhaustive-deps

  const {
    data: insightsData,
    isLoading: insightsLoading,
    isError: insightsError,
    isFetching: insightsFetching,
    refetch: refetchInsights,
  } = useQuery({
    queryKey: ['insights'],
    queryFn: getInsights,
    staleTime: 30 * 60 * 1000,
    retry: 1,
    enabled: !USE_MOCK,
  })

  const { data: staleApps } = useQuery<StaleApp[]>({
    queryKey: ['stale-apps'],
    queryFn: getStaleApps,
    enabled: !USE_MOCK,
    staleTime: 60_000,
  })

  const { data: trendData, isLoading: trendLoading } = useQuery<TrendPoint[]>({
    queryKey: ['trend', '12'],
    queryFn: () => getTrend('week', 12),
    enabled: !USE_MOCK,
    staleTime: 5 * 60 * 1000,
  })

  const GHOST_PAGE_SIZE = 8
  const [ghostPage, setGhostPage] = useState(0)
  const ghostTotalPages = staleApps ? Math.ceil(staleApps.length / GHOST_PAGE_SIZE) : 0
  const ghostPagedApps = staleApps?.slice(ghostPage * GHOST_PAGE_SIZE, (ghostPage + 1) * GHOST_PAGE_SIZE) ?? []

  const windows = data?.openWindows
  const rows = data?.rows ?? []
  const chartData = rows.map(r => ({ name: r.label, Applied: r.totalApplied, Rejected: r.totalRejected }))

  const fmtWeek = (s: string) =>
    new Date(s).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })

  const thStyle: React.CSSProperties = {
    fontSize: '0.72rem', fontWeight: 600, padding: '3px 6px',
    textTransform: 'uppercase', letterSpacing: '0.05em', textAlign: 'left',
  }
  const tdStyle: React.CSSProperties = { fontSize: '0.78rem', padding: '3px 6px' }

  return (
    <>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
        <h2 style={{ margin: 0 }}>Dashboard</h2>
        <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
          <div role="group" style={{ display: 'flex' }}>
            <button
              style={{ padding: '4px 12px', fontSize: '0.8rem', borderRadius: '4px 0 0 4px', marginBottom: 0 }}
              className={groupBy === 'month' ? 'contrast' : 'secondary'}
              onClick={() => setGroupBy('month')}
            >By Month</button>
            <button
              style={{ padding: '4px 12px', fontSize: '0.8rem', borderRadius: '0 4px 4px 0', marginBottom: 0 }}
              className={groupBy === 'year' ? 'contrast' : 'secondary'}
              onClick={() => setGroupBy('year')}
            >By Year</button>
          </div>
          {groupBy === 'month' && (
            <select
              value={year}
              onChange={e => setYear(Number(e.target.value))}
              style={{ padding: '4px 8px', fontSize: '0.8rem', width: 'auto', marginBottom: 0 }}
            >
              {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          )}
        </div>
      </div>

      {/* Open Windows KPI cards */}
      <div className="grid" style={{ marginBottom: '0.5rem' }}>
        {[
          { label: 'Today',    value: windows?.today   ?? 0 },
          { label: 'Last 7d',  value: windows?.last7d  ?? 0 },
          { label: 'Last 15d', value: windows?.last15d ?? 0 },
          { label: 'Last 30d', value: windows?.last30d ?? 0 },
          { label: 'Last 3mo', value: windows?.last3m  ?? 0 },
          { label: 'Last 6mo', value: windows?.last6m  ?? 0 },
          { label: 'Last 9mo', value: windows?.last9m  ?? 0 },
          { label: 'Last 1yr', value: windows?.last1y  ?? 0 },
        ].map(({ label, value }) => (
          <article key={label} style={{ textAlign: 'center', padding: '0.4rem 0.35rem' }}>
            <div style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.2rem' }}>{label}</div>
            <div style={{ fontSize: '1.4rem', fontWeight: 700, lineHeight: 1 }}>{loading ? '—' : value}</div>
          </article>
        ))}
      </div>
      <p style={{ fontSize: '0.7rem', color: 'var(--pico-muted-color)', marginBottom: '1rem', marginTop: 0 }}>Open applications (excl. Rejected / Accepted / Withdrawn)</p>

      {error && <article><header>Error</header><p>{error}</p></article>}
      {loading && <article aria-busy="true"><p>Loading…</p></article>}

      {/* 3-column main grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', alignItems: 'start' }}>

        {/* Col 1: Applied vs Rejected chart + Summary table */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {!loading && !error && (
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
          )}
        </div>

        {/* Col 2: Weekly Trend chart + Role table */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {/* Weekly trend chart */}
          {!USE_MOCK && (
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
          )}

          {/* Role breakdown table */}
          {!loading && !error && roleData && (() => {
            const roleRows = roleData.rows
            const totals = roleRows.reduce(
              (acc, r) => ({ eng: acc.eng + r.engineerDev, mgr: acc.mgr + r.manager, arc: acc.arc + r.architect, oth: acc.oth + r.other }),
              { eng: 0, mgr: 0, arc: 0, oth: 0 }
            )
            const hasAnyRole = totals.eng + totals.mgr + totals.arc + totals.oth > 0
            return (
              <article style={{ padding: '0', marginBottom: 0 }}>
                <header style={{ fontSize: '0.78rem', fontWeight: 600, padding: '0.65rem 0.65rem 0.3rem' }}>
                  Role Breakdown {groupBy === 'month' ? `(${year})` : '(all years)'}
                </header>
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
          })()}
        </div>

        {/* Col 3: Role KPI cards + AI Insights + Ghosting */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {/* Role KPI totals */}
          {!loading && !error && roleData && (() => {
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
          })()}

          {/* AI Insights card */}
          {!USE_MOCK && (
            <article style={{ padding: '0.65rem', marginBottom: 0 }}>
              <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>AI Insights</span>
                <button
                  style={{ padding: '3px 10px', fontSize: '0.75rem', marginBottom: 0 }}
                  className="secondary"
                  onClick={() => refetchInsights()}
                  disabled={insightsFetching}
                  aria-busy={insightsFetching}
                >
                  {insightsFetching ? 'Generating…' : 'Refresh'}
                </button>
              </header>

              {insightsLoading && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {[80, 65, 72].map(w => (
                    <div key={w} aria-hidden="true" style={{ height: '0.9rem', borderRadius: '4px', background: 'var(--pico-muted-border-color)', width: `${w}%`, animation: 'pulse 1.5s ease-in-out infinite' }} />
                  ))}
                </div>
              )}

              {!insightsLoading && insightsError && (
                <p style={{ fontSize: '0.78rem', color: 'var(--pico-muted-color)', margin: 0 }}>
                  Could not load insights. <button style={{ padding: 0, background: 'none', border: 'none', color: 'var(--pico-primary)', cursor: 'pointer', fontSize: '0.78rem' }} onClick={() => refetchInsights()}>Retry</button>
                </p>
              )}

              {!insightsLoading && !insightsError && insightsData && (
                <>
                  <ul style={{ margin: 0, paddingLeft: '1.2rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                    {insightsData.insights.map((insight, i) => (
                      <li key={i} style={{ fontSize: '0.78rem', lineHeight: 1.5 }}>{insight}</li>
                    ))}
                  </ul>
                  <p style={{ fontSize: '0.65rem', color: 'var(--pico-muted-color)', margin: '0.6rem 0 0' }}>
                    Generated at {new Date(insightsData.generatedAt).toLocaleTimeString()}
                  </p>
                </>
              )}
            </article>
          )}

          {/* Ghosting tracker */}
          {!USE_MOCK && staleApps && staleApps.length > 0 && (
            <article style={{ padding: '0.65rem', marginBottom: 0, borderLeft: '3px solid #f59e0b' }}>
              <header style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>Possibly Ghosting ({staleApps.length})</span>
                <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem' }}>No updates in 14+ days</small>
              </header>
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {ghostPagedApps.map(app => (
                  <li key={app.appId} style={{ padding: '0.3rem 0', borderBottom: '1px solid var(--pico-muted-border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Link to={`/applications/${app.appId}`} style={{ fontSize: '0.78rem', fontWeight: 500 }}>
                      {app.company} — {app.role}
                    </Link>
                    <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem', whiteSpace: 'nowrap', marginLeft: '0.75rem' }}>
                      {app.status} · {app.appliedAt ?? '—'} · {app.daysSinceLastEvent}d ago
                    </small>
                  </li>
                ))}
              </ul>
              {ghostTotalPages > 1 && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.6rem' }}>
                  <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem' }}>
                    {ghostPage * GHOST_PAGE_SIZE + 1}–{Math.min((ghostPage + 1) * GHOST_PAGE_SIZE, staleApps.length)} of {staleApps.length}
                  </small>
                  <div style={{ display: 'flex', gap: '0.25rem' }}>
                    <button
                      style={{ padding: '2px 8px', fontSize: '0.75rem', marginBottom: 0 }}
                      disabled={ghostPage === 0}
                      onClick={() => setGhostPage(p => p - 1)}
                    >‹ Prev</button>
                    <button
                      style={{ padding: '2px 8px', fontSize: '0.75rem', marginBottom: 0 }}
                      disabled={ghostPage >= ghostTotalPages - 1}
                      onClick={() => setGhostPage(p => p + 1)}
                    >Next ›</button>
                  </div>
                </div>
              )}
            </article>
          )}
        </div>

      </div>
    </>
  )
}
