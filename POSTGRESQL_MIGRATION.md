# ResortsLite - PostgreSQL Migration Guide

## Overview

This application has been successfully migrated from SQL Server to PostgreSQL 16. This document outlines the changes made and provides guidance for deployment and configuration.

## Database Migration Summary

### 1. Package Dependencies

**Updated Dependencies:**
- PostgreSQL JDBC Driver: `42.7.1`
- Spring Boot Data JPA: `3.2.0`
- HikariCP Connection Pool (included in Spring Boot)
- Spring Boot Validation: Added for entity validation

**Removed Dependencies:**
- SQL Server JDBC Driver
- SQL Server-specific libraries

### 2. Connection Configuration

**PostgreSQL Connection String Format:**
```properties
spring.datasource.url=jdbc:postgresql://<host>:<port>/<database>
spring.datasource.username=<username>
spring.datasource.password=<password>
spring.datasource.driver-class-name=org.postgresql.Driver
```

**Environment Variables (Recommended for Production):**
- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_POOL_MAX_SIZE`: Maximum connection pool size (default: 10)
- `DB_POOL_MIN_IDLE`: Minimum idle connections (default: 5)

### 3. Database Context Configuration

**Key Changes:**
- Replaced SQL Server dialect with `PostgreSQLDialect`
- Configured snake_case naming convention for tables and columns
- Set default schema to `public`
- Enabled batch processing for better performance
- Configured UTC timezone for timestamp handling

### 4. Entity Type Mappings

**PostgreSQL Type Mappings:**
- `VARCHAR(n)` → String with `@Column(length = n)`
- `DATE` → `java.time.LocalDate`
- `TIMESTAMP` → `java.time.LocalDateTime`
- `INTEGER` → `int` or `Integer`
- `BIGINT` → `long` or `Long`
- `BOOLEAN` → `boolean` or `Boolean`

### 5. Data Access Layer Modernization

**Changes Made:**
- Replaced raw JDBC (`JdbcTemplate`) with JPA Repository pattern
- Added `@Transactional` annotations for proper transaction management
- Implemented type-safe query methods using Spring Data JPA
- Added validation annotations to entity classes

### 6. SQL Query Updates

**PostgreSQL-Specific Features:**
- Used parameterized queries to prevent SQL injection
- Implemented JPQL queries for database-agnostic operations
- Added indexes for performance optimization
- Created triggers for automatic timestamp updates

### 7. Configuration Externalization

**Cloud-Ready Configuration:**
- Externalized all hardcoded credentials to environment variables
- Made file paths configurable for container compatibility
- Used `@Value` annotations for dynamic configuration
- Supported AWS Parameter Store / Secrets Manager integration

## Deployment Guide

### Local Development Setup

1. **Install PostgreSQL 16:**
   ```bash
   # Ubuntu/Debian
   sudo apt-get install postgresql-16
   
   # macOS
   brew install postgresql@16
   ```

2. **Create Database:**
   ```sql
   CREATE DATABASE resortdb;
   CREATE USER postgres WITH PASSWORD 'postgres';
   GRANT ALL PRIVILEGES ON DATABASE resortdb TO postgres;
   ```

3. **Run Schema Initialization:**
   ```bash
   psql -U postgres -d resortdb -f src/main/resources/schema.sql
   ```

4. **Configure Application:**
   ```bash
   export DB_URL=jdbc:postgresql://localhost:5432/resortdb
   export DB_USERNAME=postgres
   export DB_PASSWORD=postgres
   ```

5. **Run Application:**
   ```bash
   mvn spring-boot:run
   ```

### Docker Deployment

1. **Create Docker Compose File:**
   ```yaml
   version: '3.8'
   services:
     postgres:
       image: postgres:16
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
       environment:
         DB_URL: jdbc:postgresql://postgres:5432/resortdb
         DB_USERNAME: postgres
         DB_PASSWORD: postgres
       ports:
         - "8080:8080"
       depends_on:
         - postgres
   
   volumes:
     postgres_data:
   ```

2. **Build and Run:**
   ```bash
   docker-compose up -d
   ```

### AWS Cloud Deployment

1. **RDS PostgreSQL Setup:**
   - Create RDS PostgreSQL 16 instance
   - Configure security groups for VPC access
   - Store credentials in AWS Secrets Manager

2. **ECS/EKS Configuration:**
   ```bash
   export DB_URL=jdbc:postgresql://<rds-endpoint>:5432/resortdb
   export DB_USERNAME=$(aws secretsmanager get-secret-value --secret-id db-username --query SecretString --output text)
   export DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-password --query SecretString --output text)
   ```

3. **Application Load Balancer:**
   - Configure health check endpoint: `/actuator/health`
   - Enable HTTPS with ACM certificate
   - Configure target group with proper health checks

## Performance Optimization

### Connection Pool Settings

**Recommended Settings for Production:**
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Query Optimization

1. **Indexes Created:**
   - `idx_bookings_guest` on `guest` column
   - `idx_bookings_room` on `room` column
   - `idx_bookings_checkin` on `checkin` column
   - `idx_bookings_confirmation` on `confirmation_code` column

2. **Batch Processing:**
   - Enabled batch inserts with `hibernate.jdbc.batch_size=20`
   - Enabled order inserts/updates for better batching

### Monitoring

**Key Metrics to Monitor:**
- Connection pool utilization
- Query execution time
- Transaction rollback rate
- Database connection errors

**Actuator Endpoints:**
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Info: `http://localhost:8080/actuator/info`

