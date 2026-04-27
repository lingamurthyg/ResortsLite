# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready for deployment on Google Cloud Platform (GCP). All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System Dependencies (cr-java-0061, cr-java-0062, cr-java-0063)
**Problem**: Application used hardcoded file paths and local file system operations that don't work in ephemeral container environments.

**Solution**: 
- Replaced all local file operations with Google Cloud Storage (GCS)
- Implemented `ReportService` to use GCS SDK for file uploads/downloads
- Configured bucket name and folder paths via environment variables
- Reports and backups now stored in GCS buckets for durability

**Files Modified**:
- `ReportService.java` - Migrated to GCS operations
- `pom.xml` - Added `google-cloud-storage` dependency
- `application.properties` - Added GCS configuration

### 2. Hard-coded Database Credentials (cr-java-0069)
**Problem**: Database credentials were hardcoded in source code, creating security vulnerabilities.

**Solution**:
- Externalized all database credentials to environment variables
- Configured Spring Boot to read from `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- Credentials should be stored in Google Secret Manager in production

**Files Modified**:
- `BookingService.java` - Removed hardcoded credentials
- `application.properties` - Externalized database configuration

### 3. Hard-coded Environment URLs (cr-java-0071)
**Problem**: Service endpoints were hardcoded, preventing environment portability.

**Solution**:
- Externalized all service URLs to environment variables
- Configured via `PAYMENT_SERVICE_URL`, `INVENTORY_SERVICE_URL`, `NOTIFICATION_SERVICE_URL`
- Report download URLs now use `REPORT_DOWNLOAD_BASE_URL`

**Files Modified**:
- `BookingController.java` - Uses `@Value` injection for URLs
- `BookingService.java` - Uses externalized payment API URL
- `ReportService.java` - Uses externalized report download URL
- `application.properties` - Added URL configuration

### 4. Hard-coded Ports (cr-java-0077)
**Problem**: Server port was hardcoded, preventing dynamic port assignment by Cloud Run/GKE.

**Solution**:
- Changed `server.port` to use `${PORT:8080}` environment variable
- Cloud Run automatically sets PORT environment variable
- Falls back to 8080 for local development

**Files Modified**:
- `application.properties` - Dynamic port configuration
- `ReportService.java` - Uses `@Value` for port injection

### 5. HTTP Session State Storage (cr-java-0065)
**Problem**: Application stored state in HTTP sessions, preventing horizontal scaling.

**Solution**:
- Implemented distributed session management using Redis
- Integrated Spring Session Data Redis
- Configured Google Cloud Memorystore for Redis
- Sessions now shared across all application instances

**Files Modified**:
- `BookingController.java` - Replaced session with Redis operations
- `RedisConfig.java` - New configuration class
- `pom.xml` - Added Spring Session and Redis dependencies
- `application.properties` - Added Redis configuration

### 6. In-Memory Caching Without TTL (cr-java-0067)
**Problem**: Static in-memory cache caused memory leaks and cache inconsistency across instances.

**Solution**:
- Replaced static HashMap with Redis-backed cache
- Implemented TTL (1 hour) for all cache entries
- Cache now consistent across all instances
- Prevents memory exhaustion

**Files Modified**:
- `BookingController.java` - Uses RedisTemplate for caching
- `RedisConfig.java` - Configured Redis cache
- `application.properties` - Added cache TTL configuration

### 7. File-based Authentication (cr-java-0090)
**Problem**: Authentication credentials stored in local files.

**Solution**:
- Externalized auth credentials path to environment variable
- Supports classpath resources or GCS paths
- Production should use Google Secret Manager

**Files Modified**:
- `BookingService.java` - Uses ResourceLoader for credentials
- `application.properties` - Added `AUTH_CREDENTIALS_PATH` configuration

### 8. Clock/Time Dependencies (cr-java-0111)
**Problem**: Application relied on server-local timezone, causing inconsistencies in distributed environments.

**Solution**:
- Standardized all timestamp operations on UTC
- Replaced `SimpleDateFormat` with `DateTimeFormatter.ISO_INSTANT`
- Eliminated timezone dependencies

**Files Modified**:
- `ReportService.java` - Uses UTC for all timestamps

## Architecture Changes

### New Dependencies Added
```xml
<!-- Distributed Session Management -->
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Google Cloud Storage -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
</dependency>

