import { useState, useEffect } from 'react'
import type { CompanyCount } from '../../api/stats'
import { daysAgo } from '../../utils/formatDate'

const PAGE_SIZE = 10

interface TopCompaniesWidgetProps {
  companies: CompanyCount[] | undefined
}

export default function TopCompaniesWidget({ companies }: TopCompaniesWidgetProps) {
  const [page, setPage] = useState(0)

  useEffect(() => {
    setPage(0)
  }, [companies])

  if (!companies || companies.length === 0) return null

  const totalPages = Math.ceil(companies.length / PAGE_SIZE)
  const pageItems = companies.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE)
  const maxCount = companies[0].count

  return (
    <article style={{ padding: '0.65rem', marginBottom: 0 }}>
      <header style={{ marginBottom: '0.75rem' }}>
        <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>Top Companies Applied To</span>
      </header>
      <ol style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {pageItems.map((item, index) => (
          <li
            key={item.company}
            style={{
              padding: '0.28rem 0',
              borderBottom: '1px solid var(--pico-muted-border-color)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.2rem' }}>
              <div>
                <span style={{ fontSize: '0.78rem', fontWeight: 500 }}>
                  <span style={{ color: 'var(--pico-muted-color)', marginRight: '0.4rem', fontSize: '0.72rem' }}>
                    #{page * PAGE_SIZE + index + 1}
                  </span>
                  {item.company}
                </span>
                {item.lastAppliedAt && (
                  <div style={{ fontSize: '0.68rem', color: 'var(--pico-muted-color)', marginTop: '0.1rem', paddingLeft: '1.2rem' }}>
                    Last Applied · {daysAgo(item.lastAppliedAt)} days ago
                  </div>
                )}
              </div>
              <span
                style={{
                  fontSize: '0.72rem',
                  fontWeight: 600,
                  background: 'var(--pico-primary-background)',
                  color: 'var(--pico-primary)',
                  borderRadius: '0.25rem',
                  padding: '0 0.35rem',
                  minWidth: '1.5rem',
                  textAlign: 'center',
                  alignSelf: 'flex-start',
                }}
              >
                {item.count}
              </span>
            </div>
            <div
              style={{
                height: '3px',
                borderRadius: '2px',
                background: 'var(--pico-muted-border-color)',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  height: '100%',
                  width: `${Math.round((item.count / maxCount) * 100)}%`,
                  background: 'var(--pico-primary)',
                  borderRadius: '2px',
                }}
              />
            </div>
          </li>
        ))}
      </ol>
      {totalPages > 1 && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '0.6rem' }}>
          <button
            onClick={() => setPage(p => p - 1)}
            disabled={page === 0}
            style={{
              fontSize: '0.72rem',
              padding: '0.2rem 0.6rem',
              cursor: page === 0 ? 'not-allowed' : 'pointer',
              opacity: page === 0 ? 0.4 : 1,
            }}
          >
            ← Prev
          </button>
          <span style={{ fontSize: '0.72rem', color: 'var(--pico-muted-color)' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={page === totalPages - 1}
            style={{
              fontSize: '0.72rem',
              padding: '0.2rem 0.6rem',
              cursor: page === totalPages - 1 ? 'not-allowed' : 'pointer',
              opacity: page === totalPages - 1 ? 0.4 : 1,
            }}
          >
            Next →
          </button>
        </div>
      )}
    </article>
  )
}
