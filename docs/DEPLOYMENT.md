# ResortsLite - AWS ECS Fargate Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS ECS Fargate Prerequisites](#aws-ecs-fargate-prerequisites)
6. [ECS Fargate Setup](#ecs-fargate-setup)
7. [ECS Task Definition Explained](#ecs-task-definition-explained)
8. [ECS Service Configuration](#ecs-service-configuration)
9. [Deployment Walkthrough](#deployment-walkthrough)
10. [Configuration Management](#configuration-management)
11. [Troubleshooting](#troubleshooting)
12. [Scaling and Management](#scaling-and-management)
13. [Security Considerations](#security-considerations)
14. [Technology-Specific Notes](#technology-specific-notes)

---

## Overview

ResortsLite is a Spring Boot 2.7.x application built with Java 8, designed for containerized deployment on AWS ECS Fargate. This guide provides comprehensive instructions for building, deploying, and managing the application in a production environment.

**Application Details:**
- **Framework**: Spring Boot 2.7.18
- **Java Version**: Java 8 (1.8)
- **Build Tool**: Maven
- **Application Port**: 8080
- **Health Endpoint**: `/actuator/health`
- **Management Endpoints**: `/actuator/health`, `/actuator/info`

**Key Features:**
- RESTful API for resort booking management
- Spring Boot Actuator for health monitoring
- Redis integration for distributed session management
- AWS S3 integration for file storage
- H2 in-memory database (configurable for external databases)

---

## Prerequisites

### Required Software
- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **AWS CLI**: Version 2.x
- **Git**: For version control
- **Java 8 JDK**: For local development (optional)
- **Maven 3.6+**: For local builds (optional)

### AWS Account Requirements
- Active AWS account with appropriate permissions
- IAM user with programmatic access
- AWS CLI configured with credentials

### System Requirements
- **OS**: Linux, macOS, or Windows 10/11
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: 10GB free space

---

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ResortsLitePPP
```

### 2. Review Application Configuration
The application uses environment variables for configuration. Review `src/main/resources/application.properties`:

```properties
server.port=${SERVER_PORT:8080}
spring.datasource.url=${DB_URL:jdbc:h2:mem:resortdb}
spring.redis.host=${REDIS_HOST:localhost}
aws.s3.bucket-name=${S3_BUCKET_NAME:resortslite-reports}
```

### 3. Run with Docker Compose (Local Development)
```bash
# Start the application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### 4. Access the Application
- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **H2 Console**: http://localhost:8080/h2-console

---

## Building and Pushing Docker Images

### Using build-push.sh (Linux/macOS)

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run the script
./scripts/build-push.sh
```

**Script Workflow:**
1. Prompts for image tag (default: `latest`)
2. Select registry type:
   - **Option 1**: AWS ECR (Elastic Container Registry)
   - **Option 2**: Docker Hub
3. Enter registry credentials and details
4. Builds Docker image using multi-stage Dockerfile
5. Authenticates with selected registry
6. Pushes image to registry

**AWS ECR Example:**
```
Enter image tag: v1.0.0
Select container registry: 1
Enter AWS Region: us-east-1
Enter ECR Repository Name: resortslite
Enter AWS Account ID: 123456789012
```

**Docker Hub Example:**
```
Enter image tag: v1.0.0
Select container registry: 2
Enter Docker Hub username: myusername
Enter Docker Hub password: ********
```

### Using build-push.bat (Windows)

```cmd
# Run the script
scripts\build-push.bat
```

Follow the same prompts as the Linux/macOS version.

### Manual Build (Alternative)

```bash
# Build the image
docker build -t resortslite:latest .

# Tag for ECR
docker tag resortslite:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest
```

---

## AWS ECS Fargate Prerequisites

### 1. AWS CLI Configuration

```bash
# Configure AWS CLI
aws configure

# Verify configuration
aws sts get-caller-identity
```

### 2. Required IAM Roles

#### ECS Task Execution Role
This role allows ECS to pull images from ECR and write logs to CloudWatch.

```bash
# Create trust policy file
cat > ecs-task-execution-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create the role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach AWS managed policy
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

#### ECS Task Role (Optional)
This role grants permissions to the application (e.g., S3 access, DynamoDB access).

```bash
# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file://ecs-task-execution-trust-policy.json

# Attach policies for S3 access
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

### 3. VPC and Networking Setup

#### Create VPC (if needed)
```bash
# Create VPC
VPC_ID=$(aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --query 'Vpc.VpcId' \
  --output text)

# Enable DNS hostnames
aws ec2 modify-vpc-attribute \
  --vpc-id $VPC_ID \
  --enable-dns-hostnames
```

#### Create Subnets
```bash
# Create public subnet 1
SUBNET_1=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --query 'Subnet.SubnetId' \
  --output text)

# Create public subnet 2
SUBNET_2=$(aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --query 'Subnet.SubnetId' \
  --output text)

# Create Internet Gateway
IGW_ID=$(aws ec2 create-internet-gateway \
  --query 'InternetGateway.InternetGatewayId' \
  --output text)

# Attach to VPC
aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID

# Create route table
ROUTE_TABLE_ID=$(aws ec2 create-route-table \
  --vpc-id $VPC_ID \
  --query 'RouteTable.RouteTableId' \
  --output text)

# Add route to Internet Gateway
aws ec2 create-route \
  --route-table-id $ROUTE_TABLE_ID \
  --destination-cidr-block 0.0.0.0/0 \
  --gateway-id $IGW_ID

# Associate subnets with route table
aws ec2 associate-route-table \
  --subnet-id $SUBNET_1 \
  --route-table-id $ROUTE_TABLE_ID

aws ec2 associate-route-table \
  --subnet-id $SUBNET_2 \
  --route-table-id $ROUTE_TABLE_ID
```

#### Create Security Group
```bash
# Create security group
SG_ID=$(aws ec2 create-security-group \
  --group-name resortslite-sg \
  --description "Security group for ResortsLite ECS tasks" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Allow inbound HTTP traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Allow inbound HTTP traffic on port 80 (for ALB)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0
```

### 4. CloudWatch Log Group

```bash
# Create log group
aws logs create-log-group --log-group-name /ecs/resortslite

# Set retention policy (optional)
aws logs put-retention-policy \
  --log-group-name /ecs/resortslite \
  --retention-in-days 7
```

---

## ECS Fargate Setup

### Understanding ECS Components

1. **Cluster**: Logical grouping of tasks or services
2. **Task Definition**: Blueprint for your application (like a Docker Compose file)
3. **Service**: Maintains desired number of tasks running
4. **Task**: Running instance of a task definition

### Create ECS Cluster

```bash
# Create cluster
aws ecs create-cluster --cluster-name resortslite-cluster

# Verify cluster
aws ecs describe-clusters --clusters resortslite-cluster
```

---

## ECS Task Definition Explained

The task definition (`ecs/task-definition.json`) defines how your container runs on Fargate.

### Key Configuration Elements

#### 1. Launch Type and Network Mode
```json
{
  "requiresCompatibilities": ["FARGATE"],
  "networkMode": "awsvpc"
}
```
- **FARGATE**: Serverless compute engine
- **awsvpc**: Each task gets its own ENI (Elastic Network Interface)

#### 2. CPU and Memory
```json
{
  "cpu": "512",
  "memory": "1024"
}
```

**Valid Fargate CPU/Memory Combinations:**
| CPU (vCPU) | Memory (MB) |
|------------|-------------|
| 256 (.25)  | 512, 1024, 2048 |
| 512 (.5)   | 1024, 2048, 3072, 4096 |
| 1024 (1)   | 2048-8192 (increments of 1024) |
| 2048 (2)   | 4096-16384 (increments of 1024) |
| 4096 (4)   | 8192-30720 (increments of 1024) |

**Recommendation for ResortsLite:**
- **Development**: cpu: "512", memory: "1024"
- **Production**: cpu: "1024", memory: "2048"

#### 3. IAM Roles
```json
{
  "executionRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::ACCOUNT_ID:role/ecsTaskRole"
}
```
- **executionRoleArn**: Allows ECS to pull images and write logs
- **taskRoleArn**: Grants permissions to the application (S3, DynamoDB, etc.)

#### 4. Container Definition
```json
{
  "containerDefinitions": [
    {
      "name": "resortslite",
      "image": "IMAGE_URI",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "SERVER_PORT", "value": "8080"},
        {"name": "SPRING_PROFILES_ACTIVE", "value": "docker"}
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/resortslite",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

#### 5. Environment Variables
Configure application behavior through environment variables:
- **SERVER_PORT**: Application port (8080)
- **SPRING_PROFILES_ACTIVE**: Spring profile (docker, prod)
- **DB_URL**: Database connection string
- **REDIS_HOST**: Redis server hostname
- **S3_BUCKET_NAME**: S3 bucket for file storage
- **AWS_REGION**: AWS region

---

## ECS Service Configuration

The service definition (`ecs/service-definition.json`) manages task deployment and scaling.

### Key Configuration Elements

#### 1. Service Basics
```json
{
  "serviceName": "resortslite-service",
  "cluster": "resortslite-cluster",
  "taskDefinition": "resortslite-task",
  "desiredCount": 2,
  "launchType": "FARGATE"
}
```

#### 2. Network Configuration
```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-xxx", "subnet-yyy"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "ENABLED"
    }
  }
}
```
- **subnets**: At least 2 subnets in different AZs for high availability
- **securityGroups**: Controls inbound/outbound traffic
- **assignPublicIp**: ENABLED for public internet access

#### 3. Deployment Configuration
```json
{
  "deploymentConfiguration": {
    "maximumPercent": 200,
    "minimumHealthyPercent": 50
  }
}
```
- **maximumPercent**: Maximum tasks during deployment (200% = 2x desired count)
- **minimumHealthyPercent**: Minimum healthy tasks during deployment (50%)

#### 4. Load Balancer Integration
```json
{
  "loadBalancers": [
    {
      "targetGroupArn": "arn:aws:elasticloadbalancing:...",
      "containerName": "resortslite",
      "containerPort": 8080
    }
  ],
  "healthCheckGracePeriodSeconds": 300
}
```

---

## Deployment Walkthrough

### Step-by-Step Deployment

#### Step 1: Build and Push Docker Image

```bash
# Run build script
./scripts/build-push.sh

# Follow prompts:
# - Enter image tag: v1.0.0
# - Select registry: 1 (AWS ECR)
# - Enter AWS Region: us-east-1
# - Enter ECR Repository: resortslite
# - Enter AWS Account ID: 123456789012
```

**Expected Output:**
```
Building Docker image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:v1.0.0
Pushing image to registry...
✓ SUCCESS!
```

#### Step 2: Deploy to ECS Fargate

```bash
# Run deployment script
./scripts/deploy-image.sh

# Follow prompts:
# - AWS Region: us-east-1
# - ECS Cluster Name: resortslite-cluster
# - VPC ID: vpc-xxx
# - Subnet IDs: subnet-xxx,subnet-yyy
# - Security Group ID: sg-xxx
# - Docker Image URI: 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:v1.0.0
# - Redis Host: redis.example.com
# - Redis Port: 6379
# - S3 Bucket Name: resortslite-reports
# - Need load balancer? y
```

**Expected Output:**
```
Checking ECS cluster...
Creating Application Load Balancer...
ALB DNS Name: resortslite-alb-123456789.us-east-1.elb.amazonaws.com
Registering task definition...
Task Definition ARN: arn:aws:ecs:us-east-1:123456789012:task-definition/resortslite-task:1
Creating new ECS service...
Waiting for service to become stable...
✓ Deployment Complete!
```

#### Step 3: Verify Deployment

```bash
# Check service status
aws ecs describe-services \
  --cluster resortslite-cluster \
  --services resortslite-service \
  --query 'services[0].[serviceName,status,runningCount,desiredCount]' \
  --output table

# View logs
aws logs tail /ecs/resortslite --follow

# Test health endpoint
curl http://resortslite-alb-123456789.us-east-1.elb.amazonaws.com/actuator/health
```

**Expected Health Response:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

---

## Configuration Management

### Environment-Specific Configuration

#### Development Environment
```bash
# Use H2 in-memory database
DB_URL=jdbc:h2:mem:resortdb
REDIS_HOST=localhost
S3_BUCKET_NAME=resortslite-dev
```

#### Production Environment
```bash
# Use external database
DB_URL=jdbc:postgresql://prod-db.example.com:5432/resortdb
DB_USERNAME=app_user
DB_PASSWORD=secure_password
REDIS_HOST=prod-redis.example.com
S3_BUCKET_NAME=resortslite-prod
```

### Using AWS Systems Manager Parameter Store

```bash
# Store sensitive values
aws ssm put-parameter \
  --name /resortslite/prod/db-password \
  --value "secure_password" \
  --type SecureString

# Reference in task definition
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:ssm:us-east-1:123456789012:parameter/resortslite/prod/db-password"
    }
  ]
}
```

### Using AWS Secrets Manager

```bash
# Create secret
aws secretsmanager create-secret \
  --name resortslite/prod/credentials \
  --secret-string '{"username":"app_user","password":"secure_password"}'

# Reference in task definition
{
  "secrets": [
    {
      "name": "DB_USERNAME",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:resortslite/prod/credentials:username::"
    },
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:resortslite/prod/credentials:password::"
    }
  ]
}
```

---

## Troubleshooting

### Common Issues and Solutions

#### 1. Task Fails to Start

**Symptom**: Tasks transition from PENDING to STOPPED immediately.

**Diagnosis**:
```bash
# Get stopped task ID
TASK_ID=$(aws ecs list-tasks \
  --cluster resortslite-cluster \
  --service-name resortslite-service \
  --desired-status STOPPED \
  --query 'taskArns[0]' \
  --output text)

# Describe stopped task
aws ecs describe-tasks \
  --cluster resortslite-cluster \
  --tasks $TASK_ID \
  --query 'tasks[0].stoppedReason'
```

**Common Causes**:
- **Invalid CPU/Memory combination**: Use valid Fargate combinations
- **Image pull error**: Verify ECR permissions and image URI
- **Missing IAM role**: Ensure ecsTaskExecutionRole exists
- **Network issues**: Check security group and subnet configuration

**Solutions**:
```bash
# Verify IAM role
aws iam get-role --role-name ecsTaskExecutionRole

# Test image pull
docker pull 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest

# Check security group rules
aws ec2 describe-security-groups --group-ids sg-xxx
```

#### 2. Application Not Accessible

**Symptom**: Cannot access application through load balancer.

**Diagnosis**:
```bash
# Check target health
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:...

# Check service events
aws ecs describe-services \
  --cluster resortslite-cluster \
  --services resortslite-service \
  --query 'services[0].events[0:5]'
```

**Common Causes**:
- **Unhealthy targets**: Application not responding to health checks
- **Security group**: Not allowing traffic on port 8080
- **Health check path**: Incorrect health check endpoint

**Solutions**:
```bash
# Update security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Verify health endpoint
curl http://TASK_IP:8080/actuator/health
```

#### 3. High Memory Usage

**Symptom**: Tasks being killed due to OOM (Out of Memory).

**Diagnosis**:
```bash
# Check CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name MemoryUtilization \
  --dimensions Name=ServiceName,Value=resortslite-service \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 3600 \
  --statistics Average
```

**Solutions**:
1. **Increase task memory**:
   ```json
   {
     "cpu": "1024",
     "memory": "2048"
   }
   ```

2. **Optimize JVM settings**:
   ```bash
   JAVA_OPTS="-Xmx1536m -Xms512m -XX:MaxRAMPercentage=75.0"
   ```

3. **Enable heap dumps**:
   ```bash
   JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"
   ```

#### 4. Slow Application Startup

**Symptom**: Tasks take long time to become healthy.

**Solutions**:
1. **Increase health check grace period**:
   ```json
   {
     "healthCheckGracePeriodSeconds": 300
   }
   ```

2. **Optimize Spring Boot startup**:
   ```properties
   spring.jmx.enabled=false
   spring.main.lazy-initialization=true
   ```

3. **Use Spring Boot 2.7+ fast startup features**

#### 5. Redis Connection Issues

**Symptom**: Application logs show Redis connection errors.

**Diagnosis**:
```bash
# Check Redis connectivity from task
aws ecs execute-command \
  --cluster resortslite-cluster \
  --task TASK_ID \
  --container resortslite \
  --interactive \
  --command "/bin/sh"

# Inside container
nc -zv redis.example.com 6379
```

**Solutions**:
- Verify Redis host and port
- Check security group allows outbound traffic to Redis
- Verify Redis authentication credentials

---

## Scaling and Management

### Manual Scaling

```bash
# Scale service to 5 tasks
aws ecs update-service \
  --cluster resortslite-cluster \
  --service resortslite-service \
  --desired-count 5

# Scale down to 2 tasks
aws ecs update-service \
  --cluster resortslite-cluster \
  --service resortslite-service \
  --desired-count 2
```

### Auto Scaling

#### Configure Target Tracking Scaling

```bash
# Register scalable target
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/resortslite-cluster/resortslite-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10

# Create scaling policy (CPU-based)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/resortslite-cluster/resortslite-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name cpu-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 70.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageCPUUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'

# Create scaling policy (Memory-based)
aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/resortslite-cluster/resortslite-service \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name memory-scaling-policy \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration '{
    "TargetValue": 80.0,
    "PredefinedMetricSpecification": {
      "PredefinedMetricType": "ECSServiceAverageMemoryUtilization"
    },
    "ScaleInCooldown": 300,
    "ScaleOutCooldown": 60
  }'
```

### Blue/Green Deployments

```bash
# Create new task definition revision
aws ecs register-task-definition \
  --cli-input-json file://ecs/task-definition.json

# Update service with new task definition
aws ecs update-service \
  --cluster resortslite-cluster \
  --service resortslite-service \
  --task-definition resortslite-task:2 \
  --deployment-configuration '{
    "maximumPercent": 200,
    "minimumHealthyPercent": 100
  }'

# Monitor deployment
aws ecs wait services-stable \
  --cluster resortslite-cluster \
  --services resortslite-service
```

### Rolling Updates

```bash
# Force new deployment (pulls latest image)
aws ecs update-service \
  --cluster resortslite-cluster \
  --service resortslite-service \
  --force-new-deployment
```

### Monitoring and Observability

#### CloudWatch Metrics

```bash
# View CPU utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=resortslite-service \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 300 \
  --statistics Average

# View memory utilization
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name MemoryUtilization \
  --dimensions Name=ServiceName,Value=resortslite-service \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-01T23:59:59Z \
  --period 300 \
  --statistics Average
```

#### CloudWatch Logs Insights

```bash
# Query application logs
aws logs start-query \
  --log-group-name /ecs/resortslite \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --query-string 'fields @timestamp, @message | filter @message like /ERROR/ | sort @timestamp desc | limit 20'
```

#### CloudWatch Alarms

```bash
# Create high CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name resortslite-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=resortslite-service

# Create high memory alarm
aws cloudwatch put-metric-alarm \
  --alarm-name resortslite-high-memory \
  --alarm-description "Alert when memory exceeds 85%" \
  --metric-name MemoryUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 85 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=ServiceName,Value=resortslite-service
```

---

## Security Considerations

### 1. Container Security

#### Use Non-Root User
The Dockerfile creates and uses a non-root user:
```dockerfile
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
```

#### Scan Images for Vulnerabilities
```bash
# Scan with AWS ECR
aws ecr start-image-scan \
  --repository-name resortslite \
  --image-id imageTag=latest

# Get scan results
aws ecr describe-image-scan-findings \
  --repository-name resortslite \
  --image-id imageTag=latest
```

### 2. Network Security

#### Security Group Best Practices
- **Principle of Least Privilege**: Only allow necessary ports
- **Source Restrictions**: Limit inbound traffic to known sources
- **Egress Control**: Restrict outbound traffic

```bash
# Allow only ALB traffic
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxx \
  --protocol tcp \
  --port 8080 \
  --source-group sg-alb-xxx
```

#### Use Private Subnets
For production, deploy tasks in private subnets with NAT Gateway:
```json
{
  "networkConfiguration": {
    "awsvpcConfiguration": {
      "subnets": ["subnet-private-1", "subnet-private-2"],
      "securityGroups": ["sg-xxx"],
      "assignPublicIp": "DISABLED"
    }
  }
}
```

### 3. Secrets Management

#### Never Hardcode Secrets
Use AWS Secrets Manager or Systems Manager Parameter Store:
```json
{
  "secrets": [
    {
      "name": "DB_PASSWORD",
      "valueFrom": "arn:aws:secretsmanager:us-east-1:123456789012:secret:db-password"
    }
  ]
}
```

#### Rotate Secrets Regularly
```bash
# Enable automatic rotation
aws secretsmanager rotate-secret \
  --secret-id resortslite/prod/db-password \
  --rotation-lambda-arn arn:aws:lambda:us-east-1:123456789012:function:SecretsManagerRotation \
  --rotation-rules AutomaticallyAfterDays=30
```

### 4. IAM Best Practices

#### Task Role Permissions
Grant only necessary permissions:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::resortslite-reports/*"
    }
  ]
}
```

#### Enable CloudTrail
Monitor API calls:
```bash
aws cloudtrail create-trail \
  --name resortslite-trail \
  --s3-bucket-name resortslite-cloudtrail-logs
```

### 5. Compliance and Auditing

#### Enable Container Insights
```bash
aws ecs update-cluster-settings \
  --cluster resortslite-cluster \
  --settings name=containerInsights,value=enabled
```

#### Enable VPC Flow Logs
```bash
aws ec2 create-flow-logs \
  --resource-type VPC \
  --resource-ids vpc-xxx \
  --traffic-type ALL \
  --log-destination-type cloud-watch-logs \
  --log-group-name /aws/vpc/flowlogs
```

---

## Technology-Specific Notes

### Spring Boot Configuration

#### Profiles
Use Spring profiles for environment-specific configuration:
```properties
# application-docker.properties
spring.profiles.active=docker
logging.level.root=INFO
management.endpoints.web.exposure.include=health,info,metrics
```

#### Actuator Endpoints
Configure health checks:
```properties
management.endpoint.health.show-details=when-authorized
management.health.redis.enabled=true
management.health.db.enabled=true
```

#### Graceful Shutdown
Enable graceful shutdown for zero-downtime deployments:
```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Java 8 Considerations

#### JVM Tuning for Containers
```bash
# Recommended JVM options
JAVA_OPTS="-Xmx512m -Xms256m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200"
```

#### Monitoring JVM Metrics
Enable JMX for monitoring:
```bash
JAVA_OPTS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false"
```

### Maven Build Optimization

#### Dependency Caching
The Dockerfile uses layer caching for faster builds:
```dockerfile
# Copy pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Then copy source
COPY src ./src
RUN mvn clean package -DskipTests -B
```

#### Skip Tests in Docker Build
Tests should run in CI/CD pipeline, not during Docker build:
```bash
mvn clean package -DskipTests
```

### Redis Session Management

#### Configuration
```properties
spring.session.store-type=redis
spring.redis.host=${REDIS_HOST}
spring.redis.port=${REDIS_PORT}
spring.session.redis.namespace=resortslite:session
```

#### High Availability
Use Redis Cluster or ElastiCache with Multi-AZ:
```bash
# Create ElastiCache cluster
aws elasticache create-replication-group \
  --replication-group-id resortslite-redis \
  --replication-group-description "Redis for ResortsLite sessions" \
  --engine redis \
  --cache-node-type cache.t3.micro \
  --num-cache-clusters 2 \
  --automatic-failover-enabled
```

### AWS S3 Integration

#### IAM Permissions
Grant S3 access through task role:
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:GetObject",
    "s3:PutObject",
    "s3:DeleteObject"
  ],
  "Resource": "arn:aws:s3:::resortslite-reports/*"
}
```

#### S3 Configuration
```properties
aws.s3.bucket-name=${S3_BUCKET_NAME}
aws.s3.region=${AWS_REGION}
```

---

## Additional Resources

### AWS Documentation
- [ECS Fargate Documentation](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html)
- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [ECS Service Auto Scaling](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html)

### Spring Boot Resources
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Spring Session Redis](https://docs.spring.io/spring-session/docs/current/reference/html5/#httpsession-redis)

### Best Practices
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [12-Factor App Methodology](https://12factor.net/)

---

## Support and Maintenance

### Getting Help
- Check CloudWatch logs for application errors
- Review ECS service events for deployment issues
- Consult AWS Support for infrastructure problems

### Maintenance Tasks
- **Weekly**: Review CloudWatch metrics and logs
- **Monthly**: Update Docker base images and dependencies
- **Quarterly**: Review and optimize resource allocation
- **Annually**: Security audit and compliance review

---

## Conclusion

This guide provides comprehensive instructions for deploying ResortsLite on AWS ECS Fargate. Follow the steps carefully, and refer to the troubleshooting section for common issues. For production deployments, ensure all security best practices are implemented.

**Quick Start Summary:**
1. Build and push Docker image: `./scripts/build-push.sh`
2. Deploy to ECS: `./scripts/deploy-image.sh`
3. Verify deployment: Check health endpoint and CloudWatch logs
4. Configure auto-scaling and monitoring
5. Implement security best practices

For questions or issues, consult the AWS documentation or contact your DevOps team.
