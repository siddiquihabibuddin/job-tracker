import { Link } from 'react-router-dom'

/**
 * Shown to FREE-tier users in place of premium features.
 * Styled to match the existing PicoCSS card/article visual language.
 */
export default function UpsellCard() {
  return (
    <article style={{
      padding: '1.1rem 1.25rem',
      marginBottom: 0,
      borderLeft: '3px solid var(--pico-primary)',
      background: 'var(--pico-card-background-color)',
    }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
        {/* Lock icon */}
        <svg
          xmlns="http://www.w3.org/2000/svg"
          width="15"
          height="15"
          viewBox="0 0 24 24"
          fill="none"
          stroke="var(--pico-primary)"
          strokeWidth="2.2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
          style={{ flexShrink: 0 }}
        >
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
        </svg>
        <span style={{ fontSize: '0.82rem', fontWeight: 600 }}>Pro feature</span>
      </header>
      <p style={{ fontSize: '0.78rem', color: 'var(--pico-muted-color)', margin: '0 0 0.75rem' }}>
        Upgrade to unlock this premium feature.
      </p>
      <Link to="/upgrade">
        <button
          type="button"
          className="contrast"
          style={{ padding: '5px 14px', fontSize: '0.78rem', marginBottom: 0, width: 'auto' }}
        >
          Upgrade to Pro
        </button>
      </Link>
    </article>
  )
}
