-- Migration Script for Step 11: Notifications & Vessel Location Tracking
USE digital_port_shipping;

-- 1. Create notifications table if not exists
CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  title VARCHAR(160) NOT NULL,
  message TEXT NOT NULL,
  type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
  read_status BOOLEAN NOT NULL DEFAULT FALSE,
  entity_name VARCHAR(80) NULL,
  entity_id BIGINT UNSIGNED NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notifications_user (user_id),
  KEY idx_notifications_read (user_id, read_status),
  CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 2. Add location tracking columns to vessels table safely if not present
SET @dbname = DATABASE();
SET @tablename = "vessels";
SET @columnname = "latitude";
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = @columnname
  ) > 0,
  "SELECT 1",
  "ALTER TABLE vessels ADD COLUMN latitude FLOAT NULL, ADD COLUMN longitude FLOAT NULL, ADD COLUMN current_location VARCHAR(120) NULL, ADD COLUMN last_tracked_at TIMESTAMP NULL;"
));
PREPARE add_cols FROM @preparedStatement;
EXECUTE add_cols;
DEALLOCATE PREPARE add_cols;
