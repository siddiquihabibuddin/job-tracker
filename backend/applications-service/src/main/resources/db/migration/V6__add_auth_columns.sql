ALTER TABLE users ADD COLUMN IF NOT EXISTS display_name TEXT;

UPDATE users
SET password_hash = '$2a$10$loBSOh2XwC.5ErradBuym.srqgN4AO3dTSnpfjS0N/we/pMMESaGS'
WHERE id = '00000000-0000-0000-0000-000000000001'
  AND password_hash IS NULL;
