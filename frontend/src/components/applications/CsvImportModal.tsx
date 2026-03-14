import type React from 'react'
import type { CsvImportResult } from '../../api/applications'

interface Props {
  show: boolean
  importFile: File | null
  importResult: CsvImportResult | null
  importError: string | null
  importing: boolean
  fileInputRef: React.RefObject<HTMLInputElement | null>
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void
  onImport: () => void
  onClose: () => void
}

export default function CsvImportModal({
  show,
  importFile,
  importResult,
  importError,
  importing,
  fileInputRef,
  onFileChange,
  onImport,
  onClose,
}: Props) {
  if (!show) return null

  return (
    <div
      style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <article style={{ minWidth: '22rem', maxWidth: '32rem', width: '100%', margin: '1rem' }}>
        <header><strong>Import from CSV</strong></header>
        <label>
          Select .csv file
          <input ref={fileInputRef} type="file" accept=".csv" onChange={onFileChange} />
        </label>
        {importError && <p><small style={{ color: '#ef4444' }}>{importError}</small></p>}
        {importResult && (
          <div>
            <p>
              <strong style={{ color: '#22c55e' }}>✓ {importResult.imported} imported</strong>
              {importResult.updated > 0 && <span style={{ color: '#f59e0b', marginLeft: '1rem' }}>↻ {importResult.updated} updated</span>}
              {importResult.failed > 0 && <span style={{ color: '#ef4444', marginLeft: '1rem' }}>✗ {importResult.failed} failed</span>}
            </p>
            {importResult.errors.length > 0 && (
              <ul style={{ fontSize: '0.8rem', maxHeight: '8rem', overflowY: 'auto' }}>
                {importResult.errors.map((err, i) => <li key={i}>{err}</li>)}
              </ul>
            )}
          </div>
        )}
        <footer style={{ display: 'flex', justifyContent: 'flex-end', gap: '.5rem' }}>
          <button type="button" className="secondary" onClick={onClose}>Close</button>
          <button type="button" className="contrast" disabled={!importFile || importing} onClick={onImport}>
            {importing ? 'Importing…' : 'Import'}
          </button>
        </footer>
      </article>
    </div>
  )
}
