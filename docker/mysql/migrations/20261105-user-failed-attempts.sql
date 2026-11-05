-- Preserve existing counters and lockout settings; repair legacy NULL values only.
USE db_identity;
UPDATE users SET failed_attempts = 0, updated_at = updated_at WHERE failed_attempts IS NULL;
ALTER TABLE users MODIFY COLUMN failed_attempts INT NOT NULL DEFAULT 0;
