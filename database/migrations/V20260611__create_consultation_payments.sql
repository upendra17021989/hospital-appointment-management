-- Consultation payments for consultation fee receipt printing

CREATE TABLE IF NOT EXISTS consultation_payments (
    id UUID PRIMARY KEY,

    hospital_id UUID NOT NULL,
    appointment_id UUID NOT NULL,
    patient_id UUID NOT NULL,

    consultation_fee NUMERIC(10,2) NOT NULL,

    payment_mode VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(120),

    amount_paid NUMERIC(10,2) NOT NULL,
    paid_at TIMESTAMP NOT NULL,

    received_by_user_id UUID,
    received_by_name VARCHAR(150) NOT NULL,

    created_at TIMESTAMP,

    CONSTRAINT fk_consultation_payments_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),

    CONSTRAINT fk_consultation_payments_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments(id),

    CONSTRAINT fk_consultation_payments_patient
        FOREIGN KEY (patient_id) REFERENCES patients(id),

    CONSTRAINT fk_consultation_payments_received_by
        FOREIGN KEY (received_by_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_consultation_payments_hospital_patient_paid_at
    ON consultation_payments (hospital_id, patient_id, paid_at DESC);