<!-- Spring Cloud GCP -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>spring-cloud-gcp-starter-secretmanager</artifactId>
</dependency>
```

### New Configuration Classes
- `RedisConfig.java` - Redis and session management configuration
- `GcpConfig.java` - Google Cloud Storage configuration

### Configuration Files
- `application.properties` - Externalized all configuration
- `.env.template` - Environment variable template

## Deployment Requirements

### Required GCP Services
1. **Google Cloud Storage** - For file storage (reports, backups)
2. **Google Cloud Memorystore for Redis** - For distributed sessions and caching
3. **Google Secret Manager** - For secure credential storage (recommended)
4. **Cloud Run or GKE** - For application hosting

### Environment Variables
See `.env.template` for complete list of required environment variables.

Key variables:
- `PORT` - Server port (auto-set by Cloud Run)
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` - Database connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` - Redis connection
- `GCS_BUCKET_NAME` - Google Cloud Storage bucket
- `GCP_PROJECT_ID` - GCP project identifier
- Service endpoint URLs (PAYMENT_SERVICE_URL, etc.)

## Local Development

### Prerequisites
- Java 8 or higher
- Maven 3.6+
- Redis server (or use Docker)
- Google Cloud SDK (optional)

### Setup
1. Copy `.env.template` to `.env` and configure
2. Start Redis: `docker run -p 6379:6379 redis:alpine`
3. Run application: `mvn spring-boot:run`

### Testing with Local Storage
For local development without GCS:
- Set `GCS_BUCKET_NAME` to a test bucket
- Use GCS emulator or create a development bucket

## Cloud Deployment

### Cloud Run Deployment
```bash
# Build container
gcloud builds submit --tag gcr.io/PROJECT_ID/resorts-lite

# Deploy to Cloud Run
gcloud run deploy resorts-lite \
  --image gcr.io/PROJECT_ID/resorts-lite \
  --platform managed \
  --region us-central1 \
  --set-env-vars GCS_BUCKET_NAME=your-bucket,REDIS_HOST=your-redis-ip
```

### GKE Deployment
Use Kubernetes ConfigMaps and Secrets for environment variables.

## Security Considerations

1. **Never commit credentials** - Use Secret Manager or environment variables
2. **Use HTTPS** - All external URLs should use HTTPS
3. **Enable authentication** - Configure Cloud IAM for service-to-service auth
4. **Rotate credentials** - Use Secret Manager for automatic rotation
5. **Network security** - Use VPC and firewall rules

## Monitoring and Logging

The application is now compatible with:
- Google Cloud Logging (structured logs)
- Google Cloud Monitoring (metrics)
- Google Cloud Trace (distributed tracing)

## 12-Factor App Compliance

This application now follows 12-factor app principles:
- ✅ Codebase - Single codebase tracked in version control
- ✅ Dependencies - Explicitly declared in pom.xml
- ✅ Config - Externalized to environment variables
- ✅ Backing services - Treated as attached resources
- ✅ Build, release, run - Strictly separated
- ✅ Processes - Stateless (sessions in Redis)
- ✅ Port binding - Dynamic port binding
- ✅ Concurrency - Horizontally scalable
- ✅ Disposability - Fast startup and graceful shutdown
- ✅ Dev/prod parity - Same backing services
- ✅ Logs - Treated as event streams
- ✅ Admin processes - Run as one-off processes

## Support

For issues or questions, refer to the GCP documentation:
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Cloud Storage Documentation](https://cloud.google.com/storage/docs)
- [Memorystore for Redis Documentation](https://cloud.google.com/memorystore/docs/redis)
