-- ENUM for application status
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'app_status') THEN
    CREATE TYPE app_status AS ENUM (
      'APPLIED','PHONE','ONSITE','OFFER','REJECTED','ACCEPTED','WITHDRAWN'
    );
  END IF;
END$$;

-- users (minimal for scoping; you can replace later with real auth)
CREATE TABLE IF NOT EXISTS users (
  id            UUID PRIMARY KEY,
  email         TEXT NOT NULL UNIQUE,
  password_hash TEXT,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- applications
CREATE TABLE IF NOT EXISTS applications (
  id               UUID PRIMARY KEY,
  user_id          UUID NOT NULL REFERENCES users(id),
  company          TEXT NOT NULL,
  role             TEXT NOT NULL,
  status           app_status NOT NULL DEFAULT 'APPLIED',
  source           TEXT,
  location         TEXT,
  salary_min       NUMERIC,
  salary_max       NUMERIC,
  currency         CHAR(3) NOT NULL DEFAULT 'USD',
  next_follow_up_on DATE,
  tags_json        JSONB,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at       TIMESTAMPTZ
);

-- helpful indexes
CREATE INDEX IF NOT EXISTS idx_apps_user_created ON applications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apps_user_status_created ON applications (user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apps_user_source_created ON applications (user_id, source, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_apps_tags_gin ON applications USING GIN (tags_json);

-- status history
CREATE TABLE IF NOT EXISTS application_status_history (
  id             BIGSERIAL PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES applications(id),
  from_status    app_status,
  to_status      app_status NOT NULL,
  changed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_history_app_changed ON application_status_history (application_id, changed_at);

-- notes
CREATE TABLE IF NOT EXISTS application_notes (
  id             BIGSERIAL PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES applications(id),
  user_id        UUID NOT NULL REFERENCES users(id),
  body           TEXT NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notes_app_created ON application_notes (application_id, created_at);

-- seed a demo user so the service works before real auth
INSERT INTO users (id, email, password_hash)
VALUES ('00000000-0000-0000-0000-000000000001', 'demo@example.com', null)
ON CONFLICT (id) DO NOTHING;