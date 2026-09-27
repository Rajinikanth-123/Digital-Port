-- Digital Port & Shipping Management System - Step 12 Migration
-- Customs Clearance Workflows and Secure Document Management

-- Add officer assignment, remarks, and update timestamp columns to customs_clearances
ALTER TABLE customs_clearances
  ADD COLUMN IF NOT EXISTS assigned_officer_id BIGINT UNSIGNED NULL AFTER document_details,
  ADD COLUMN IF NOT EXISTS remarks TEXT NULL AFTER assigned_officer_id,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at;

-- Add foreign key constraint for assigned officer
ALTER TABLE customs_clearances
  ADD CONSTRAINT fk_customs_officer FOREIGN KEY (assigned_officer_id) REFERENCES users(id);

-- Add optional customs_clearance_id reference and checksum column to documents table
ALTER TABLE documents
  ADD COLUMN IF NOT EXISTS customs_clearance_id BIGINT UNSIGNED NULL AFTER shipment_id,
  ADD COLUMN IF NOT EXISTS checksum_sha256 VARCHAR(64) NULL AFTER file_size_bytes;

-- Add foreign key constraint for customs clearance link
ALTER TABLE documents
  ADD CONSTRAINT fk_documents_customs FOREIGN KEY (customs_clearance_id) REFERENCES customs_clearances(id);
