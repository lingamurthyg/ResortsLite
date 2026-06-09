# ResortsLite - Cloud-Ready Application

## Overview
This application has been transformed to be fully cloud-ready and compatible with AWS cloud environments. All cloud compatibility blockers have been resolved.

## Cloud-Native Features Implemented

### 1. Amazon S3 Integration (File Storage)
**Blockers Fixed:** cr-java-0061, cr-java-0062, cr-java-0063

- **Before:** Hard-coded file paths (`/var/legacy/reports/`, `C:\\ResortBackups\\`)
- **After:** Amazon S3 object storage with configurable bucket and prefix
- **Configuration:**
  - `aws.s3.bucket` - S3 bucket name for storage
  - `aws.s3.reports.prefix` - Prefix for report objects

### 2. AWS Secrets Manager (Credential Management)
**Blockers Fixed:** cr-java-0069, cr-java-0090

- **Before:** Hard-coded database credentials in source code
- **After:** Credentials retrieved from AWS Secrets Manager
- **Configuration:**
  - `aws.secrets.db-credentials` - Secret name for database credentials
  - Supports automatic credential rotation

### 3. AWS Systems Manager Parameter Store (Configuration Management)
**Blockers Fixed:** cr-java-0071

- **Before:** Hard-coded environment URLs and endpoints
- **After:** Configuration retrieved from Parameter Store
- **Configuration:**
  - `aws.ssm.parameter.prefix` - Prefix for parameter paths
  - Supports dynamic configuration updates

### 4. Amazon ElastiCache for Redis (Session Management)
**Blockers Fixed:** cr-java-0065, cr-java-0067

- **Before:** HTTP session storage (instance-local, non-scalable)
- **After:** Distributed Redis session storage with TTL
- **Configuration:**
  - `spring.redis.host` - Redis host (ElastiCache endpoint)
  - `spring.redis.port` - Redis port
  - `spring.cache.redis.time-to-live` - Cache TTL in milliseconds

### 5. Dynamic Port Configuration
**Blockers Fixed:** cr-java-0077

- **Before:** Hard-coded port 8080
- **After:** Environment variable `SERVER_PORT` with fallback
- **Configuration:**
  - `server.port=${SERVER_PORT:8080}`

### 6. UTC Timezone Standardization
**Blockers Fixed:** cr-java-0111

- **Before:** java.util.Date with local timezone
- **After:** java.time API with UTC timezone
- **Configuration:**
  - `spring.jackson.time-zone=UTC`

## Environment Variables

### Required for AWS Deployment

```bash
# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=resorts-lite-storage
DB_CREDENTIALS_SECRET_NAME=resorts/db/credentials

# Redis Configuration (ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Database Configuration
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/resortsdb
DB_USERNAME=admin
DB_PASSWORD=your-db-password

# Server Configuration
SERVER_PORT=8080

# External Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify-svc.internal:7070/send
```

### Optional Configuration

```bash
# Cache TTL (milliseconds)
CACHE_TTL=3600000

# Session timeout (seconds)
SESSION_TIMEOUT=1800

# Database connection pool
DB_POOL_SIZE=10
DB_POOL_MIN_IDLE=2
```

## AWS Services Required

1. **Amazon S3** - Object storage for reports and files
2. **AWS Secrets Manager** - Secure credential storage
3. **AWS Systems Manager Parameter Store** - Configuration management
4. **Amazon ElastiCache for Redis** - Distributed session and cache storage
5. **Amazon RDS** (optional) - Managed database service

## Security Improvements

1. **SQL Injection Prevention:** All queries use parameterized statements
2. **Secure Hashing:** Replaced MD5 with SHA-256
3. **Credential Externalization:** No credentials in source code
4. **HTTPS Support:** Endpoints configured for HTTPS

## Deployment Considerations

### AWS ECS/EKS Deployment
- Application is stateless and horizontally scalable
- Uses distributed session management (Redis)
- All configuration externalized to environment variables
- No local file system dependencies

### AWS Elastic Beanstalk Deployment
- Configure environment variables in Beanstalk console
- Set up ElastiCache Redis cluster
- Configure S3 bucket permissions
- Set up Secrets Manager secrets

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
        "arn:aws:s3:::resorts-lite-storage/*",
        "arn:aws:s3:::resorts-lite-storage"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:resorts/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/resorts-lite/*"
    }
  ]
}
```

## Testing

### Local Development
For local development, you can use:
- LocalStack for AWS services simulation
- Redis Docker container for session management
- H2 in-memory database

### Environment Variables for Local Testing
```bash
AWS_REGION=us-east-1
S3_BUCKET_NAME=local-test-bucket
REDIS_HOST=localhost
REDIS_PORT=6379
DB_URL=jdbc:h2:mem:resortdb
DB_USERNAME=sa
DB_PASSWORD=
```

## Migration Notes

### Breaking Changes
1. HTTP session attributes are no longer supported - use Redis-backed sessions
2. Local file paths are no longer supported - use S3 object keys
3. Hard-coded credentials are no longer supported - use Secrets Manager

### Backward Compatibility
- Application maintains same REST API endpoints
- Business logic unchanged
- Database schema unchanged

## Monitoring and Observability

### CloudWatch Integration
- Application logs are JSON-formatted for CloudWatch Logs
- Metrics can be published to CloudWatch Metrics
- X-Ray tracing can be added for distributed tracing

### Health Checks
- Spring Boot Actuator endpoints available
- `/actuator/health` - Application health status
- `/actuator/info` - Application information

## Support

For issues or questions regarding cloud deployment, refer to:
- AWS Documentation: https://docs.aws.amazon.com/
- Spring Cloud AWS: https://spring.io/projects/spring-cloud-aws
- AWS SDK for Java v2: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/
