-- PostgreSQL 16 Schema Initialisation Script
-- Migrated from H2/MySQL to PostgreSQL 16
-- All identifiers use snake_case per PostgreSQL naming conventions

-- Create bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id          VARCHAR(50)  PRIMARY KEY,
    guest       VARCHAR(255) NOT NULL,
    room        VARCHAR(100) NOT NULL,
    checkin     DATE         NOT NULL,
    checkout    DATE         NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index on guest name for lookup performance
CREATE INDEX IF NOT EXISTS idx_bookings_guest ON bookings (guest);

-- Index on room type for availability queries
CREATE INDEX IF NOT EXISTS idx_bookings_room ON bookings (room);
