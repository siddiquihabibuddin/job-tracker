CREATE TABLE IF NOT EXISTS _bootstrap_check (
  id SERIAL PRIMARY KEY,
  note TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO _bootstrap_check (note) VALUES ('applications-service up');