ALTER TABLE applications_snapshot ADD COLUMN IF NOT EXISTS role VARCHAR(200);

CREATE TABLE agg_role (
    user_id  UUID        NOT NULL,
    year     SMALLINT    NOT NULL,
    month    SMALLINT    NOT NULL,
    category VARCHAR(20) NOT NULL,
    cnt      INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, year, month, category)
);
CREATE INDEX idx_agg_role_user_year ON agg_role (user_id, year);

-- Backfill from existing snapshot data (role is NULL for old rows → goes to OTHER)
INSERT INTO agg_role (user_id, year, month, category, cnt)
SELECT user_id,
       EXTRACT(year  FROM COALESCE(applied_at, created_at::date))::smallint,
       EXTRACT(month FROM COALESCE(applied_at, created_at::date))::smallint,
       CASE
           WHEN LOWER(COALESCE(role, '')) SIMILAR TO '%(engineer|developer|dev)%' THEN 'ENGINEER_DEV'
           WHEN LOWER(COALESCE(role, '')) SIMILAR TO '%(manager|mgr)%'             THEN 'MANAGER'
           WHEN LOWER(COALESCE(role, '')) LIKE '%architect%'                        THEN 'ARCHITECT'
           ELSE 'OTHER'
       END,
       COUNT(*)
FROM applications_snapshot
WHERE deleted_at IS NULL
GROUP BY 1, 2, 3, 4
ON CONFLICT (user_id, year, month, category) DO UPDATE SET cnt = EXCLUDED.cnt;
