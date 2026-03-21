/** Formats an ISO date string (YYYY-MM-DD) as Mon/DD/YYYY, e.g. Mar/18/2026 */
export function formatDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-').map(Number)
  const monthName = new Date(year, month - 1).toLocaleString('en-US', { month: 'short' })
  return `${monthName}/${day}/${year}`
}
