import { STATUS_COLORS } from './constants'

export default function StatusBadge({ status }: { status: string }) {
  const color = STATUS_COLORS[status] ?? '#6b7280'
  return (
    <span style={{
      display: 'inline-block',
      padding: '1px 6px',
      borderRadius: '999px',
      fontSize: '0.7rem',
      fontWeight: 600,
      letterSpacing: '0.02em',
      backgroundColor: color + '22',
      color,
      border: `1px solid ${color}55`,
      whiteSpace: 'nowrap',
    }}>
      {status}
    </span>
  )
}
