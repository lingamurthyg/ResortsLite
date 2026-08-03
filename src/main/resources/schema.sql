-- Schema for ResortsLite Application
-- H2 Database initialization script

CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(50) PRIMARY KEY,
    guest VARCHAR(255) NOT NULL,
    room VARCHAR(50) NOT NULL,
    checkin DATE NOT NULL,
    checkout DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_bookings_guest ON bookings(guest);
CREATE INDEX IF NOT EXISTS idx_bookings_checkin ON bookings(checkin);
