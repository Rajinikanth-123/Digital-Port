-- Migration Script for Step 13: Invoicing, Payments, and Financial Management
-- Safe schema update: Adds currency, discount, and notes columns to invoices table without dropping data.

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'USD' AFTER due_date,
    ADD COLUMN IF NOT EXISTS discount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER taxes,
    ADD COLUMN IF NOT EXISTS notes TEXT NULL AFTER payment_status;

-- Add helpful financial indices if they don't already exist
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON invoices(due_date);
CREATE INDEX IF NOT EXISTS idx_payments_paid_at ON payments(paid_at);
