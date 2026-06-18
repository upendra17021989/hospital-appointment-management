-- Extends consultation receipts for voiding and status management

ALTER TABLE consultation_receipts
    ADD COLUMN IF NOT EXISTS receipt_status VARCHAR(30) DEFAULT 'ACTIVE' NOT NULL,
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS voided_by UUID;

-- Convenience index for reporting/filtering
CREATE INDEX IF NOT EXISTS idx_consultation_receipts_voided_at ON consultation_receipts(voided_at);
CREATE INDEX IF NOT EXISTS idx_consultation_receipts_receipt_status ON consultation_receipts(receipt_status);

-- Optional foreign key if users table exists (in your schema it does via auth_schema.sql)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_consultation_receipts_voided_by'
        ) THEN
            ALTER TABLE consultation_receipts
                ADD CONSTRAINT fk_consultation_receipts_voided_by
                FOREIGN KEY (voided_by) REFERENCES users(id);
        END IF;
    END IF;
END $$;

