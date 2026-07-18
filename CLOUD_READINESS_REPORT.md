# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System & Storage (Blockers 1-7)
**Issues Fixed:**
- Hard-coded file paths (`/var/legacy/reports/`, `C:\ResortBackups\`)
- Local file system write operations
- Java.io.File usage for data storage

**Solution:**
- Migrated to **Amazon S3** for all file storage operations
- Implemented AWS SDK for Java v2 for S3 operations
- Reports are now stored in S3 buckets with configurable prefixes
- All file paths are now S3 URIs (e.g., `s3://bucket-name/reports/file.csv`)

**Files Modified:**
- `ReportService.java` - Complete rewrite to use S3Client
- `pom.xml` - Added AWS SDK S3 dependency

### 2. Configuration Management (Blockers 8-11, 19)
**Issues Fixed:**
- Hard-coded database credentials in source code
- Hard-coded environment URLs
- Hard-coded ports
- Clock/time dependencies using java.util.Date

**Solution:**
- Implemented **AWS Secrets Manager** for database credentials
- Implemented **AWS Systems Manager Parameter Store** for configuration
- All configuration externalized to environment variables
- Migrated to java.time API with UTC standardization

**Files Modified:**
- `BookingService.java` - Integrated Secrets Manager for credentials
- `ReportService.java` - Integrated Parameter Store for URLs
- `application.properties` - Externalized all configuration
- `AwsConfig.java` - Created centralized AWS SDK configuration

### 3. Networking & Communication (Blocker 12)
**Issues Fixed:**
- Hard-coded port numbers preventing dynamic assignment

**Solution:**
- Server port now configurable via `SERVER_PORT` environment variable
- Default port 8080 with override capability
- Compatible with ECS/EKS dynamic port assignment

**Files Modified:**
- `application.properties` - Added `server.port=${SERVER_PORT:8080}`
- `ReportService.java` - Removed hard-coded port references

### 4. State Management & Session (Blockers 13-17, 20)
**Issues Fixed:**
- HTTP session state storage preventing horizontal scaling
- In-memory caching without TTL causing memory issues

**Solution:**
- Implemented **Amazon ElastiCache for Redis** for distributed sessions
- Integrated **Spring Session** for Redis-backed session management
- Replaced in-memory cache with Redis cache with TTL policies
- All session data now shared across instances

**Files Modified:**
- `BookingController.java` - Integrated Redis for caching and sessions
- `RedisConfig.java` - Created Redis configuration
- `pom.xml` - Added Spring Session and Redis dependencies
- `application.properties` - Added Redis configuration

### 5. Security & Authentication (Blocker 18)
**Issues Fixed:**
- File-based authentication not scalable in cloud

**Solution:**
- Implemented **AWS Secrets Manager** for credential storage
- Added **Amazon Cognito-ready** authentication pattern
- Replaced MD5 with SHA-256 for secure hashing
- Centralized user credential management

**Files Modified:**
- `BookingService.java` - Added authenticateUser() method with Secrets Manager

## AWS Services Integrated

### 1. Amazon S3
- **Purpose:** Durable, scalable object storage for reports and files
- **Configuration:** `aws.s3.bucket`, `aws.s3.reports.prefix`
- **Usage:** All file operations now use S3Client

### 2. AWS Secrets Manager
- **Purpose:** Secure credential storage with automatic rotation
- **Configuration:** `aws.secrets.db.secret-name`, `aws.secrets.enabled`
- **Usage:** Database credentials and user authentication

### 3. AWS Systems Manager Parameter Store
- **Purpose:** Centralized configuration management
- **Configuration:** `aws.ssm.enabled`
- **Usage:** Environment-specific URLs and settings

### 4. Amazon ElastiCache for Redis
- **Purpose:** Distributed caching and session management
- **Configuration:** `spring.redis.host`, `spring.redis.port`
- **Usage:** HTTP sessions and application cache with TTL

## Configuration

### Environment Variables
```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration
DB_URL=jdbc:postgresql://db-host:5432/resorts
DB_USERNAME=app_user
DB_PASSWORD=secure_password
DB_DRIVER=org.postgresql.Driver

# Redis Configuration (ElastiCache)
REDIS_HOST=redis-cluster.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=redis_password
REDIS_SSL=true

# AWS Configuration
AWS_REGION=us-east-1
AWS_S3_BUCKET=resorts-reports-bucket
AWS_S3_REPORTS_PREFIX=reports/

# AWS Secrets Manager
AWS_DB_SECRET_NAME=resorts/db/credentials
AWS_SECRETS_ENABLED=true

# AWS Systems Manager
AWS_SSM_ENABLED=true

# External Services
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send

# Cache Configuration
CACHE_TTL_SECONDS=3600
CACHE_MAX_ENTRIES=1000
```

### AWS Secrets Manager Secret Format
```json
{
  "host": "db-prod.resorts.com",
  "username": "app_user",
  "password": "secure_password",
  "database": "resorts_db"
}
```

## Deployment Readiness

### AWS ECS/Fargate
- ✅ No hard-coded file paths
- ✅ Dynamic port assignment supported
- ✅ Stateless application design
- ✅ Environment variable configuration
- ✅ External session storage (Redis)

### AWS EKS (Kubernetes)
- ✅ 12-factor app compliant
- ✅ Horizontal scaling ready
- ✅ ConfigMaps/Secrets compatible
- ✅ Health check endpoints available

### AWS Elastic Beanstalk
- ✅ Standard Spring Boot application
- ✅ Environment properties supported
- ✅ Auto-scaling compatible

## Dependencies Added

### AWS SDK v2
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.26</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.20.26</version>
</dependency>
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>ssm</artifactId>
    <version>2.20.26</version>
</dependency>
```

### Spring Session & Redis
```xml
<dependency>
    <groupId>org.springframework.session</groupId>
    <artifactId>spring-session-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## Testing

### Local Development
1. Start Redis locally: `docker run -p 6379:6379 redis:latest`
2. Set environment variables or use defaults
3. Run application: `mvn spring-boot:run`

### AWS Deployment
1. Create S3 bucket for reports
2. Create ElastiCache Redis cluster
3. Store database credentials in Secrets Manager
4. Configure environment variables in ECS/EKS
5. Deploy application

## Security Improvements
- ✅ No credentials in source code
- ✅ Secrets stored in AWS Secrets Manager
- ✅ SHA-256 hashing instead of MD5
- ✅ Parameterized SQL queries (SQL injection prevention)
- ✅ HTTPS-ready endpoints

## Scalability Improvements
- ✅ Stateless application design
- ✅ Distributed session management
- ✅ Centralized caching with TTL
- ✅ No local file system dependencies
- ✅ Horizontal scaling ready

## Monitoring & Observability
- ✅ UTC timestamps for consistent logging
- ✅ Cloud-native logging patterns
- ✅ Ready for CloudWatch integration
- ✅ Structured logging support

## Next Steps
1. Configure AWS resources (S3, ElastiCache, Secrets Manager)
2. Set up IAM roles with appropriate permissions
3. Deploy to AWS ECS/EKS/Elastic Beanstalk
4. Configure CloudWatch for monitoring
5. Set up AWS X-Ray for distributed tracing (optional)

## Support
For issues or questions, refer to AWS documentation:
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [Amazon S3](https://docs.aws.amazon.com/s3/)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Amazon ElastiCache](https://docs.aws.amazon.com/elasticache/)