## Migration Validation

### Functional Testing

1. **CRUD Operations:**
   ```bash
   # Create booking
   curl -X POST "http://localhost:8080/api/bookings/create" \
     -d "guestName=John Doe&roomType=SUITE&checkIn=2024-03-01&checkOut=2024-03-05"
   
   # Get booking
   curl "http://localhost:8080/api/bookings/status/BK-12345678"
   
   # Check availability
   curl "http://localhost:8080/api/bookings/availability?roomType=DELUXE"
   ```

2. **Database Verification:**
   ```sql
   -- Check table structure
   \d bookings
   
   -- Verify data
   SELECT * FROM bookings LIMIT 10;
   
   -- Check indexes
   \di
   ```

### Performance Testing

1. **Connection Pool Test:**
   ```bash
   # Monitor active connections
   SELECT count(*) FROM pg_stat_activity WHERE datname = 'resortdb';
   ```

2. **Query Performance:**
   ```sql
   -- Enable query timing
   \timing on
   
   -- Test query performance
   EXPLAIN ANALYZE SELECT * FROM bookings WHERE guest = 'John Doe';
   ```

## Troubleshooting

### Common Issues

1. **Connection Refused:**
   - Check PostgreSQL is running: `sudo systemctl status postgresql`
   - Verify firewall rules allow port 5432
   - Check `pg_hba.conf` for authentication settings

2. **Authentication Failed:**
   - Verify credentials in environment variables
   - Check user permissions in PostgreSQL
   - Ensure password is correctly escaped

3. **Schema Not Found:**
   - Run schema initialization script
   - Verify `spring.jpa.properties.hibernate.default_schema=public`
   - Check database connection URL includes correct database name

4. **Performance Issues:**
   - Increase connection pool size
   - Add missing indexes
   - Enable query logging to identify slow queries
   - Consider using connection pooling at application level

## Security Considerations

### Production Checklist

- [ ] Store credentials in AWS Secrets Manager or Parameter Store
- [ ] Enable SSL/TLS for database connections
- [ ] Use IAM authentication for RDS (if on AWS)
- [ ] Implement connection encryption
- [ ] Enable audit logging
- [ ] Configure proper security groups
- [ ] Use least privilege principle for database users
- [ ] Enable automated backups
- [ ] Implement disaster recovery plan

### SSL Configuration

```properties
spring.datasource.url=jdbc:postgresql://host:5432/resortdb?ssl=true&sslmode=require
```

## Migration Compliance Report

### Rules Implemented

1. ✅ **Package Dependencies**: Updated to PostgreSQL driver and removed SQL Server dependencies
2. ✅ **Connection Strings**: Converted to PostgreSQL format with environment variable support
3. ✅ **DbContext Configuration**: Replaced SQL Server dialect with PostgreSQL dialect
4. ✅ **Entity Type Mappings**: Updated all type mappings for PostgreSQL compatibility
5. ✅ **SQL Query Updates**: Replaced raw JDBC with JPA repository pattern
6. ✅ **Configuration Externalization**: Moved all hardcoded values to environment variables
7. ✅ **Transaction Management**: Added proper @Transactional annotations
8. ✅ **Validation**: Added entity validation with Jakarta Bean Validation
9. ✅ **Performance Optimization**: Configured connection pooling and batch processing
10. ✅ **Cloud Readiness**: Externalized all configuration for cloud deployment

## Support and Maintenance

### Contact Information
- Application Owner: Development Team
- Database Administrator: DBA Team
- Cloud Infrastructure: DevOps Team

### Documentation
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- PostgreSQL Documentation: https://www.postgresql.org/docs/16/
- Hibernate Documentation: https://hibernate.org/orm/documentation/

### Version History
- v1.0.0 (2024-03-01): Initial PostgreSQL migration
- Application: Spring Boot 3.2.0
- Java: 21
- PostgreSQL: 16
- HikariCP: Managed by Spring Boot

---

**Last Updated**: 2024-03-01  
**Migration Status**: ✅ Complete  
**Validation Status**: ✅ Passed
