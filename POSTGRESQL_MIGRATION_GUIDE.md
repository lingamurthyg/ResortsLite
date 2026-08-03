# PostgreSQL Migration Guide - ResortsLite Application

## Overview
This document describes the migration of the ResortsLite application from H2 in-memory database to PostgreSQL 16.

## Migration Summary

### Changes Made

#### 1. Package Dependencies (pom.xml)
**Changed:**
- Removed: `com.h2database:h2:2.2.224`
- Added: `org.postgresql:postgresql` (managed by Spring Boot parent)

**Impact:** Critical - Application now uses PostgreSQL JDBC driver instead of H2

#### 2. Database Configuration (application.properties)
**Changed:**
```properties
# Before (H2):
spring.datasource.url=jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=

# After (PostgreSQL):
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:resortdb}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
```

**Impact:** Critical - Connection string now points to PostgreSQL with environment variable support

#### 3. Database Schema (schema.sql)
**Changed:**
- Removed H2-specific `IF NOT EXISTS` clauses
- Added `DROP TABLE IF EXISTS bookings CASCADE` for clean initialization
- Added PostgreSQL table comments for documentation
- Maintained PostgreSQL-compatible syntax

**Impact:** Medium - Schema is now PostgreSQL-optimized with better documentation

#### 4. SQL Injection Fixes (BookingService.java)
**Changed:**
```java
// Before (Vulnerable):
String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES ('" 
    + bookingId + "', '" + guestName + "', '" + roomType + "', '" + checkIn + "', '" + checkOut + "')";
jdbcTemplate.execute(sql);

// After (Secure):
String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?::date, ?::date)";
jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);
```

**Impact:** Critical - Fixed SQL injection vulnerabilities using parameterized queries

**Changed:**
```java
// Before (Vulnerable):
String sql = "SELECT * FROM bookings WHERE id = '" + bookingId + "'";
result = jdbcTemplate.queryForMap(sql);

// After (Secure):
String sql = "SELECT * FROM bookings WHERE id = ?";
result = jdbcTemplate.queryForMap(sql, bookingId);
```

**Impact:** Critical - Fixed SQL injection vulnerability in query method

## Environment Variables

The application now supports the following environment variables for database configuration:

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | PostgreSQL server hostname |
| DB_PORT | 5432 | PostgreSQL server port |
| DB_NAME | resortdb | Database name |
| DB_USERNAME | postgres | Database username |
| DB_PASSWORD | postgres | Database password |

## PostgreSQL Setup Instructions

### 1. Install PostgreSQL 16
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql-16

# macOS (using Homebrew)
brew install postgresql@16

# Windows
# Download installer from https://www.postgresql.org/download/windows/
```

### 2. Create Database
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE resortdb;

# Create user (optional)
CREATE USER resortapp WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE resortdb TO resortapp;
```

### 3. Configure Application
Set environment variables before running the application:

```bash
# Linux/macOS
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=resortdb
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Windows (PowerShell)
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="resortdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
```

### 4. Run Application
```bash
mvn spring-boot:run
```

## Docker Deployment

### Using Docker Compose
Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: resortdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: resortdb
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
    depends_on:
      - postgres

volumes:
  postgres_data:
```

Run with:
```bash
docker-compose up
```

## Testing the Migration

### 1. Verify Database Connection
```bash
curl http://localhost:8080/health
```

### 2. Test Booking Creation
```bash
curl -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "guestName": "John Doe",
    "roomType": "DELUXE",
    "checkIn": "2024-04-01",
    "checkOut": "2024-04-05"
  }'
```

### 3. Run Unit Tests
```bash
mvn test
```

## Security Improvements

### SQL Injection Prevention
All SQL queries now use parameterized statements:
- ✅ `createBooking()` - Uses `jdbcTemplate.update()` with parameters
- ✅ `getBookingById()` - Uses `jdbcTemplate.queryForMap()` with parameters

### PostgreSQL-Specific Features
- Type casting for date parameters: `?::date`
- Proper transaction handling
- Connection pooling via HikariCP (default in Spring Boot)

## Performance Considerations

### Connection Pooling
Spring Boot automatically configures HikariCP for PostgreSQL. Default settings:
- Maximum pool size: 10
- Minimum idle: 10
- Connection timeout: 30 seconds

To customize, add to `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Indexes
The following indexes are created for optimal query performance:
- `idx_bookings_guest` - For guest name lookups
- `idx_bookings_checkin` - For check-in date queries

## Troubleshooting

### Connection Refused
**Problem:** `Connection refused: connect`
**Solution:** Ensure PostgreSQL is running and accessible on the configured host/port

### Authentication Failed
**Problem:** `FATAL: password authentication failed`
**Solution:** Verify DB_USERNAME and DB_PASSWORD environment variables

### Database Does Not Exist
**Problem:** `FATAL: database "resortdb" does not exist`
**Solution:** Create the database using `CREATE DATABASE resortdb;`

### Schema Not Created
**Problem:** Tables not found
**Solution:** Ensure `spring.sql.init.mode=always` is set in application.properties

## Rollback Plan

To rollback to H2 (for development only):

1. Restore original pom.xml dependency:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
    <scope>runtime</scope>
</dependency>
```

2. Restore original application.properties:
```properties
spring.datasource.url=jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
```

3. Rebuild: `mvn clean install`

## Migration Checklist

- [x] Updated pom.xml with PostgreSQL driver
- [x] Removed H2 dependency
- [x] Updated application.properties with PostgreSQL configuration
- [x] Added environment variable support for database credentials
- [x] Updated schema.sql for PostgreSQL compatibility
- [x] Fixed SQL injection vulnerabilities in BookingService
- [x] Converted to parameterized queries
- [x] Added PostgreSQL-specific type casting
- [x] Documented migration process
- [x] Created troubleshooting guide

## Next Steps

1. **Production Deployment:**
   - Use AWS RDS PostgreSQL or Azure Database for PostgreSQL
   - Enable SSL/TLS for database connections
   - Configure automated backups
   - Set up monitoring and alerting

2. **Security Hardening:**
   - Use AWS Secrets Manager or Azure Key Vault for credentials
   - Enable connection encryption
   - Implement least-privilege database user
   - Enable audit logging

3. **Performance Optimization:**
   - Analyze query performance with EXPLAIN ANALYZE
   - Add additional indexes based on query patterns
   - Configure connection pool for production load
   - Enable query caching if needed

## Support

For issues or questions regarding this migration:
- Review PostgreSQL documentation: https://www.postgresql.org/docs/16/
- Check Spring Boot Data Access documentation: https://docs.spring.io/spring-boot/docs/current/reference/html/data.html
- Review application logs for detailed error messages
