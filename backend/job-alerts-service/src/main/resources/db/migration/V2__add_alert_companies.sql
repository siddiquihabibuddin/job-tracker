CREATE TABLE alert_companies (
    id             UUID     PRIMARY KEY,
    alert_id       UUID     NOT NULL REFERENCES job_alerts(id),
    company_name   TEXT     NOT NULL,
    board_token    TEXT,
    workday_tenant TEXT,
    workday_site   TEXT,
    workday_wd_num SMALLINT,
    display_order  INTEGER  NOT NULL DEFAULT 0
);
CREATE INDEX idx_alert_companies_alert ON alert_companies(alert_id);

-- Migrate existing single-company alerts into the new table
INSERT INTO alert_companies(id, alert_id, company_name, board_token, workday_tenant, workday_site, workday_wd_num, display_order)
SELECT gen_random_uuid(), id, company_name, board_token, workday_tenant, workday_site, workday_wd_num, 0
FROM job_alerts
WHERE deleted_at IS NULL;
