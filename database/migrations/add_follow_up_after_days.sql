-- Add follow_up_after_days to prescriptions (0 = no follow-up required)
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS follow_up_after_days INTEGER;
