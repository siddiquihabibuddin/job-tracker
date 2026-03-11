import { STATUSES } from './constants'
import type { AppStatus } from '../../api/applications'

export function isValidStatus(s: string): boolean {
  return STATUSES.includes(s as AppStatus | 'ALL')
}

export function fmtDate(iso: string): string {
  if (!iso) return '—'
  // YYYY-MM-DD strings are UTC midnight in JS — parse as local to avoid off-by-one day
  const [y, m, d] = iso.split('T')[0].split('-').map(Number)
  const date = new Date(y, m - 1, d)
  return isNaN(date.getTime()) ? iso : date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: '2-digit' })
}

export function fmtSalary(min?: number, max?: number, currency?: string): string {
  if (!min && !max) return '—'
  const fmt = (n: number) => n >= 1000 ? `${Math.round(n / 1000)}k` : String(n)
  const cur = currency && currency !== 'USD' ? currency + ' ' : ''
  if (min && max) return `${cur}${fmt(min)}–${fmt(max)}`
  if (min) return `${cur}${fmt(min)}+`
  return `${cur}up to ${fmt(max!)}`
}

/** Produce page numbers + ellipsis for the pagination bar */
export function pageRange(current: number, total: number): (number | '…')[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages: (number | '…')[] = []
  const add = (n: number) => { if (!pages.includes(n)) pages.push(n) }
  add(0)
  if (current > 2) pages.push('…')
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) add(i)
  if (current < total - 3) pages.push('…')
  add(total - 1)
  return pages
}
