-- PostgreSQL Database Initialization Script for ResortsLite
-- This script creates the necessary schema and tables for the application

-- Create schema if not exists (default is 'public')
CREATE SCHEMA IF NOT EXISTS public;

-- Set search path
SET search_path TO public;

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS bookings CASCADE;

-- Create bookings table with PostgreSQL-specific features
CREATE TABLE bookings (
    id VARCHAR(50) PRIMARY KEY,
    guest VARCHAR(255) NOT NULL,
    room VARCHAR(50) NOT NULL,
    checkin DATE NOT NULL,
    checkout DATE NOT NULL,
    confirmation_code VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dates CHECK (checkout > checkin)
);

-- Create indexes for better query performance
CREATE INDEX idx_bookings_guest ON bookings(guest);
CREATE INDEX idx_bookings_room ON bookings(room);
CREATE INDEX idx_bookings_checkin ON bookings(checkin);
CREATE INDEX idx_bookings_confirmation ON bookings(confirmation_code);

-- Create a function to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at on row update
CREATE TRIGGER update_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for testing
INSERT INTO bookings (id, guest, room, checkin, checkout, confirmation_code, created_at, updated_at)
VALUES 
    ('BK-SAMPLE01', 'John Smith', 'SUITE', '2024-03-01', '2024-03-05', 'CONF-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BK-SAMPLE02', 'Jane Doe', 'DELUXE', '2024-03-03', '2024-03-07', 'CONF-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BK-SAMPLE03', 'Bob Johnson', 'STANDARD', '2024-03-10', '2024-03-15', 'CONF-003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Grant necessary permissions (adjust as needed for your environment)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- Display table information
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM 
    information_schema.columns
WHERE 
    table_schema = 'public' 
    AND table_name = 'bookings'
ORDER BY 
    ordinal_position;
