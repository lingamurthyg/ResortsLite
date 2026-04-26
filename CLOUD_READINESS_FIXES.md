# Cloud Readiness Fixes - Deployment Guide

## Overview
This document describes the cloud readiness fixes applied to the ResortsLite application for Google Cloud Platform (GCP) deployment.

## Fixed Cloud Readiness Issues

### 1. File System Dependencies (cr-java-0061, cr-java-0062, cr-java-0063)
**Issue**: Hard-coded file paths and local file system operations
**Fix**: Migrated to Google Cloud Storage (GCS)
- Replaced `/var/legacy/reports/` with GCS bucket operations
- Replaced `java.io.File` operations with GCS Java SDK
- All report generation now stores files in GCS bucket

**Configuration Required**:
```bash
GCS_BUCKET_NAME=resorts-reports-bucket
GCP_PROJECT_ID=your-project-id
```

### 2. Hard-coded Database Credentials (cr-java-0069)
**Issue**: Database credentials embedded in source code
**Fix**: Migrated to Google Secret Manager
- Database host, username, and password now retrieved from Secret Manager
- Credentials externalized using Spring Cloud GCP Secret Manager

**Configuration Required**:
```bash
DB_HOST=sm://projects/YOUR_PROJECT_ID/secrets/db-host
DB_USER=sm://projects/YOUR_PROJECT_ID/secrets/db-user
DB_PASS=sm://projects/YOUR_PROJECT_ID/secrets/db-pass
```

**Secret Manager Setup**:
```bash
# Create secrets in Google Secret Manager
gcloud secrets create db-host --data-file=- <<< "your-db-host"
gcloud secrets create db-user --data-file=- <<< "your-db-user"
gcloud secrets create db-pass --data-file=- <<< "your-db-password"
```

### 3. Hard-coded Environment URLs (cr-java-0071)
**Issue**: Hard-coded service endpoints in source code
**Fix**: Externalized to environment variables with HTTPS
- Payment endpoint: `PAYMENT_ENDPOINT`
- Inventory endpoint: `INVENTORY_ENDPOINT`
- Notification endpoint: `NOTIFICATION_ENDPOINT`
- Reports base URL: `REPORTS_BASE_URL`

**Configuration Required**:
```bash
PAYMENT_ENDPOINT=https://payment-service/charge
INVENTORY_ENDPOINT=https://inventory-service/rooms/available
NOTIFICATION_ENDPOINT=https://notification-service/send
REPORTS_BASE_URL=https://reports-service/download
```

### 4. Hard-coded Ports (cr-java-0077)
**Issue**: Fixed port 8080 preventing dynamic port assignment
**Fix**: Externalized to `PORT` environment variable
- Server port now configurable via environment variable
- Compatible with Cloud Run dynamic port assignment

**Configuration Required**:
```bash
PORT=8080  # Cloud Run will override this automatically
```

### 5. HTTP Session State Storage (cr-java-0065)
**Issue**: Session data stored in local HTTP session, preventing horizontal scaling
**Fix**: Migrated to Memorystore for Redis
- Session data now stored in distributed Redis cache
- Enables stateless application architecture
- Supports horizontal scaling and load balancing

**Configuration Required**:
```bash
REDIS_HOST=10.0.0.3  # Memorystore for Redis IP
REDIS_PORT=6379
REDIS_PASSWORD=sm://projects/YOUR_PROJECT_ID/secrets/redis-password
```

**Memorystore Setup**:
```bash
# Create Memorystore for Redis instance
gcloud redis instances create resorts-cache \
    --size=1 \
    --region=us-central1 \
    --redis-version=redis_6_x
```

### 6. In-Memory Caching Without TTL (cr-java-0067)
**Issue**: Unbounded in-memory cache causing memory exhaustion
**Fix**: Migrated to Redis cache with TTL
- Cache entries now have configurable TTL
- Prevents memory exhaustion
- Consistent cache across all instances

**Configuration Required**:
```bash
CACHE_TTL=3600000  # 1 hour in milliseconds
```

### 7. File-based Authentication (cr-java-0090)
**Issue**: Authentication credentials stored in local files
**Fix**: Migrated to Google Secret Manager
- Authentication credentials retrieved from Secret Manager
- Supports distributed authentication without file dependencies

**Configuration Required**:
```bash
AUTH_CREDENTIALS_PATH=sm://projects/YOUR_PROJECT_ID/secrets/auth-credentials
```

## Dependencies Added

