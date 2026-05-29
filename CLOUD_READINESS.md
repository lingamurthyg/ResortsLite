# ResortsLite - Cloud-Ready Application for AWS

## Cloud Readiness Fixes Applied

This application has been transformed to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

### Fixed Issues Summary

#### 1. File System Dependencies (cr-java-0061, cr-java-0062, cr-java-0063)
- **Issue**: Hard-coded file paths and local file system operations
- **Fix**: Migrated all file operations to Amazon S3 using AWS SDK for Java v2
- **Files Modified**: `ReportService.java`
- **Configuration**: S3 bucket name configurable via `aws.s3.reports.bucket` property

#### 2. Hard-coded Database Credentials (cr-java-0069)
- **Issue**: Database credentials embedded in source code
- **Fix**: Migrated to AWS Secrets Manager for centralized, encrypted credential storage
- **Files Modified**: `BookingService.java`
- **Configuration**: Secret name configurable via `aws.secretsmanager.database.secret.name` property

#### 3. Hard-coded Environment URLs (cr-java-0071)
- **Issue**: Environment-specific URLs hard-coded in application
- **Fix**: Externalized all URLs to AWS Systems Manager Parameter Store
- **Files Modified**: `BookingController.java`, `ReportService.java`, `BookingService.java`
- **Configuration**: All endpoints configurable via environment variables

#### 4. Hard-coded Ports (cr-java-0077)
- **Issue**: Fixed port numbers preventing dynamic assignment
- **Fix**: Port configuration externalized to environment variables
- **Files Modified**: `ReportService.java`, `application.properties`
- **Configuration**: Port configurable via `SERVER_PORT` environment variable

#### 5. HTTP Session State Storage (cr-java-0065)
- **Issue**: Session data stored in local memory, preventing horizontal scaling
- **Fix**: Migrated to Amazon ElastiCache for Redis with Spring Session
- **Files Modified**: `BookingController.java`
- **Configuration**: Redis connection details configurable via environment variables

#### 6. In-Memory Caching Without TTL (cr-java-0067)
- **Issue**: Unbounded in-memory cache causing memory issues
- **Fix**: Replaced with Amazon ElastiCache for Redis with proper TTL policies
- **Files Modified**: `BookingController.java`
- **Configuration**: Cache TTL configurable in `application.properties`

#### 7. File-based Authentication (cr-java-0090)
- **Issue**: Authentication credentials stored in local files
- **Fix**: Migrated to AWS Secrets Manager and Amazon Cognito integration ready
- **Files Modified**: `BookingService.java`

#### 8. Clock/Time Dependencies (cr-java-0111)
- **Issue**: Using java.util.Date with local timezone dependencies
- **Fix**: Migrated to java.time API with UTC standardization
- **Files Modified**: `ReportService.java`

## AWS Services Required

### 1. Amazon S3
- **Purpose**: Durable file storage for reports and documents
- **Configuration**: 
  - Bucket name: Set via `S3_REPORTS_BUCKET` environment variable
  - Default: `resort-reports-bucket`

### 2. AWS Secrets Manager
- **Purpose**: Secure storage for database credentials
- **Configuration**:
  - Secret name: Set via `DB_SECRET_NAME` environment variable
  - Default: `resorts-db-credentials`
  - Secret format (JSON):
    ```json
    {
      "host": "database-host.rds.amazonaws.com",
      "username": "admin",
      "password": "secure-password"
    }
    ```

### 3. Amazon ElastiCache for Redis
- **Purpose**: Distributed session management and caching
- **Configuration**:
  - Host: Set via `REDIS_HOST` environment variable
  - Port: Set via `REDIS_PORT` environment variable (default: 6379)
  - Password: Set via `REDIS_PASSWORD` environment variable
  - SSL: Set via `REDIS_SSL` environment variable (default: true)

### 4. AWS Systems Manager Parameter Store
- **Purpose**: Externalized configuration for environment-specific values
- **Parameters to configure**:
  - `/resortslite/payment/endpoint`
  - `/resortslite/inventory/endpoint`
  - `/resortslite/notification/endpoint`
  - `/resortslite/reports/download/url`

## Environment Variables

The following environment variables should be configured in your AWS deployment:

```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration (or use Secrets Manager)
DB_URL=jdbc:postgresql://database-host.rds.amazonaws.com:5432/resortdb
DB_USERNAME=admin
DB_PASSWORD=secure-password

# Redis Configuration (ElastiCache)
REDIS_HOST=redis-cluster.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=redis-password
REDIS_SSL=true

# AWS Configuration
AWS_REGION=us-east-1
S3_REPORTS_BUCKET=resort-reports-bucket
DB_SECRET_NAME=resorts-db-credentials

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms/available
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send
REPORTS_DOWNLOAD_URL=https://reports.resorts-internal.com/download
```

## IAM Permissions Required

The application requires the following IAM permissions:

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
        "arn:aws:s3:::resort-reports-bucket",
        "arn:aws:s3:::resort-reports-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:*:*:secret:resorts-db-credentials-*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/resortslite/*"
      ]
    }
  ]
}
```

## Deployment Options

### AWS Elastic Beanstalk
1. Package application: `mvn clean package`
2. Upload JAR to Elastic Beanstalk
3. Configure environment variables in EB console
4. Deploy

### Amazon ECS/Fargate
1. Build application: `mvn clean package`
2. Create container image (separate workflow)
3. Configure task definition with environment variables
4. Deploy to ECS cluster

### Amazon EKS
1. Build application: `mvn clean package`
2. Create container image (separate workflow)
3. Create Kubernetes deployment with ConfigMap/Secrets
4. Deploy to EKS cluster

## Testing Cloud Readiness

### Local Testing with LocalStack
You can test AWS integrations locally using LocalStack:

```bash
# Start LocalStack
docker run -d -p 4566:4566 localstack/localstack

# Configure AWS CLI for LocalStack
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1

# Create test resources
aws --endpoint-url=http://localhost:4566 s3 mb s3://resort-reports-bucket
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret \
  --name resorts-db-credentials \
  --secret-string '{"host":"localhost","username":"sa","password":""}'
```

## Migration Checklist

- [x] Replace hard-coded file paths with S3
- [x] Migrate database credentials to Secrets Manager
- [x] Externalize all environment-specific URLs
- [x] Configure dynamic port assignment
- [x] Implement distributed session management with Redis
- [x] Replace in-memory cache with Redis
- [x] Migrate authentication to cloud-native approach
- [x] Standardize time handling with UTC
- [x] Add AWS SDK dependencies
- [x] Create AWS configuration beans
- [x] Update application properties for cloud deployment

## Next Steps

1. **Set up AWS Resources**: Create S3 bucket, ElastiCache cluster, and Secrets Manager secrets
2. **Configure IAM Roles**: Attach required IAM policies to application execution role
3. **Deploy Application**: Choose deployment method (EB, ECS, or EKS)
4. **Test Endpoints**: Verify all API endpoints work correctly in cloud environment
5. **Monitor**: Set up CloudWatch logs and metrics for application monitoring

## Support

For issues or questions about cloud deployment, refer to AWS documentation:
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)
- [Amazon S3](https://docs.aws.amazon.com/s3/)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)
- [Amazon ElastiCache](https://docs.aws.amazon.com/elasticache/)
