ALTER TABLE applications_snapshot ADD COLUMN IF NOT EXISTS applied_at DATE;

CREATE INDEX idx_snap_user_applied ON applications_snapshot(user_id, applied_at DESC);
