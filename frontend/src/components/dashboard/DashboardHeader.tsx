const YEARS = [2023, 2024, 2025, 2026]

interface DashboardHeaderProps {
  groupBy: 'month' | 'year'
  year: number
  onGroupByChange: (v: 'month' | 'year') => void
  onYearChange: (y: number) => void
}

export default function DashboardHeader({ groupBy, year, onGroupByChange, onYearChange }: DashboardHeaderProps) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
      <h2 style={{ margin: 0 }}>Dashboard</h2>
      <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
        <div role="group" style={{ display: 'flex' }}>
          <button
            style={{ padding: '4px 12px', fontSize: '0.8rem', borderRadius: '4px 0 0 4px', marginBottom: 0 }}
            className={groupBy === 'month' ? 'contrast' : 'secondary'}
            onClick={() => onGroupByChange('month')}
          >By Month</button>
          <button
            style={{ padding: '4px 12px', fontSize: '0.8rem', borderRadius: '0 4px 4px 0', marginBottom: 0 }}
            className={groupBy === 'year' ? 'contrast' : 'secondary'}
            onClick={() => onGroupByChange('year')}
          >By Year</button>
        </div>
        {groupBy === 'month' && (
          <select
            value={year}
            onChange={e => onYearChange(Number(e.target.value))}
            style={{ padding: '4px 8px', fontSize: '0.8rem', width: 'auto', marginBottom: 0 }}
          >
            {YEARS.map(y => <option key={y} value={y}>{y}</option>)}
          </select>
        )}
      </div>
    </div>
  )
}
