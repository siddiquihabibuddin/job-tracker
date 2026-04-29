import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getBreakdown, getInsights, getRoleCounts, getStaleApps, getTopCompanies, getTrend, type BreakdownResponse, type CompanyCount, type RoleCountsResponse, type StaleApp, type TrendPoint } from '../api/stats'
import DashboardHeader from '../components/dashboard/DashboardHeader'
import OpenWindowsCards from '../components/dashboard/OpenWindowsCards'
import AppliedVsRejectedChart from '../components/dashboard/AppliedVsRejectedChart'
import WeeklyTrendChart from '../components/dashboard/WeeklyTrendChart'
import RoleBreakdownTable from '../components/dashboard/RoleBreakdownTable'
import RoleKpiCards from '../components/dashboard/RoleKpiCards'
import AiInsightsCard from '../components/dashboard/AiInsightsCard'
import GhostingTracker from '../components/dashboard/GhostingTracker'
import PremiumGate from '../components/PremiumGate'
import { AlertsWidget } from '../components/dashboard/AlertsWidget'
import TopCompaniesWidget from '../components/dashboard/TopCompaniesWidget'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'
const CURRENT_YEAR = new Date().getFullYear()

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

  const { data: topCompanies } = useQuery<CompanyCount[]>({
    queryKey: ['top-companies'],
    queryFn: getTopCompanies,
    staleTime: 60_000,
    enabled: !USE_MOCK,
  })

  const windows = data?.openWindows
  const rows = data?.rows ?? []
  const chartData = rows.map(r => ({ name: r.label, Applied: r.totalApplied, Rejected: r.totalRejected }))

  return (
    <>
      <DashboardHeader
        groupBy={groupBy}
        year={year}
        onGroupByChange={setGroupBy}
        onYearChange={setYear}
      />

      <OpenWindowsCards windows={windows} loading={loading} />

      {error && <article><header>Error</header><p>{error}</p></article>}
      {loading && <article aria-busy="true"><p>Loading…</p></article>}

      {/* 3-column main grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', alignItems: 'start' }}>

        {/* Col 1: Applied vs Rejected chart + Summary table */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {!loading && !error && (
            <AppliedVsRejectedChart
              chartData={chartData}
              rows={rows}
              groupBy={groupBy}
              year={year}
            />
          )}
        </div>

        {/* Col 2: Weekly Trend chart + Role table */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {!USE_MOCK && (
            <WeeklyTrendChart trendData={trendData} trendLoading={trendLoading} />
          )}
          <RoleBreakdownTable
            roleData={roleData}
            groupBy={groupBy}
            loading={loading}
            error={error}
          />
        </div>

        {/* Col 3: Role KPI cards + AI Insights + Ghosting + Alerts */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <RoleKpiCards
            roleData={roleData}
            groupBy={groupBy}
            year={year}
            loading={loading}
            error={error}
          />
          {!USE_MOCK && (
            <AlertsWidget />
          )}
          {!USE_MOCK && (
            <PremiumGate mode="upsell">
              <AiInsightsCard
                insightsData={insightsData}
                insightsLoading={insightsLoading}
                insightsError={insightsError}
                insightsFetching={insightsFetching}
                refetchInsights={refetchInsights}
              />
            </PremiumGate>
          )}
          {!USE_MOCK && (
            <TopCompaniesWidget companies={topCompanies} />
          )}
          {!USE_MOCK && (
            <GhostingTracker staleApps={staleApps} />
          )}
        </div>

      </div>
    </>
  )
}
