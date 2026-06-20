ALTER TABLE hospitals
    ADD COLUMN IF NOT EXISTS consultation_receipt_header_enabled BOOLEAN DEFAULT TRUE NOT NULL;
