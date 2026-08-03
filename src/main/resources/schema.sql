-- Schema for ResortsLite Application
-- PostgreSQL Database initialization script

-- Drop table if exists (for clean initialization)
DROP TABLE IF EXISTS bookings CASCADE;

-- Create bookings table with PostgreSQL-specific features
CREATE TABLE bookings (
    id VARCHAR(50) PRIMARY KEY,
    guest VARCHAR(255) NOT NULL,
    room VARCHAR(50) NOT NULL,
    checkin DATE NOT NULL,
    checkout DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for faster lookups
CREATE INDEX idx_bookings_guest ON bookings(guest);
CREATE INDEX idx_bookings_checkin ON bookings(checkin);

-- Add comments for documentation (PostgreSQL feature)
COMMENT ON TABLE bookings IS 'Stores resort booking information';
COMMENT ON COLUMN bookings.id IS 'Unique booking identifier';
COMMENT ON COLUMN bookings.guest IS 'Guest name';
COMMENT ON COLUMN bookings.room IS 'Room type (STANDARD, DELUXE, SUITE, VILLA)';
COMMENT ON COLUMN bookings.checkin IS 'Check-in date';
COMMENT ON COLUMN bookings.checkout IS 'Check-out date';
COMMENT ON COLUMN bookings.created_at IS 'Timestamp when booking was created';
