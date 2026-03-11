import { STATUSES } from './constants'
import { filterInput } from './styles'
import type { AppStatus } from '../../api/applications'

interface Props {
  status: string
  search: string
  month: string
  year: string
  gotCall: string
  sortBy: string
  useMock: boolean
  onStatusChange: (v: string) => void
  onSearchChange: (v: string) => void
  onMonthChange: (v: string) => void
  onYearChange: (v: string) => void
  onGotCallChange: (v: string) => void
  onSortByChange: (v: string) => void
  onClear: () => void
}

export default function ApplicationFilters({
  status,
  search,
  month,
  year,
  gotCall,
  sortBy,
  useMock,
  onStatusChange,
  onSearchChange,
  onMonthChange,
  onYearChange,
  onGotCallChange,
  onSortByChange,
  onClear,
}: Props) {
  return (
    <details open>
      <summary style={{ fontSize: '0.82rem', padding: '4px 0', marginBottom: '0.4rem' }}>Filters</summary>
      <div className="grid" style={{ fontSize: '0.78rem', gap: '0.4rem', alignItems: 'end', marginBottom: '0.5rem' }}>
        <label style={{ marginBottom: 0 }}>
          <small>Status</small>
          <select style={filterInput} value={status} onChange={e => onStatusChange(e.target.value as AppStatus | 'ALL')}>
            {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </label>
        <label style={{ marginBottom: 0 }}>
          <small>Search</small>
          <input style={filterInput} placeholder="Company or role" value={search} onChange={e => onSearchChange(e.target.value)} />
        </label>
        <label style={{ marginBottom: 0 }}>
          <small>Month</small>
          <select style={filterInput} value={month} onChange={e => onMonthChange(e.target.value)}>
            <option value="">All</option>
            <option value="1">Jan</option>
            <option value="2">Feb</option>
            <option value="3">Mar</option>
            <option value="4">Apr</option>
            <option value="5">May</option>
            <option value="6">Jun</option>
            <option value="7">Jul</option>
            <option value="8">Aug</option>
            <option value="9">Sep</option>
            <option value="10">Oct</option>
            <option value="11">Nov</option>
            <option value="12">Dec</option>
          </select>
        </label>
        <label style={{ marginBottom: 0 }}>
          <small>Year</small>
          <select style={filterInput} value={year} onChange={e => onYearChange(e.target.value)}>
            <option value="">All</option>
            <option value="2023">2023</option>
            <option value="2024">2024</option>
            <option value="2025">2025</option>
            <option value="2026">2026</option>
          </select>
        </label>
        <label style={{ marginBottom: 0 }}>
          <small>Got Call</small>
          <select style={filterInput} value={gotCall} onChange={e => onGotCallChange(e.target.value)}>
            <option value="">All</option>
            <option value="true">Yes</option>
            <option value="false">No</option>
          </select>
        </label>
        <label style={{ marginBottom: 0 }}>
          <small>Sort By</small>
          <select style={filterInput} value={sortBy} onChange={e => onSortByChange(e.target.value)}>
            <option value="appliedAt">Apply Date</option>
            <option value="createdAt">Date Added</option>
          </select>
        </label>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <small style={{ visibility: 'hidden' }}>_</small>
          <div style={{ display: 'flex', alignItems: 'center', gap: '.4rem' }}>
            <button type="button" style={{ padding: '3px 9px', fontSize: '0.76rem', marginBottom: 0 }} onClick={onClear}>Clear</button>
            <small style={{ color: 'var(--pico-muted-color)' }}><code>{useMock ? 'mock' : 'API'}</code></small>
          </div>
        </div>
      </div>
    </details>
  )
}
