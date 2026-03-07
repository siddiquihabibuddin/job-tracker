ALTER TABLE applications_snapshot
    ADD COLUMN IF NOT EXISTS last_event_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS company        TEXT;

UPDATE applications_snapshot SET last_event_at = created_at WHERE last_event_at IS NULL;

CREATE INDEX idx_snapshot_stale ON applications_snapshot (user_id, status, last_event_at)
    WHERE deleted_at IS NULL;

CREATE TABLE stale_flags (
    app_id      UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    company     TEXT,
    role        TEXT,
    status      VARCHAR(20),
    days_stale  INT         NOT NULL,
    flagged_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_stale_user_unresolved ON stale_flags (user_id, resolved_at)
    WHERE resolved_at IS NULL;
