-- Digital Port and Shipping Management System
-- MySQL 8.0+, UTC timestamps, InnoDB, utf8mb4.
CREATE DATABASE IF NOT EXISTS digital_port_shipping
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE digital_port_shipping;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(40) NOT NULL,
  description VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  full_name VARCHAR(120) NULL,
  email VARCHAR(160) NOT NULL,
  password VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NULL,
  role VARCHAR(40) NOT NULL DEFAULT 'VIEWER',
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  phone_number VARCHAR(40) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_users_email (email), KEY idx_users_role_name (role)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_preferences (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  theme VARCHAR(20) NOT NULL DEFAULT 'light',
  language VARCHAR(10) NOT NULL DEFAULT 'en',
  timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
  notify_shipments BOOLEAN NOT NULL DEFAULT TRUE,
  notify_vessels BOOLEAN NOT NULL DEFAULT TRUE,
  notify_customs BOOLEAN NOT NULL DEFAULT TRUE,
  notify_invoices BOOLEAN NOT NULL DEFAULT TRUE,
  notify_system BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_user_preferences_user_id (user_id),
  CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ports (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  code VARCHAR(10) NOT NULL,
  country VARCHAR(80) NOT NULL,
  timezone VARCHAR(60) NOT NULL DEFAULT 'UTC',
  latitude FLOAT NULL, longitude FLOAT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_ports_code (code), KEY idx_ports_country (country)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS terminals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  port_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(120) NOT NULL, code VARCHAR(20) NOT NULL,
  terminal_type VARCHAR(50) NOT NULL DEFAULT 'CONTAINER',
  capacity INT UNSIGNED NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_terminals_code (code), KEY idx_terminals_port (port_id),
  CONSTRAINT fk_terminals_port FOREIGN KEY (port_id) REFERENCES ports(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vessels (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(160) NOT NULL, imo_number VARCHAR(20) NOT NULL,
  vessel_type VARCHAR(60) NOT NULL, flag VARCHAR(80) NOT NULL,
  capacity INT UNSIGNED NOT NULL DEFAULT 0, operator_name VARCHAR(160) NOT NULL,
  length_m FLOAT NULL, draft_m FLOAT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_vessels_imo (imo_number),
  KEY idx_vessels_status (status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS berths (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  terminal_id BIGINT UNSIGNED NOT NULL, name VARCHAR(80) NOT NULL,
  max_length_m FLOAT NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_berths_terminal_name (terminal_id, name),
  KEY idx_berths_status (status),
  CONSTRAINT fk_berths_terminal FOREIGN KEY (terminal_id) REFERENCES terminals(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS shipments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  tracking_number VARCHAR(40) NOT NULL, reference_number VARCHAR(40) NULL,
  origin VARCHAR(120) NOT NULL, destination VARCHAR(120) NOT NULL,
  cargo_details TEXT NULL, shipper VARCHAR(160) NULL, consignee VARCHAR(160) NULL,
  estimated_arrival DATE NULL, status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
  container_count INT UNSIGNED NOT NULL DEFAULT 0, vessel_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_shipments_tracking (tracking_number),
  UNIQUE KEY uq_shipments_reference (reference_number), KEY idx_shipments_status (status),
  KEY idx_shipments_vessel (vessel_id),
  CONSTRAINT fk_shipments_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cargo (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shipment_id BIGINT UNSIGNED NOT NULL, description VARCHAR(255) NOT NULL,
  cargo_type VARCHAR(80) NULL, weight_kg DECIMAL(12,3) NOT NULL,
  quantity INT UNSIGNED NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_cargo_shipment (shipment_id),
  CONSTRAINT fk_cargo_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS containers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  container_number VARCHAR(20) NOT NULL, size VARCHAR(10) NOT NULL,
  type VARCHAR(30) NOT NULL, size_code VARCHAR(10) NULL, type_code VARCHAR(30) NULL,
  weight_kg DECIMAL(12,3) NOT NULL DEFAULT 0, location VARCHAR(120) NULL,
  shipment_id BIGINT UNSIGNED NULL, status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_containers_number (container_number),
  KEY idx_containers_status (status), KEY idx_containers_shipment (shipment_id),
  CONSTRAINT fk_containers_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB;

-- Compatibility table for the existing Container entity's @Table(name = "port_containers").
CREATE TABLE IF NOT EXISTS port_containers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, container_number VARCHAR(20) NOT NULL,
  size VARCHAR(10) NULL, type VARCHAR(30) NULL, status VARCHAR(30) NULL,
  shipment_id BIGINT UNSIGNED NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_port_containers_number (container_number),
  KEY idx_port_containers_shipment (shipment_id),
  CONSTRAINT fk_port_containers_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS berth_allocations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  berth_id BIGINT UNSIGNED NULL, berth VARCHAR(80) NULL, vessel_id BIGINT UNSIGNED NOT NULL,
  start_time DATETIME(6) NOT NULL, end_time DATETIME(6) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_allocations_window (berth_id, start_time, end_time),
  KEY idx_allocations_vessel (vessel_id),
  CONSTRAINT chk_allocation_time CHECK (end_time > start_time),
  CONSTRAINT fk_allocations_berth FOREIGN KEY (berth_id) REFERENCES berths(id),
  CONSTRAINT fk_allocations_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS shipping_schedules (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  vessel_id BIGINT UNSIGNED NOT NULL, berth_id BIGINT UNSIGNED NULL,
  arrival_estimated DATETIME(6) NOT NULL, departure_estimated DATETIME(6) NULL,
  arrival_actual DATETIME(6) NULL, departure_actual DATETIME(6) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_schedules_arrival (arrival_estimated),
  KEY idx_schedules_vessel (vessel_id),
  CONSTRAINT fk_schedules_vessel FOREIGN KEY (vessel_id) REFERENCES vessels(id),
  CONSTRAINT fk_schedules_berth FOREIGN KEY (berth_id) REFERENCES berths(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customs_clearances (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, shipment_id BIGINT UNSIGNED NOT NULL,
  declaration_number VARCHAR(60) NOT NULL, status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  document_details TEXT NULL, assigned_officer_id BIGINT UNSIGNED NULL, remarks TEXT NULL,
  submitted_at DATETIME(6) NULL, cleared_at DATETIME(6) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_customs_declaration (declaration_number),
  KEY idx_customs_status (status),
  CONSTRAINT fk_customs_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  CONSTRAINT fk_customs_officer FOREIGN KEY (assigned_officer_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS documents (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, shipment_id BIGINT UNSIGNED NULL,
  customs_clearance_id BIGINT UNSIGNED NULL,
  document_type VARCHAR(60) NOT NULL, file_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL, mime_type VARCHAR(120) NOT NULL,
  file_size_bytes BIGINT UNSIGNED NOT NULL, checksum_sha256 VARCHAR(64) NULL,
  uploaded_by BIGINT UNSIGNED NULL, uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_documents_shipment (shipment_id), KEY idx_documents_customs (customs_clearance_id),
  CONSTRAINT fk_documents_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  CONSTRAINT fk_documents_customs FOREIGN KEY (customs_clearance_id) REFERENCES customs_clearances(id),
  CONSTRAINT fk_documents_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, invoice_number VARCHAR(50) NOT NULL,
  customer_name VARCHAR(160) NOT NULL, shipment_id BIGINT UNSIGNED NULL,
  charges DECIMAL(14,2) NOT NULL DEFAULT 0, taxes DECIMAL(14,2) NOT NULL DEFAULT 0,
  discount DECIMAL(14,2) NOT NULL DEFAULT 0,
  total DECIMAL(14,2) AS (charges + taxes - discount) STORED, due_date DATE NOT NULL,
  currency VARCHAR(10) NOT NULL DEFAULT 'USD',
  payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  notes TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id), UNIQUE KEY uq_invoices_number (invoice_number),
  KEY idx_invoices_status (payment_status),
  KEY idx_invoices_due_date (due_date),
  CONSTRAINT fk_invoices_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS invoice_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, invoice_id BIGINT UNSIGNED NOT NULL,
  description VARCHAR(255) NOT NULL, quantity DECIMAL(12,3) NOT NULL DEFAULT 1,
  unit_price DECIMAL(14,2) NOT NULL, line_total DECIMAL(14,2) AS (quantity * unit_price) STORED,
  PRIMARY KEY (id), KEY idx_invoice_items_invoice (invoice_id),
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, invoice_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(14,2) NOT NULL, paid_at DATETIME(6) NOT NULL,
  payment_method VARCHAR(40) NOT NULL, reference VARCHAR(80) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uq_payments_reference (reference),
  KEY idx_payments_invoice (invoice_id),
  CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  actor_name VARCHAR(120) NULL,
  actor_role VARCHAR(50) NULL,
  action VARCHAR(80) NOT NULL,
  entity_name VARCHAR(80) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  outcome VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  ip_address VARCHAR(45) NULL,
  description VARCHAR(500) NULL,
  details JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_created (created_at),
  KEY idx_audit_user (user_id),
  KEY idx_audit_action (action),
  KEY idx_audit_entity_name (entity_name),
  KEY idx_audit_entity_id (entity_id),
  KEY idx_audit_outcome (outcome),
  KEY idx_audit_entity (entity_name, entity_id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, user_id BIGINT UNSIGNED NULL,
  title VARCHAR(160) NOT NULL, message TEXT NOT NULL,
  type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM', read_status BOOLEAN NOT NULL DEFAULT FALSE,
  entity_name VARCHAR(80) NULL, entity_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id), KEY idx_notifications_user (user_id), KEY idx_notifications_read (user_id, read_status),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS support_tickets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ticket_reference VARCHAR(60) NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  assigned_to BIGINT UNSIGNED NULL,
  subject VARCHAR(200) NOT NULL,
  description TEXT NOT NULL,
  category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
  priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_support_tickets_ref (ticket_reference),
  KEY idx_tickets_created_by (created_by),
  KEY idx_tickets_assigned_to (assigned_to),
  KEY idx_tickets_status (status),
  KEY idx_tickets_priority (priority),
  KEY idx_tickets_category (category),
  CONSTRAINT fk_tickets_created_by FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_tickets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ticket_id BIGINT UNSIGNED NOT NULL,
  sender_id BIGINT UNSIGNED NOT NULL,
  message_body TEXT NOT NULL,
  is_internal_note BOOLEAN NOT NULL DEFAULT FALSE,
  attachment_filename VARCHAR(255) NULL,
  attachment_storage_key VARCHAR(500) NULL,
  attachment_mime_type VARCHAR(120) NULL,
  attachment_size_bytes BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ticket_msgs_ticket (ticket_id),
  KEY idx_ticket_msgs_sender (sender_id),
  CONSTRAINT fk_ticket_msgs_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
  CONSTRAINT fk_ticket_msgs_sender FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  ticket_id BIGINT UNSIGNED NOT NULL,
  actor_id BIGINT UNSIGNED NOT NULL,
  previous_status VARCHAR(30) NULL,
  new_status VARCHAR(30) NOT NULL,
  note VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_status_hist_ticket (ticket_id),
  CONSTRAINT fk_status_hist_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
  CONSTRAINT fk_status_hist_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  ticket_id BIGINT UNSIGNED NULL,
  rating INT UNSIGNED NULL,
  category VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
  feedback_text TEXT NOT NULL,
  review_state VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_feedback_user (user_id),
  KEY idx_feedback_ticket (ticket_id),
  KEY idx_feedback_state (review_state),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  CONSTRAINT fk_feedback_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS help_articles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  slug VARCHAR(200) NOT NULL,
  category VARCHAR(50) NOT NULL,
  content TEXT NOT NULL,
  published BOOLEAN NOT NULL DEFAULT TRUE,
  author_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_help_articles_slug (slug),
  KEY idx_help_articles_category (category),
  CONSTRAINT fk_help_articles_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;


