import type { AppStatus } from '../../api/applications'

export const STATUSES: (AppStatus | 'ALL')[] = ['ALL', 'APPLIED', 'PHONE', 'ONSITE', 'OFFER', 'REJECTED', 'ACCEPTED', 'WITHDRAWN']

export const STATUS_COLORS: Record<string, string> = {
  APPLIED:   '#3b82f6',
  PHONE:     '#f59e0b',
  ONSITE:    '#8b5cf6',
  OFFER:     '#10b981',
  REJECTED:  '#ef4444',
  ACCEPTED:  '#22c55e',
  WITHDRAWN: '#6b7280',
}

export const PAGE_SIZE = 20
