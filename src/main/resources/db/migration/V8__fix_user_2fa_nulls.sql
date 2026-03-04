-- Update existing NULL values to false
UPDATE users SET is_two_factor_enabled = false WHERE is_two_factor_enabled IS NULL;

-- Add NOT NULL constraint and DEFAULT value
ALTER TABLE users MODIFY is_two_factor_enabled BOOLEAN NOT NULL DEFAULT false;
