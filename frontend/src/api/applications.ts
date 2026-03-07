import { httpApps } from './client'

export type AppStatus = 'APPLIED' | 'PHONE' | 'ONSITE' | 'OFFER' | 'REJECTED' | 'ACCEPTED' | 'WITHDRAWN'

export interface Application {
  id: string
  company: string
  role: string
  status: AppStatus
  source?: string
  location?: string
  salaryMin?: number
  salaryMax?: number
  currency?: string
  nextFollowUpOn?: string
  appliedAt?: string
  jobLink?: string
  resumeUploaded?: string
  gotCall?: boolean
  rejectDate?: string
  loginDetails?: string
  createdAt: string
  updatedAt?: string
}

export interface ListResponse {
  items: Application[]
  limit: number
  totalElements: number
  totalPages: number
  page: number
}

export interface CsvImportResult {
  imported: number
  failed: number
  errors: string[]
}

export interface FileImportSummary {
  fileName: string
  imported: number
  failed: number
  errors: string[]
}

export interface FolderImportResult {
  totalFiles: number
  totalImported: number
  totalFailed: number
  files: FileImportSummary[]
}

export async function listApplications(params: {
  status?: AppStatus | 'ALL'
  search?: string
  page?: number
  limit?: number
  month?: number
  year?: number
  gotCall?: boolean
  sortBy?: string
}): Promise<ListResponse> {
  const { status, ...rest } = params
  const q: Record<string, string> = {}
  if (status && status !== 'ALL') q.status = status
  Object.entries(rest).forEach(([k, v]) => {
    if (v !== undefined && v !== '') q[k] = String(v)
  })
  const res = await httpApps.get<ListResponse>('/applications', { params: q })
  return res.data
}

// tiny UUID v4 helper (good enough for idempotency keys in dev)
export function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export async function createApplication(input: {
  company: string
  role: string
  status?: AppStatus
  source?: string
  tags?: string[]
  location?: string
  salary?: { min?: number; max?: number; currency?: string }
  nextFollowUpOn?: string
  notes?: string
  appliedAt?: string
  jobLink?: string
  resumeUploaded?: string
  gotCall?: boolean
  rejectDate?: string
  loginDetails?: string
}): Promise<Application> {
  const res = await httpApps.post<Application>(
    '/applications',
    input,
    { headers: { 'Idempotency-Key': uuidv4() } }
  )
  return res.data
}

export async function getApplicationById(id: string): Promise<Application> {
  const res = await httpApps.get<Application>(`/applications/${id}`)
  return res.data
}

export async function patchApplication(id: string, body: Partial<Pick<Application, 'status' | 'source' | 'rejectDate' | 'gotCall'>>): Promise<Application> {
  const res = await httpApps.patch<Application>(
    `/applications/${id}`,
    body,
    { headers: { 'Idempotency-Key': uuidv4() } }
  )
  return res.data
}

export async function deleteApplication(id: string): Promise<void> {
  await httpApps.delete(`/applications/${id}`, {
    headers: { 'Idempotency-Key': uuidv4() },
  })
}

export async function resyncStats(): Promise<void> {
  await httpApps.post('/applications/resync-stats')
}

export async function importCsv(file: File): Promise<CsvImportResult> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await httpApps.post<CsvImportResult>('/applications/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

export async function importFolder(): Promise<FolderImportResult> {
  const res = await httpApps.post<FolderImportResult>('/applications/import-folder')
  return res.data
}
