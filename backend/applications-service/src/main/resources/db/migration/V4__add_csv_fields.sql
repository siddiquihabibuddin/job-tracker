ALTER TABLE applications
  ADD COLUMN IF NOT EXISTS applied_at       DATE,
  ADD COLUMN IF NOT EXISTS job_link         TEXT,
  ADD COLUMN IF NOT EXISTS resume_uploaded  TEXT,
  ADD COLUMN IF NOT EXISTS got_call         BOOLEAN NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS reject_date      DATE,
  ADD COLUMN IF NOT EXISTS login_details    TEXT;
