# Cloud Deployment Environment Variables

This document describes the environment variables required for cloud deployment of the ResortsLite application.

## AWS Configuration

### Required Environment Variables

```bash
# AWS Region
AWS_REGION=us-east-1

# Server Configuration
SERVER_PORT=8080

# Redis Configuration (Amazon ElastiCache)
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_SSL=true
SESSION_TIMEOUT=30m

# S3 Configuration
REPORTS_BUCKET=resort-reports-bucket

# AWS Secrets Manager
DB_SECRET_NAME=resorts/database/credentials
AUTH_SECRET_NAME=resorts/auth/credentials

# Service Endpoints (from Parameter Store)
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send
REPORTS_BASE_URL=https://reports.resorts-internal.com

# Cache Configuration
BOOKING_CACHE_TTL=30

# Logging
LOG_LEVEL=INFO
APP_LOG_LEVEL=DEBUG
```

## AWS Secrets Manager Secret Format

### Database Credentials Secret (resorts/database/credentials)

```json
{
  "host": "your-rds-endpoint.rds.amazonaws.com",
  "username": "admin",
  "password": "your-secure-password",
  "database": "resortdb",
  "port": "3306"
}
```

### Authentication Credentials Secret (resorts/auth/credentials)

```json
{
  "username": "admin",
  "passwordHash": "sha256-hash-of-password"
}
```

## AWS IAM Permissions Required

The application requires the following IAM permissions:

### S3 Permissions
- `s3:PutObject`
- `s3:GetObject`
- `s3:ListBucket`

### Secrets Manager Permissions
- `secretsmanager:GetSecretValue`
- `secretsmanager:DescribeSecret`

### Systems Manager Parameter Store Permissions
- `ssm:GetParameter`
- `ssm:GetParameters`
- `ssm:GetParametersByPath`

### ElastiCache Permissions
- `elasticache:DescribeCacheClusters`
- `elasticache:DescribeReplicationGroups`

## Deployment Checklist

1. ✅ Create S3 bucket for reports storage
2. ✅ Create ElastiCache Redis cluster
3. ✅ Store database credentials in Secrets Manager
4. ✅ Store authentication credentials in Secrets Manager
5. ✅ Configure service endpoints in Parameter Store
6. ✅ Attach IAM role with required permissions to ECS task or EC2 instance
7. ✅ Set environment variables in ECS task definition or Elastic Beanstalk configuration
8. ✅ Configure security groups to allow Redis and RDS connectivity
9. ✅ Enable VPC endpoints for AWS services (optional, for enhanced security)

## Local Development

For local development, you can use the default values in `application.properties` or set environment variables:

```bash
export AWS_REGION=us-east-1
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REPORTS_BUCKET=local-reports-bucket
```

## Docker Environment Variables

When running in Docker, pass environment variables using `-e` flag or docker-compose:

```yaml
environment:
  - AWS_REGION=us-east-1
  - REDIS_HOST=redis
  - REDIS_PORT=6379
  - REPORTS_BUCKET=resort-reports-bucket
  - DB_SECRET_NAME=resorts/database/credentials
```

## Kubernetes ConfigMap and Secrets

For Kubernetes deployment, use ConfigMaps for non-sensitive data and Secrets for sensitive data:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: resorts-config
data:
  AWS_REGION: "us-east-1"
  REPORTS_BUCKET: "resort-reports-bucket"
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
```

## Troubleshooting

### Common Issues

1. **Cannot connect to Redis**: Verify security group rules allow traffic on port 6379
2. **Cannot access S3 bucket**: Verify IAM role has s3:PutObject and s3:GetObject permissions
3. **Cannot retrieve secrets**: Verify IAM role has secretsmanager:GetSecretValue permission
4. **Database connection fails**: Verify RDS security group allows traffic from application security group

### Health Check Endpoints

- Application health: `GET /actuator/health`
- Redis connectivity: Check application logs for Redis connection errors
- S3 connectivity: Check application logs for S3 client errors
