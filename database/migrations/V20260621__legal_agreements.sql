-- Legal documents, hospital acceptance audit, and signed agreement uploads

CREATE TABLE IF NOT EXISTS legal_documents (
    id UUID PRIMARY KEY,
    document_type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    version VARCHAR(40) NOT NULL,
    effective_date DATE NOT NULL,
    content TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uq_legal_documents_type_version UNIQUE (document_type, version)
);

CREATE TABLE IF NOT EXISTS hospital_legal_acceptances (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    legal_document_id UUID NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    document_version VARCHAR(40) NOT NULL,
    accepted_by_user_id UUID,
    accepted_by_name VARCHAR(150),
    accepted_by_email VARCHAR(150),
    acceptance_text TEXT,
    ip_address VARCHAR(80),
    user_agent TEXT,
    subscription_plan VARCHAR(100),
    max_users INTEGER,
    billing_cycle VARCHAR(40),
    accepted_at TIMESTAMP,

    CONSTRAINT fk_hospital_legal_acceptances_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_hospital_legal_acceptances_document
        FOREIGN KEY (legal_document_id) REFERENCES legal_documents(id),
    CONSTRAINT fk_hospital_legal_acceptances_user
        FOREIGN KEY (accepted_by_user_id) REFERENCES users(id),
    CONSTRAINT uq_hospital_legal_acceptance_version
        UNIQUE (hospital_id, document_type, document_version)
);

CREATE TABLE IF NOT EXISTS hospital_signed_agreements (
    id UUID PRIMARY KEY,
    hospital_id UUID NOT NULL,
    agreement_type VARCHAR(60) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT,
    storage_path TEXT NOT NULL,
    review_status VARCHAR(30) DEFAULT 'PENDING' NOT NULL,
    review_notes TEXT,
    reviewed_at TIMESTAMP,
    reviewed_by UUID,
    uploaded_at TIMESTAMP,

    CONSTRAINT fk_hospital_signed_agreements_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_hospital_signed_agreements_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_legal_documents_active
    ON legal_documents (document_type, is_active);

CREATE INDEX IF NOT EXISTS idx_hospital_legal_acceptances_hospital
    ON hospital_legal_acceptances (hospital_id, document_type);

CREATE INDEX IF NOT EXISTS idx_hospital_signed_agreements_hospital
    ON hospital_signed_agreements (hospital_id, agreement_type, uploaded_at DESC);

INSERT INTO legal_documents (id, document_type, title, version, effective_date, content, is_active, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'SERVICE_AGREEMENT', 'Service Agreement', 'v1.0', CURRENT_DATE,
     'Subscription plan, number of users, payment terms, data ownership, support terms, cancellation policy, and service usage terms apply as published by the platform.', TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'PRIVACY_POLICY', 'Privacy Policy', 'v1.0', CURRENT_DATE,
     'Patient data storage, data security measures, backup policies, patient records, prescriptions, medical history, and lab report handling are governed by this privacy policy.', TRUE, NOW(), NOW()),
    (gen_random_uuid(), 'DATA_PROCESSING_AGREEMENT', 'Data Processing Agreement', 'v1.0', CURRENT_DATE,
     'The hospital authorizes the platform to process patient records, prescriptions, medical history, and related health data only for providing the subscribed services.', TRUE, NOW(), NOW())
ON CONFLICT (document_type, version) DO NOTHING;
