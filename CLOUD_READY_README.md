# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready and compatible with AWS cloud environments. All cloud readiness blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System Dependencies (Blockers 1-7)
**Issue**: Hard-coded file paths and local file system operations
**Fix**: Migrated to Amazon S3 for all file storage operations
- Replaced `/var/legacy/reports/` with S3 bucket storage
- Replaced `C:\\ResortBackups\\nightly\\` with S3
- All file operations now use AWS SDK for Java v2 S3 client
- Reports are generated and stored in S3 with proper key structure

### 2. Hard-coded Database Credentials (Blockers 8-9)
**Issue**: Database credentials embedded in source code
**Fix**: Migrated to AWS Secrets Manager
- Database credentials retrieved from AWS Secrets Manager at runtime
- Secrets stored in JSON format: `{"host": "...", "username": "...", "password": "..."}`
- Automatic credential rotation support
- Fallback to environment variables for local development

### 3. Hard-coded Environment URLs (Blockers 10-11)
**Issue**: Environment-specific URLs hard-coded in application
**Fix**: Externalized to AWS Systems Manager Parameter Store
- All service endpoints configurable via environment variables
- Support for AWS Parameter Store retrieval
- Environment-agnostic deployment capability

### 4. Hard-coded Ports (Blocker 12)
**Issue**: Fixed port numbers preventing dynamic assignment
**Fix**: Externalized port configuration
- Server port configurable via `SERVER_PORT` environment variable
- Compatible with ECS, EKS, and Elastic Beanstalk dynamic port assignment

### 5. HTTP Session State Storage (Blockers 13-17)
**Issue**: Session data stored in local memory, breaking horizontal scaling
**Fix**: Migrated to Amazon ElastiCache for Redis
- Spring Session with Redis backend
- Distributed session management across all instances
- Automatic session replication and failover
- Stateless application instances

### 6. File-based Authentication (Blocker 18)
**Issue**: Authentication credentials stored in local files
**Fix**: Migrated to AWS Secrets Manager and Amazon Cognito pattern
- User credentials stored in AWS Secrets Manager
- Per-user secret retrieval: `resorts/users/{username}`
- Centralized, encrypted, and auditable authentication

### 7. Clock/Time Dependencies (Blocker 19)
**Issue**: Using java.util.Date and local timezone dependencies
**Fix**: Migrated to java.time API with UTC standardization
- All timestamps use `java.time.Instant` and `ZonedDateTime`
- Standardized on UTC timezone across all services
- ISO-8601 formatted timestamps for consistency

### 8. In-Memory Caching Without TTL (Blocker 20)
**Issue**: Unbounded in-memory cache causing memory growth
**Fix**: Migrated to Amazon ElastiCache for Redis with TTL
- Distributed Redis cache with configurable TTL
- Consistent cache across all instances
- Automatic expiration and memory management

## AWS Services Used

### Amazon S3
- **Purpose**: Durable file storage for reports and documents
- **Configuration**: `aws.s3.bucket.name` and `aws.s3.region`
- **Benefits**: Scalable, durable, and highly available storage

### AWS Secrets Manager
- **Purpose**: Secure credential storage and rotation
- **Configuration**: `aws.secrets.db.secret.name`
- **Benefits**: Encrypted storage, automatic rotation, audit logging

### AWS Systems Manager Parameter Store
- **Purpose**: Centralized configuration management
- **Configuration**: Parameters stored under `/resorts/config/`
- **Benefits**: Version control, change tracking, hierarchical organization

### Amazon ElastiCache for Redis
- **Purpose**: Distributed session management and caching
- **Configuration**: `spring.redis.host`, `spring.redis.port`
- **Benefits**: High performance, automatic failover, data persistence

## Environment Variables

### Required for Production
```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration (or use Secrets Manager)
DB_URL=jdbc:postgresql://db-host:5432/resorts
DB_USERNAME=app_user
DB_PASSWORD=secure_password

# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=resorts-reports-bucket
DB_SECRET_NAME=resorts/db/credentials

# Redis Configuration
REDIS_HOST=resorts-cache.abc123.ng.0001.use1.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=optional_password

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send

# Cache Configuration
CACHE_TTL=3600
```

### Optional for Development
```bash
H2_CONSOLE_ENABLED=true
```

## Deployment Considerations

### AWS ECS/Fargate
- Application is now stateless and can scale horizontally
- Session data persists across container restarts
- Use task IAM roles for AWS service access
- Configure environment variables via task definition

### AWS EKS (Kubernetes)
- Deploy as stateless pods with multiple replicas
- Use ConfigMaps for non-sensitive configuration
- Use Secrets for sensitive data
- Configure Redis as a separate service or use ElastiCache

### AWS Elastic Beanstalk
- Application supports dynamic port binding
- Configure environment variables in EB console
- Use IAM instance profile for AWS service access

## Security Improvements

1. **No Hard-coded Credentials**: All credentials externalized
2. **Secrets Rotation**: AWS Secrets Manager supports automatic rotation
3. **Encrypted Storage**: Secrets encrypted at rest and in transit
4. **Audit Logging**: All secret access logged to CloudTrail
5. **SHA-256 Hashing**: Replaced MD5 with secure SHA-256
6. **Parameterized Queries**: SQL injection prevention

## 12-Factor App Compliance

✅ **I. Codebase**: Single codebase tracked in version control
✅ **II. Dependencies**: Explicitly declared in pom.xml
✅ **III. Config**: Externalized to environment variables
✅ **IV. Backing Services**: Attached resources (Redis, S3, Secrets Manager)
✅ **V. Build, Release, Run**: Strict separation maintained
✅ **VI. Processes**: Stateless processes with Redis-backed sessions
✅ **VII. Port Binding**: Dynamic port binding support
✅ **VIII. Concurrency**: Horizontal scaling enabled
✅ **IX. Disposability**: Fast startup and graceful shutdown
✅ **X. Dev/Prod Parity**: Same backing services across environments
✅ **XI. Logs**: Structured logging to stdout
✅ **XII. Admin Processes**: Separate admin tasks support

## Testing Locally

### Prerequisites
- Docker and Docker Compose
- AWS CLI configured with credentials
- Java 8 or higher

### Start Redis Locally
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

### Set Environment Variables
```bash
export SERVER_PORT=8080
export REDIS_HOST=localhost
export REDIS_PORT=6379
export S3_BUCKET_NAME=local-test-bucket
export AWS_REGION=us-east-1
```

### Run Application
```bash
mvn spring-boot:run
```

## Migration Checklist

- [x] Replace hard-coded file paths with S3
- [x] Migrate database credentials to Secrets Manager
- [x] Externalize environment URLs to Parameter Store
- [x] Configure dynamic port binding
- [x] Implement Redis-backed session management
- [x] Add Redis distributed caching with TTL
- [x] Migrate authentication to Secrets Manager
- [x] Replace java.util.Date with java.time API
- [x] Update all timestamps to UTC
- [x] Add AWS SDK dependencies
- [x] Add Spring Session Redis dependencies
- [x] Create AWS configuration beans
- [x] Create Redis configuration beans
- [x] Update application.properties with environment variables
- [x] Remove all hard-coded credentials
- [x] Remove all hard-coded file paths
- [x] Remove all hard-coded URLs

## Support

For issues or questions about the cloud-ready implementation, please refer to:
- AWS SDK for Java v2 Documentation
- Spring Session Documentation
- Spring Data Redis Documentation
- AWS Secrets Manager Best Practices
- AWS Systems Manager Parameter Store Guide
