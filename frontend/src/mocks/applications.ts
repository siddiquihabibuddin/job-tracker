export type AppStatus = 'APPLIED' | 'PHONE' | 'ONSITE' | 'OFFER' | 'REJECTED'

export interface Application {
  id: string
  company: string
  role: string
  status: AppStatus
  source: string
  createdAt: string // ISO
}

export const mockApplications: Application[] = [
  { id: 'a1', company: 'Acme Corp',  role: 'Senior Java Engineer', status: 'APPLIED',  source: 'LinkedIn', createdAt: '2025-10-10T15:12:00Z' },
  { id: 'a2', company: 'Globex',     role: 'Solutions Architect',  status: 'PHONE',    source: 'Referral', createdAt: '2025-10-09T11:30:00Z' },
  { id: 'a3', company: 'Initech',    role: 'Backend Engineer',     status: 'ONSITE',   source: 'Company',  createdAt: '2025-10-07T09:05:00Z' },
  { id: 'a4', company: 'Umbrella',   role: 'Java Lead',            status: 'REJECTED', source: 'Indeed',   createdAt: '2025-10-05T18:42:00Z' },
  { id: 'a5', company: 'Hooli',      role: 'Platform Engineer',    status: 'APPLIED',  source: 'LinkedIn', createdAt: '2025-10-04T13:20:00Z' },
]

export function sleep(ms: number) {
  return new Promise(res => setTimeout(res, ms))
}

export async function fetchMockApplications(
  filters?: { status?: AppStatus | 'ALL'; search?: string }
): Promise<Application[]> {
  await sleep(300) // simulate network
  let rows = [...mockApplications].sort((a,b)=> b.createdAt.localeCompare(a.createdAt))
  if (filters?.status && filters.status !== 'ALL') {
    rows = rows.filter(r => r.status === filters.status)
  }
  if (filters?.search && filters.search.trim()) {
    const q = filters.search.trim().toLowerCase()
    rows = rows.filter(r => r.company.toLowerCase().includes(q) || r.role.toLowerCase().includes(q))
  }
  return rows
}

export async function fetchMockApplicationById(id: string) {
  await sleep(200)
  return mockApplications.find(a => a.id === id) || null
}
export async function createMockApplication(input: {
    company: string
    role: string
    status?: AppStatus
    source?: string
    createdAt?: string
  }) {
    await sleep(250)
    const id = `a${Math.random().toString(36).slice(2, 8)}`
    const now = new Date().toISOString()
    const row: Application = {
      id,
      company: input.company,
      role: input.role,
      status: input.status ?? 'APPLIED',
      source: input.source ?? 'LinkedIn',
      createdAt: input.createdAt ?? now,
    }
    // mutate the in-memory list for demo purposes
    mockApplications.push(row)
    // keep newest first
    mockApplications.sort((a,b)=> b.createdAt.localeCompare(a.createdAt))
    return row
  }

  export async function updateMockApplication(id: string, patch: Partial<Pick<Application, 'status' | 'source'>>) {
    await sleep(200)
    const idx = mockApplications.findIndex(a => a.id === id)
    if (idx === -1) throw new Error('Not found')
    mockApplications[idx] = { ...mockApplications[idx], ...patch }
    return mockApplications[idx]
  }

  export async function deleteMockApplication(id: string) {
    await sleep(150)
    const idx = mockApplications.findIndex(a => a.id === id)
    if (idx === -1) throw new Error('Not found')
    mockApplications.splice(idx, 1)
  }