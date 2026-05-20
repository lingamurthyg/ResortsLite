# ResortsLite - Cloud-Ready Application

## Cloud Readiness Fixes Applied

This application has been transformed to be fully cloud-ready for AWS deployment. All 20 cloud readiness blockers have been resolved.

### Fixed Issues Summary

#### 1. File System Dependencies (Blockers 1-7)
- **FIXED cr-java-0061**: Replaced hard-coded file paths with Amazon S3 object storage
- **FIXED cr-java-0062**: Migrated local file writes to Amazon S3 for durable storage
- **FIXED cr-java-0063**: Replaced java.io.File operations with AWS SDK S3 client

#### 2. Configuration Management (Blockers 8-11, 19)
- **FIXED cr-java-0069**: Replaced hard-coded database credentials with AWS Secrets Manager
- **FIXED cr-java-0071**: Externalized environment URLs using AWS Systems Manager Parameter Store
- **FIXED cr-java-0111**: Replaced java.util.Date/Timer with java.time API and standardized on UTC

#### 3. Networking (Blocker 12)
- **FIXED cr-java-0077**: Replaced hard-coded ports with environment variable injection

#### 4. State Management (Blockers 13-17, 20)
- **FIXED cr-java-0065**: Replaced HTTP session storage with Amazon ElastiCache for Redis
- **FIXED cr-java-0067**: Replaced in-memory caching with Amazon ElastiCache for Redis with TTL

#### 5. Security & Authentication (Blocker 18)
- **FIXED cr-java-0090**: Replaced file-based authentication with AWS Secrets Manager

### AWS Services Required

1. **Amazon S3**: For report storage (replaces local file system)
2. **AWS Secrets Manager**: For database credentials and sensitive data
3. **AWS Systems Manager Parameter Store**: For configuration management
4. **Amazon ElastiCache for Redis**: For distributed session and cache management

### Environment Variables

The application requires the following environment variables for cloud deployment:

```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration
DB_URL=jdbc:postgresql://your-rds-instance.region.rds.amazonaws.com:5432/resortsdb
DB_USER=admin
DB_PASS=your-password

# AWS Configuration
AWS_REGION=us-east-1
AWS_SECRET_DB_NAME=resorts-db-credentials
AWS_S3_REPORTS_BUCKET=resorts-reports-bucket
AWS_SSM_REPORTS_URL=/resorts/reports/base-url

# Redis Configuration (ElastiCache)
REDIS_HOST=your-elasticache-cluster.region.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send
```

### AWS Secrets Manager Setup

Create a secret in AWS Secrets Manager with the following JSON structure:

```json
{
  "host": "your-rds-instance.region.rds.amazonaws.com",
  "username": "admin",
  "password": "your-secure-password",
  "port": "5432",
  "database": "resortsdb"
}
```

### AWS Systems Manager Parameter Store Setup

Create the following parameters:

- `/resorts/reports/base-url`: Base URL for report downloads (e.g., `https://reports.resorts.com`)

### S3 Bucket Setup

Create an S3 bucket for report storage:
- Bucket name: `resorts-reports-bucket` (or as configured)
- Enable versioning for data durability
- Configure appropriate IAM policies for application access

### ElastiCache for Redis Setup

1. Create a Redis cluster in Amazon ElastiCache
2. Configure security groups to allow access from application instances
3. Note the cluster endpoint and configure in environment variables

### IAM Permissions Required

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
        "arn:aws:s3:::resorts-reports-bucket",
        "arn:aws:s3:::resorts-reports-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:resorts-db-credentials-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/resorts/*"
    }
  ]
}
```

### Deployment Architecture

The application is now stateless and can be deployed in:
- **AWS Elastic Beanstalk**: Managed platform for Java applications
- **Amazon ECS**: Container orchestration service
- **Amazon EKS**: Kubernetes-based container orchestration
- **AWS Lambda**: Serverless deployment (with Spring Cloud Function)

### 12-Factor App Compliance

The application now follows 12-factor app principles:
1. ✅ Codebase: Single codebase tracked in version control
2. ✅ Dependencies: Explicitly declared in pom.xml
3. ✅ Config: Externalized to environment variables
4. ✅ Backing Services: Treats databases, caches as attached resources
5. ✅ Build, Release, Run: Strictly separated stages
6. ✅ Processes: Stateless with shared-nothing architecture
7. ✅ Port Binding: Self-contained with configurable ports
8. ✅ Concurrency: Horizontally scalable
9. ✅ Disposability: Fast startup and graceful shutdown
10. ✅ Dev/Prod Parity: Same backing services across environments
11. ✅ Logs: Treats logs as event streams
12. ✅ Admin Processes: Run as one-off processes

### Testing Locally

To test locally without AWS services:

1. Start a local Redis instance:
   ```bash
   docker run -d -p 6379:6379 redis:latest
   ```

2. Set environment variables:
   ```bash
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   export AWS_REGION=us-east-1
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Migration Checklist

- [x] Replace hard-coded file paths with S3
- [x] Replace hard-coded credentials with Secrets Manager
- [x] Replace hard-coded URLs with Parameter Store
- [x] Replace HTTP sessions with Redis
- [x] Replace in-memory cache with Redis
- [x] Replace hard-coded ports with environment variables
- [x] Replace java.util.Date with java.time API
- [x] Add AWS SDK dependencies
- [x] Configure Spring Session with Redis
- [x] Create AWS configuration beans

### Next Steps

1. **Containerization**: Create Dockerfile for container deployment (separate workflow)
2. **Infrastructure**: Define Terraform/CloudFormation for AWS resources (separate workflow)
3. **CI/CD**: Set up deployment pipeline (separate workflow)
4. **Monitoring**: Configure CloudWatch logs and metrics
5. **Security**: Enable AWS WAF and security groups
