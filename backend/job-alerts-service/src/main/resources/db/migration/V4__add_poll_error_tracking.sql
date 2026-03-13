ALTER TABLE alert_companies
  ADD COLUMN last_error_message TEXT,
  ADD COLUMN last_error_at      TIMESTAMPTZ,
  ADD COLUMN last_success_at    TIMESTAMPTZ;
