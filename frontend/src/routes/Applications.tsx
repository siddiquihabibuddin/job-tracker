import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  listApplications as apiList,
  deleteApplication as apiDelete,
  importCsv,
  importFolder,
  type Application,
  type AppStatus,
  type CsvImportResult,
  type FolderImportResult,
} from '../api/applications'
import { bulkDeleteApplications } from '../api/billing'
import {
  fetchMockApplications as mockList,
  deleteMockApplication as mockDelete,
} from '../mocks/applications'
import { useDebounce } from '../hooks/useDebounce'
import { PAGE_SIZE } from '../components/applications/constants'
import { isValidStatus } from '../components/applications/utils'
import ApplicationsHeader from '../components/applications/ApplicationsHeader'
import FolderImportModal from '../components/applications/FolderImportModal'
import CsvImportModal from '../components/applications/CsvImportModal'
import ApplicationFilters from '../components/applications/ApplicationFilters'
import ApplicationsTable from '../components/applications/ApplicationsTable'

const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true'

export default function Applications() {
  const [params, setParams] = useSearchParams()

  const statusParam = (params.get('status') || 'ALL').toUpperCase()
  const searchParam = params.get('search') || ''

  const [status, setStatus] = useState<AppStatus | 'ALL'>(isValidStatus(statusParam) ? (statusParam as AppStatus | 'ALL') : 'ALL')
  const [search, setSearch] = useState(searchParam)
  const [month, setMonth] = useState<string>('')
  const [year, setYear] = useState<string>('')
  const [gotCall, setGotCall] = useState<string>('')
  const [sortBy, setSortBy] = useState<string>('appliedAt')
  const [page, setPage] = useState(0)

  // sync filters → URL
  useEffect(() => {
    const next = new URLSearchParams()
    if (status !== 'ALL') next.set('status', status)
    if (search.trim()) next.set('search', search.trim())
    setParams(next, { replace: true })
  }, [status, search, setParams])

  // reset to page 0 when filters change
  useEffect(() => { setPage(0) }, [status, search, month, year, gotCall, sortBy])

  // clear selection when page or filters change
  useEffect(() => { setSelected(new Set()) }, [status, search, month, year, gotCall, sortBy, page])

  const debouncedSearch = useDebounce(search, 300)

  const [rows, setRows] = useState<Application[]>([])
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loadKey, setLoadKey] = useState(0)

  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [bulkDeleting, setBulkDeleting] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  // CSV import state
  const [showImport, setShowImport] = useState(false)
  const [importFile, setImportFile] = useState<File | null>(null)
  const [importing, setImporting] = useState(false)
  const [importResult, setImportResult] = useState<CsvImportResult | null>(null)
  const [importError, setImportError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Folder bulk import state
  const [folderImporting, setFolderImporting] = useState(false)
  const [folderResult, setFolderResult] = useState<FolderImportResult | null>(null)
  const [showFolderResult, setShowFolderResult] = useState(false)

  useEffect(() => {
    let alive = true
    async function load() {
      setLoading(true)
      setError(null)
      try {
        if (USE_MOCK) {
          const data = await mockList({ status: status as 'ALL' | undefined, search: debouncedSearch })
          if (alive) {
            setRows(data as Application[])
            setTotalPages(1)
            setTotalElements((data as Application[]).length)
          }
        } else {
          const res = await apiList({
            status,
            search: debouncedSearch,
            page,
            limit: PAGE_SIZE,
            month: month ? Number(month) : undefined,
            year: year ? Number(year) : undefined,
            gotCall: gotCall !== '' ? gotCall === 'true' : undefined,
            sortBy: sortBy || undefined,
          })
          if (alive) {
            setRows(res.items)
            setTotalPages(res.totalPages)
            setTotalElements(res.totalElements)
          }
        }
      } catch (e: unknown) {
        if (alive) setError(e instanceof Error ? e.message : 'Failed to load')
      } finally {
        if (alive) setLoading(false)
      }
    }
    load()
    return () => { alive = false }
  }, [status, debouncedSearch, page, loadKey, month, year, gotCall, sortBy])

  async function handleDelete(id: string, label: string) {
    setDeletingId(id)
    try {
      if (USE_MOCK) await mockDelete(id)
      else await apiDelete(id)
      setRows(prev => prev.filter(r => r.id !== id))
      setToast(`Deleted: ${label}`)
      setTimeout(() => setToast(null), 2000)
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : 'Failed to delete')
    } finally {
      setDeletingId(null)
    }
  }

  async function handleBulkDelete() {
    const ids = Array.from(selected)
    if (!confirm(`Delete ${ids.length} application${ids.length > 1 ? 's' : ''}?`)) return
    setBulkDeleting(true)
    try {
      if (USE_MOCK) {
        await Promise.all(ids.map(id => mockDelete(id)))
        setRows(prev => prev.filter(r => !selected.has(r.id)))
        setSelected(new Set())
        setToast(`Deleted ${ids.length} application${ids.length > 1 ? 's' : ''}`)
      } else {
        const result = await bulkDeleteApplications(ids)
        // Re-fetch to get authoritative state from the server
        setLoadKey(k => k + 1)
        setSelected(new Set())
        setToast(`Deleted ${result.deleted} application${result.deleted !== 1 ? 's' : ''}`)
      }
      setTimeout(() => setToast(null), 2500)
    } catch {
      alert('Bulk delete failed')
    } finally {
      setBulkDeleting(false)
    }
  }

  function openImportModal() {
    setImportFile(null); setImportResult(null); setImportError(null); setShowImport(true)
  }
  function closeImportModal() {
    setShowImport(false); setImportFile(null); setImportResult(null); setImportError(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  async function handleFolderImport() {
    setFolderImporting(true)
    try {
      const result = await importFolder()
      setFolderResult(result)
      setShowFolderResult(true)
      if (result.totalImported > 0) { setPage(0); setLoadKey(k => k + 1) }
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : 'Folder import failed')
    } finally {
      setFolderImporting(false)
    }
  }

  async function handleImport() {
    if (!importFile) return
    setImporting(true); setImportError(null); setImportResult(null)
    try {
      const result = await importCsv(importFile)
      setImportResult(result)
      if (result.imported > 0) { setPage(0); setLoadKey(k => k + 1) }
    } catch (e: unknown) {
      setImportError(e instanceof Error ? e.message : 'Import failed')
    } finally {
      setImporting(false)
    }
  }

  return (
    <>
      <ApplicationsHeader
        totalElements={totalElements}
        loading={loading}
        selected={selected}
        bulkDeleting={bulkDeleting}
        folderImporting={folderImporting}
        onBulkDelete={handleBulkDelete}
        onOpenImport={openImportModal}
        onFolderImport={handleFolderImport}
      />

      <FolderImportModal
        show={showFolderResult}
        result={folderResult}
        onClose={() => setShowFolderResult(false)}
      />

      <CsvImportModal
        show={showImport}
        importFile={importFile}
        importResult={importResult}
        importError={importError}
        importing={importing}
        fileInputRef={fileInputRef}
        onFileChange={e => { setImportFile(e.target.files?.[0] ?? null); setImportResult(null); setImportError(null) }}
        onImport={handleImport}
        onClose={closeImportModal}
      />

      <ApplicationFilters
        status={status}
        search={search}
        month={month}
        year={year}
        gotCall={gotCall}
        sortBy={sortBy}
        useMock={USE_MOCK}
        onStatusChange={v => setStatus(v as AppStatus | 'ALL')}
        onSearchChange={setSearch}
        onMonthChange={setMonth}
        onYearChange={setYear}
        onGotCallChange={setGotCall}
        onSortByChange={setSortBy}
        onClear={() => { setStatus('ALL'); setSearch(''); setMonth(''); setYear(''); setGotCall(''); setSortBy('appliedAt') }}
      />

      {toast && <article><p>{toast}</p></article>}
      {loading && <article aria-busy="true"><p>Loading…</p></article>}
      {error && <article><header>Error</header><p>{error}</p></article>}

      <ApplicationsTable
        rows={rows}
        loading={loading}
        error={error}
        selected={selected}
        page={page}
        totalPages={totalPages}
        deletingId={deletingId}
        onSelectAll={checked => setSelected(checked ? new Set(rows.map(r => r.id)) : new Set())}
        onToggleSelect={id => setSelected(prev => {
          const next = new Set(prev)
          if (next.has(id)) next.delete(id); else next.add(id)
          return next
        })}
        onDelete={handleDelete}
        onPageChange={setPage}
      />
    </>
  )
}
