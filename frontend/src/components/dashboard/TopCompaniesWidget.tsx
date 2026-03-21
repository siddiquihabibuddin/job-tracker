import type { CompanyCount } from '../../api/stats'

interface TopCompaniesWidgetProps {
  companies: CompanyCount[] | undefined
}

export default function TopCompaniesWidget({ companies }: TopCompaniesWidgetProps) {
  if (!companies || companies.length === 0) return null

  const maxCount = companies[0].count

  return (
    <article style={{ padding: '0.65rem', marginBottom: 0 }}>
      <header style={{ marginBottom: '0.75rem' }}>
        <span style={{ fontSize: '0.78rem', fontWeight: 600 }}>Top Companies Applied To</span>
      </header>
      <ol style={{ listStyle: 'none', padding: 0, margin: 0 }}>
        {companies.map((item, index) => (
          <li
            key={item.company}
            style={{
              padding: '0.28rem 0',
              borderBottom: '1px solid var(--pico-muted-border-color)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.2rem' }}>
              <span style={{ fontSize: '0.78rem', fontWeight: 500 }}>
                <span style={{ color: 'var(--pico-muted-color)', marginRight: '0.4rem', fontSize: '0.72rem' }}>
                  #{index + 1}
                </span>
                {item.company}
              </span>
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
    </article>
  )
}
