CREATE TABLE job_alerts (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL,
    company_name    TEXT        NOT NULL,
    board_token     TEXT,
    role_keywords   TEXT        NOT NULL,
    platforms       TEXT        NOT NULL,
    workday_tenant  TEXT,
    workday_site    TEXT,
    workday_wd_num  SMALLINT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_job_alerts_user_active
    ON job_alerts (user_id, is_active) WHERE deleted_at IS NULL;

CREATE TABLE job_alert_matches (
    id              UUID        PRIMARY KEY,
    alert_id        UUID        NOT NULL REFERENCES job_alerts(id),
    user_id         UUID        NOT NULL,
    platform        TEXT        NOT NULL,
    external_id     TEXT        NOT NULL,
    title           TEXT        NOT NULL,
    job_url         TEXT,
    company_name    TEXT        NOT NULL,
    location        TEXT,
    posted_at       TIMESTAMPTZ,
    seen_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uidx_alert_match_platform_external
    ON job_alert_matches (alert_id, platform, external_id);
CREATE INDEX idx_alert_matches_user_unseen
    ON job_alert_matches (user_id, seen_at) WHERE seen_at IS NULL;
