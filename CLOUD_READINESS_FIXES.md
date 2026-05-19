# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System & Storage (Critical)
**Issues Fixed:**
- ❌ Hard-coded file paths (`/var/legacy/reports/`, `C:\\ResortBackups\\`)
- ❌ Local file system write operations
- ❌ java.io.File usage for data storage

**Solutions Implemented:**
- ✅ Replaced all file operations with **Amazon S3** object storage
- ✅ Reports are now stored in S3 bucket (configurable via `aws.s3.bucket`)
- ✅ No local file system dependencies
- ✅ Durable, scalable storage across all instances

**Files Modified:**
- `ReportService.java` - Complete S3 integration with AWS SDK v2

### 2. Configuration Management (Critical)
**Issues Fixed:**
- ❌ Hard-coded database credentials in source code
- ❌ Hard-coded environment URLs
- ❌ Hard-coded ports

**Solutions Implemented:**
- ✅ Database credentials retrieved from **AWS Secrets Manager**
- ✅ Service URLs externalized to **AWS Systems Manager Parameter Store**
- ✅ All ports configurable via environment variables
- ✅ Zero hard-coded configuration values

**Files Modified:**
- `BookingService.java` - Secrets Manager integration
- `ReportService.java` - Parameter Store integration
- `application.properties` - All values externalized

### 3. Session Management (High)
**Issues Fixed:**
- ❌ HTTP session state storage (breaks horizontal scaling)
- ❌ Session data lost on instance termination
- ❌ No session sharing across instances

**Solutions Implemented:**
- ✅ Distributed session management with **Amazon ElastiCache for Redis**
- ✅ Spring Session integration for automatic Redis-backed sessions
- ✅ Sessions persist across instance restarts and scale events
- ✅ Full horizontal scalability

**Files Modified:**
- `BookingController.java` - Redis session integration
- `RedisConfig.java` - Redis configuration for ElastiCache

### 4. Caching (Medium)
**Issues Fixed:**
- ❌ In-memory caching without TTL
- ❌ Cache not shared across instances
- ❌ Memory growth and stale data issues

**Solutions Implemented:**
- ✅ Distributed caching with **Amazon ElastiCache for Redis**
- ✅ Configurable TTL policies (`spring.cache.redis.time-to-live`)
- ✅ Cache shared across all instances
- ✅ Automatic expiration and memory management

**Files Modified:**
- `BookingController.java` - Redis cache integration
- `RedisConfig.java` - Redis template configuration

### 5. Security & Authentication (High)
**Issues Fixed:**
- ❌ File-based authentication
- ❌ Credentials in source code

**Solutions Implemented:**
- ✅ AWS Secrets Manager for credential storage
- ✅ IAM role-based authentication for AWS services
- ✅ No credentials in source code or configuration files
- ✅ Support for automatic credential rotation

**Files Modified:**
- `BookingService.java` - Secrets Manager integration
- `AwsConfig.java` - AWS SDK configuration

### 6. Time & Clock Dependencies (High)
**Issues Fixed:**
- ❌ java.util.Date and java.util.Timer usage
- ❌ Timezone inconsistencies

**Solutions Implemented:**
- ✅ Migrated to **java.time API** (Instant, ZonedDateTime)
- ✅ All timestamps in **UTC** for consistency
- ✅ ISO-8601 formatted timestamps

**Files Modified:**
- `ReportService.java` - java.time API usage

## AWS Services Integration

### Required AWS Services
1. **Amazon S3** - Object storage for reports and files
2. **Amazon ElastiCache for Redis** - Distributed session and cache management
3. **AWS Secrets Manager** - Secure credential storage
4. **AWS Systems Manager Parameter Store** - Configuration management

