import type { FolderImportResult } from '../../api/applications'

interface Props {
  show: boolean
  result: FolderImportResult | null
  onClose: () => void
}

export default function FolderImportModal({ show, result, onClose }: Props) {
  if (!show || !result) return null

  return (
    <div
      style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}
      onClick={e => { if (e.target === e.currentTarget) onClose() }}
    >
      <article style={{ minWidth: '24rem', maxWidth: '38rem', width: '100%', margin: '1rem' }}>
        <header>
          <strong>Bulk Import — {result.totalFiles} file{result.totalFiles !== 1 ? 's' : ''}</strong>
          <span style={{ marginLeft: '1rem', color: '#22c55e' }}>✓ {result.totalImported} imported</span>
          {result.totalFailed > 0 && (
            <span style={{ marginLeft: '0.75rem', color: '#ef4444' }}>✗ {result.totalFailed} failed</span>
          )}
        </header>
        {result.totalFiles === 0 ? (
          <p style={{ color: 'var(--pico-muted-color)', fontSize: '0.85rem' }}>No CSV files found in the import folder.</p>
        ) : (
          <div style={{ maxHeight: '20rem', overflowY: 'auto' }}>
            {result.files.map((f, i) => (
              <div key={i} style={{ borderBottom: '1px solid var(--pico-muted-border-color)', padding: '0.5rem 0' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem' }}>
                  <span style={{ fontWeight: 500 }}>{f.fileName}</span>
                  <span>
                    <span style={{ color: '#22c55e' }}>✓ {f.imported}</span>
                    {f.failed > 0 && <span style={{ color: '#ef4444', marginLeft: '0.5rem' }}>✗ {f.failed}</span>}
                  </span>
                </div>
                {f.errors.length > 0 && (
                  <ul style={{ fontSize: '0.75rem', color: '#ef4444', margin: '0.25rem 0 0', paddingLeft: '1.2rem', maxHeight: '5rem', overflowY: 'auto' }}>
                    {f.errors.map((err, j) => <li key={j}>{err}</li>)}
                  </ul>
                )}
              </div>
            ))}
          </div>
        )}
        <footer style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button type="button" className="secondary" onClick={onClose}>Close</button>
        </footer>
      </article>
    </div>
  )
}
