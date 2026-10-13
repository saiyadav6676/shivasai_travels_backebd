ALTER TABLE buses
  ADD COLUMN fleet_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  ADD COLUMN fuel_level_percent INT NOT NULL DEFAULT 75,
  ADD COLUMN parking_location VARCHAR(100) NOT NULL DEFAULT 'Main depot',
  ADD COLUMN maintenance_due DATE NULL,
  ADD COLUMN last_service DATE NULL;

CREATE TABLE charter_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  customer_name VARCHAR(120) NOT NULL,
  email VARCHAR(180) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  occasion VARCHAR(80) NOT NULL,
  travel_date DATE NOT NULL,
  pickup_location VARCHAR(180) NOT NULL,
  destination VARCHAR(180) NOT NULL,
  passenger_count INT NOT NULL,
  notes VARCHAR(1000) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX ix_charter_created(created_at)
);
