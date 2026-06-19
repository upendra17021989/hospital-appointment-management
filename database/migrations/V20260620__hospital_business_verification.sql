-- Hospital business verification / KYC fields and uploaded document metadata

ALTER TABLE hospitals
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS gst_number VARCHAR(30),
    ADD COLUMN IF NOT EXISTS pan_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS owner_director_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) DEFAULT 'PENDING' NOT NULL,
    ADD COLUMN IF NOT EXISTS verification_notes TEXT;

CREATE TABLE IF NOT EXISTS hospital_documents (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT,
    storage_path TEXT NOT NULL,
    uploaded_at TIMESTAMP,

    CONSTRAINT fk_hospital_documents_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE INDEX IF NOT EXISTS idx_hospital_documents_hospital_type
    ON hospital_documents (hospital_id, document_type);
