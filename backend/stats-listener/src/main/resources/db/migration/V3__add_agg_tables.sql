CREATE TABLE agg_monthly (
    user_id  UUID        NOT NULL,
    year     SMALLINT    NOT NULL,
    month    SMALLINT    NOT NULL,
    status   VARCHAR(20) NOT NULL,
    cnt      INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, year, month, status)
);
CREATE INDEX idx_agg_monthly_user_year ON agg_monthly (user_id, year);

CREATE TABLE agg_weekly (
    user_id    UUID NOT NULL,
    week_start DATE NOT NULL,
    cnt        INT  NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, week_start)
);
CREATE INDEX idx_agg_weekly_user_week ON agg_weekly (user_id, week_start);

-- Backfill from existing snapshot at migration time
INSERT INTO agg_monthly (user_id, year, month, status, cnt)
SELECT user_id,
       EXTRACT(year  FROM COALESCE(applied_at, created_at::date))::smallint,
       EXTRACT(month FROM COALESCE(applied_at, created_at::date))::smallint,
       status, COUNT(*)
FROM applications_snapshot WHERE deleted_at IS NULL
GROUP BY 1,2,3,4
ON CONFLICT (user_id, year, month, status) DO UPDATE SET cnt = EXCLUDED.cnt;

INSERT INTO agg_weekly (user_id, week_start, cnt)
SELECT user_id,
       date_trunc('week', COALESCE(applied_at, created_at::date))::date,
       COUNT(*)
FROM applications_snapshot WHERE deleted_at IS NULL
GROUP BY 1,2
ON CONFLICT (user_id, week_start) DO UPDATE SET cnt = EXCLUDED.cnt;
