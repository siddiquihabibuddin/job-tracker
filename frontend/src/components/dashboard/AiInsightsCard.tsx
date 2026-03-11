import type { InsightsResponse } from '../../api/stats'

interface AiInsightsCardProps {
  insightsData: InsightsResponse | undefined
  insightsLoading: boolean
  insightsError: boolean
  insightsFetching: boolean
  refetchInsights: () => void
}

export default function AiInsightsCard({ insightsData, insightsLoading, insightsError, insightsFetching, refetchInsights }: AiInsightsCardProps) {
  return (
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
          Could not load insights.{' '}
          <button
            style={{ padding: 0, background: 'none', border: 'none', color: 'var(--pico-primary)', cursor: 'pointer', fontSize: '0.78rem' }}
            onClick={() => refetchInsights()}
          >
            Retry
          </button>
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
  )
}
