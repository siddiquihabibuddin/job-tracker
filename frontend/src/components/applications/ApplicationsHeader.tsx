import { Link } from 'react-router-dom'
import UpsellCard from '../UpsellCard'
import { usePremium } from '../../auth/usePremium'

interface Props {
  totalElements: number
  loading: boolean
  selected: Set<string>
  bulkDeleting: boolean
  folderImporting: boolean
  onBulkDelete: () => void
  onOpenImport: () => void
  onFolderImport: () => void
}

export default function ApplicationsHeader({
  totalElements,
  loading,
  selected,
  bulkDeleting,
  folderImporting,
  onBulkDelete,
  onOpenImport,
  onFolderImport,
}: Props) {
  const isPremium = usePremium()

  return (
    <>
      <header className="grid">
        <h2 style={{ margin: 0 }}>Applications
          {!loading && <small style={{ fontWeight: 400, fontSize: '0.85rem', marginLeft: '0.5rem', color: 'var(--pico-muted-color)' }}>
            {totalElements} total
          </small>}
        </h2>
        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: '.4rem' }}>
          {/* Bulk delete — premium only, hidden for free users */}
          {selected.size > 0 && isPremium && (
            <button
              type="button"
              style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0, background: '#ef4444', border: 'none', color: '#fff' }}
              disabled={bulkDeleting}
              onClick={onBulkDelete}
            >
              {bulkDeleting ? 'Deleting…' : `Delete ${selected.size} selected`}
            </button>
          )}
          {/* Import buttons — premium only, hidden for free users */}
          {isPremium && (
            <>
              <button
                type="button"
                style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0 }}
                disabled={folderImporting}
                aria-busy={folderImporting}
                onClick={onFolderImport}
              >
                {folderImporting ? 'Importing…' : 'Bulk Import'}
              </button>
              <div style={{ position: 'relative', display: 'inline-flex' }} className="csv-tooltip-wrap">
                <button type="button" style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0 }} onClick={onOpenImport}>Import CSV</button>
                <div className="csv-tooltip">
                  Expected header (first row):<br />
                  <code>Company, Role, Location, Salary Range, Apply Date, Final Status, Job Link, Resume Uploaded, Call, Reject Date, Login Details, Days pending</code>
                </div>
              </div>
            </>
          )}
          <Link to="/applications/new" style={{ display: 'inline-flex' }}>
            <button className="contrast" style={{ padding: '4px 10px', fontSize: '0.8rem', marginBottom: 0 }}>+ New</button>
          </Link>
        </div>
        <style>{`
          .csv-tooltip-wrap .csv-tooltip {
            display: none;
            position: absolute;
            top: calc(100% + 6px);
            right: 0;
            background: var(--pico-card-background-color, #1e2535);
            border: 1px solid var(--pico-muted-border-color, #374151);
            border-radius: 6px;
            padding: 8px 10px;
            font-size: 0.72rem;
            line-height: 1.5;
            white-space: normal;
            width: 480px;
            max-width: 92vw;
            z-index: 999;
            color: var(--pico-color);
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          }
          .csv-tooltip-wrap .csv-tooltip code {
            display: block;
            margin-top: 4px;
            font-size: 0.69rem;
            color: var(--pico-primary);
            word-break: break-word;
            white-space: normal;
          }
          .csv-tooltip-wrap:hover .csv-tooltip {
            display: block;
          }
        `}</style>
      </header>

      {/* For free users: show a single upsell card below the header for import features */}
      {!isPremium && (
        <div style={{ marginBottom: '0.75rem' }}>
          <UpsellCard />
        </div>
      )}
    </>
  )
}
