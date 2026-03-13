import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getUnseenCount } from '../../api/alerts'

export function AlertsWidget() {
  const { data: count = 0, isLoading } = useQuery({
    queryKey: ['unseen-count'],
    queryFn: getUnseenCount,
    refetchInterval: 5 * 60 * 1000,
    staleTime: 60_000,
  })

  if (isLoading || count === 0) return null

  return (
    <article style={{ padding: '0.65rem', marginBottom: 0, borderLeft: '3px solid #2563eb' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{
            background: '#2563eb',
            color: '#fff',
            borderRadius: '9999px',
            fontSize: '0.75rem',
            fontWeight: 700,
            padding: '0.15rem 0.55rem',
            lineHeight: 1.4,
          }}>
            {count}
          </span>
          <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>
            New job {count === 1 ? 'match' : 'matches'}
          </span>
        </div>
        <Link
          to="/alerts"
          style={{ fontSize: '0.78rem', fontWeight: 500, whiteSpace: 'nowrap' }}
        >
          View Alerts →
        </Link>
      </div>
    </article>
  )
}
