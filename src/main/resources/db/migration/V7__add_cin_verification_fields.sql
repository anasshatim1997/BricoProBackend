ALTER TABLE worker_profiles
    ADD COLUMN verification_status VARCHAR(20) NOT NULL DEFAULT 'UNSUBMITTED',
    ADD COLUMN cin_number VARCHAR(12) NULL,
    ADD COLUMN cin_rejection_reason TEXT NULL,
    ADD COLUMN cin_submitted_at DATETIME NULL;

ALTER TABLE worker_profiles
    ADD CONSTRAINT uq_worker_profiles_cin_number UNIQUE (cin_number);

UPDATE worker_profiles SET verification_status = 'VERIFIED' WHERE cin_verified = true;
UPDATE worker_profiles SET verification_status = 'PENDING' WHERE cin_verified = false AND cin_document_url IS NOT NULL;