### IAM Permissions Required
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::${S3_BUCKET_NAME}",
        "arn:aws:s3:::${S3_BUCKET_NAME}/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:${REGION}:${ACCOUNT_ID}:secret:${SECRET_NAME}"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:${REGION}:${ACCOUNT_ID}:parameter/${PARAMETER_PREFIX}/*"
    }
  ]
}
```

## Environment Variables

### Required Configuration
```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/resortdb
DB_USERNAME=admin
DB_PASSWORD=your-password

# AWS Configuration
AWS_REGION=us-east-1
AWS_S3_BUCKET=resorts-lite-reports
AWS_SECRET_DB_CREDENTIALS=resorts/db/credentials
AWS_SSM_PARAMETER_PREFIX=/resortslite

# Redis Configuration (ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_SSL=true

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc:8081/rooms
NOTIFICATION_ENDPOINT=https://notify-svc:7070/send

# Cache Configuration
CACHE_TTL=3600000
```

## Deployment Architecture

### Recommended AWS Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                     Application Load Balancer               │
│                         (HTTPS/TLS)                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              ECS/EKS Cluster (Auto Scaling)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │Instance 1│  │Instance 2│  │Instance 3│  │Instance N│  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
         │              │              │              │
         └──────────────┴──────────────┴──────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  ElastiCache    │  │  Secrets        │  │  Parameter      │
│  for Redis      │  │  Manager        │  │  Store          │
│  (Sessions)     │  │  (Credentials)  │  │  (Config)       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │
         ▼
┌─────────────────┐
│   Amazon S3     │
│   (Reports)     │
└─────────────────┘
```

## Dependencies Added

### AWS SDK v2
- `software.amazon.awssdk:s3` - S3 client
- `software.amazon.awssdk:secretsmanager` - Secrets Manager client
- `software.amazon.awssdk:ssm` - Systems Manager client

### Spring Session & Redis
- `spring-session-data-redis` - Distributed session management
- `spring-boot-starter-data-redis` - Redis integration
- `lettuce-core` - Redis client

## Testing

### Local Development
For local development, you can use:
- LocalStack for AWS services simulation
- Redis Docker container for session/cache testing

```bash
# Start Redis locally
docker run -d -p 6379:6379 redis:latest

# Set environment variables for local testing
export AWS_REGION=us-east-1
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

### Cloud Deployment
1. Create required AWS resources (S3 bucket, ElastiCache cluster, Secrets)
2. Configure IAM roles with required permissions
3. Set environment variables in ECS task definition or EKS deployment
4. Deploy application

## Migration Checklist

- [x] Replace hard-coded file paths with S3
- [x] Replace local file writes with S3 uploads
- [x] Replace java.io.File with S3 client
- [x] Externalize database credentials to Secrets Manager
- [x] Externalize service URLs to Parameter Store
- [x] Replace hard-coded ports with environment variables
- [x] Replace HTTP sessions with Redis-backed sessions
- [x] Replace in-memory cache with Redis cache with TTL
- [x] Replace file-based auth with Secrets Manager
- [x] Replace java.util.Date with java.time API (UTC)
- [x] Add AWS SDK dependencies
- [x] Add Spring Session Redis dependencies
- [x] Configure Redis connection factory
- [x] Configure AWS clients with IAM roles

## Compliance

### 12-Factor App Principles
- ✅ **I. Codebase** - Single codebase tracked in version control
- ✅ **II. Dependencies** - Explicitly declared in pom.xml
- ✅ **III. Config** - Externalized to environment variables
- ✅ **IV. Backing Services** - S3, Redis, Secrets Manager as attached resources
- ✅ **V. Build, Release, Run** - Strict separation maintained
- ✅ **VI. Processes** - Stateless processes with Redis for shared state
- ✅ **VII. Port Binding** - Configurable port binding
- ✅ **VIII. Concurrency** - Horizontal scaling enabled
- ✅ **IX. Disposability** - Fast startup, graceful shutdown
- ✅ **X. Dev/Prod Parity** - Same backing services in all environments
- ✅ **XI. Logs** - Structured logging to stdout
- ✅ **XII. Admin Processes** - One-off admin tasks supported

## Support

For issues or questions about the cloud readiness transformation, refer to:
- AWS SDK for Java v2 documentation
- Spring Session documentation
- Spring Boot on AWS best practices
