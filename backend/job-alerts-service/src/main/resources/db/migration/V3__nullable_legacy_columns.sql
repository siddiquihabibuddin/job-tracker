-- company_name is now stored in alert_companies; make the legacy column nullable
ALTER TABLE job_alerts ALTER COLUMN company_name DROP NOT NULL;
