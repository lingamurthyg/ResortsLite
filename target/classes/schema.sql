-- Database schema for ResortsLite application
-- Creates the bookings table for H2 in-memory database

CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(50) PRIMARY KEY,
    guest VARCHAR(255) NOT NULL,
    room VARCHAR(100) NOT NULL,
    checkin VARCHAR(50) NOT NULL,
    checkout VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_bookings_guest ON bookings(guest);
CREATE INDEX IF NOT EXISTS idx_bookings_checkin ON bookings(checkin);
