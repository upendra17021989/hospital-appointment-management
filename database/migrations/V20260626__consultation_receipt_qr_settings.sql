ALTER TABLE hospitals
    ADD COLUMN IF NOT EXISTS consultation_receipt_qr_code_url TEXT;
