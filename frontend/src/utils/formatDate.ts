/** Formats an ISO date string (YYYY-MM-DD) as Mon/DD/YYYY, e.g. Mar/18/2026 */
export function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-').map(Number)
  const monthName = new Date(year, month - 1).toLocaleString('en-US', { month: 'short' })
  return `${monthName}/${day}/${year}`
}

/** Returns the number of whole days between today and a YYYY-MM-DD date string. */
export function daysAgo(dateStr: string): number {
  const [year, month, day] = dateStr.split('-').map(Number)
  const then = new Date(year, month - 1, day)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.floor((today.getTime() - then.getTime()) / (1000 * 60 * 60 * 24))
}
