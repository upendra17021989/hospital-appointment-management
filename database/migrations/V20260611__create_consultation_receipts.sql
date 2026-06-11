-- Consultation receipts for consultation fee receipt printing

CREATE TABLE IF NOT EXISTS consultation_receipts (
    id UUID PRIMARY KEY,

    hospital_id UUID NOT NULL,
    consultation_payment_id UUID NOT NULL,

    receipt_number VARCHAR(60) NOT NULL,
    receipt_date_time TIMESTAMP NOT NULL,

    hospital_name VARCHAR(150) NOT NULL,
    hospital_address TEXT,
    hospital_phone VARCHAR(25),

    patient_name VARCHAR(200) NOT NULL,
    patient_identifier VARCHAR(120) NOT NULL,

    doctor_name VARCHAR(200) NOT NULL,
    department_name VARCHAR(200) NOT NULL,

    consultation_fee NUMERIC(10,2) NOT NULL,

    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(120),

    amount_paid NUMERIC(10,2) NOT NULL,

    received_by_name VARCHAR(150) NOT NULL,

    stamp_placeholder VARCHAR(200),

    created_at TIMESTAMP,

    CONSTRAINT uq_consultation_receipts_hospital_receipt_number
        UNIQUE (hospital_id, receipt_number),

    CONSTRAINT fk_consultation_receipts_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),

    CONSTRAINT fk_consultation_receipts_consultation_payment
        FOREIGN KEY (consultation_payment_id) REFERENCES consultation_payments(id)
);

CREATE INDEX IF NOT EXISTS idx_consultation_receipts_hospital_receipt_number
    ON consultation_receipts (hospital_id, receipt_number);

CREATE INDEX IF NOT EXISTS idx_consultation_receipts_hospital_payment
    ON consultation_receipts (hospital_id, consultation_payment_id, receipt_date_time DESC);