### Maven Dependencies (pom.xml)
1. **Google Cloud Storage**: `google-cloud-storage:2.22.3`
2. **Spring Cloud GCP Secret Manager**: `spring-cloud-gcp-starter-secretmanager`
3. **Spring Session Redis**: `spring-session-data-redis`
4. **Spring Data Redis**: `spring-boot-starter-data-redis`
5. **Lettuce Core**: Redis client for Spring

## Configuration Files Modified

### 1. application.properties
- Externalized all hard-coded values to environment variables
- Added Redis configuration for session management
- Added GCS configuration for file storage
- Added Secret Manager configuration
- Added HikariCP connection pool settings

### 2. pom.xml
- Added GCP dependencies
- Added Redis dependencies
- Added Spring Cloud GCP dependencies

## New Files Created

### 1. RedisConfig.java
- Configures Redis connection factory
- Enables distributed session management
- Configures RedisTemplate for cache operations

### 2. GcpConfig.java
- Configures Google Cloud Storage client
- Enables GCP service integration

### 3. .env.template
- Template for environment configuration
- Documents all required environment variables

## Deployment Steps

### 1. Set up GCP Resources

#### Create GCS Bucket
```bash
gsutil mb -p YOUR_PROJECT_ID -c STANDARD -l us-central1 gs://resorts-reports-bucket/
```

#### Create Memorystore for Redis
```bash
gcloud redis instances create resorts-cache \
    --size=1 \
    --region=us-central1 \
    --redis-version=redis_6_x
```

#### Create Secrets in Secret Manager
```bash
gcloud secrets create db-host --data-file=- <<< "your-db-host"
gcloud secrets create db-user --data-file=- <<< "your-db-user"
gcloud secrets create db-pass --data-file=- <<< "your-db-password"
gcloud secrets create redis-password --data-file=- <<< "your-redis-password"
gcloud secrets create auth-credentials --data-file=credentials.json
```

### 2. Configure Environment Variables

Copy `.env.template` to `.env` and configure with actual values:
```bash
cp .env.template .env
# Edit .env with your actual configuration
```

### 3. Grant IAM Permissions

```bash
# Grant Secret Manager access
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:YOUR_SERVICE_ACCOUNT@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"

# Grant Cloud Storage access
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:YOUR_SERVICE_ACCOUNT@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/storage.objectAdmin"
```

### 4. Deploy to Cloud Run (Example)

```bash
# Build the application
mvn clean package

# Deploy to Cloud Run
gcloud run deploy resorts-lite \
    --source . \
    --region=us-central1 \
    --allow-unauthenticated \
    --set-env-vars="GCP_PROJECT_ID=YOUR_PROJECT_ID,GCS_BUCKET_NAME=resorts-reports-bucket,REDIS_HOST=10.0.0.3"
```

## Testing

### 1. Test GCS Integration
```bash
curl -X POST "https://your-service-url/api/bookings/report/download?month=2024-03"
```

### 2. Test Redis Session Management
```bash
# Create booking with session
curl -X POST "https://your-service-url/api/bookings/create" \
    -H "X-Session-Id: test-session-123" \
    -d "guestName=John&roomType=SUITE&checkIn=2024-03-01&checkOut=2024-03-05"

# Retrieve session data
curl "https://your-service-url/api/bookings/status/BK-12345" \
    -H "X-Session-Id: test-session-123"
```

### 3. Test Secret Manager Integration
```bash
# Verify secrets are loaded
curl "https://your-service-url/actuator/health"
```

## Monitoring

### Cloud Logging
All application logs are automatically sent to Cloud Logging when deployed on GCP.

### Cloud Monitoring
Set up monitoring for:
- Redis connection health
- GCS operation latency
- Secret Manager access patterns
- Application error rates

## Security Considerations

1. **Never commit .env files** to version control
2. **Use Secret Manager** for all sensitive data
3. **Enable VPC Service Controls** for enhanced security
4. **Use Private IP** for Memorystore for Redis
5. **Enable Cloud Armor** for DDoS protection
6. **Use HTTPS** for all external endpoints

## Rollback Plan

If issues occur after deployment:
1. Revert to previous version using Cloud Run revisions
2. Check Cloud Logging for error messages
3. Verify environment variables are correctly set
4. Verify GCP resources (GCS, Redis, Secret Manager) are accessible

## Support

For issues or questions:
1. Check Cloud Logging for error messages
2. Verify all environment variables are set correctly
3. Verify IAM permissions are granted
4. Check GCP resource status (GCS bucket, Redis instance, secrets)
