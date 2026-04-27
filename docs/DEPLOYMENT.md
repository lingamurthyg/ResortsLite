# ResortsLite - Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development with Docker](#local-development-with-docker)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Health Checks](#monitoring-and-health-checks)
8. [Troubleshooting](#troubleshooting)
9. [Security Considerations](#security-considerations)
10. [Scaling and Performance](#scaling-and-performance)

---

## Overview

ResortsLite is a Spring Boot 2.7.x application built with Java 8, designed for containerized deployment on AWS EKS (Elastic Kubernetes Service). This guide provides comprehensive instructions for building, deploying, and managing the application in both local and cloud environments.

**Technology Stack:**
- Java 8
- Spring Boot 2.7.18
- Maven 3.x
- Spring Boot Actuator (health checks)
- Spring Session with Redis
- AWS SDK for S3
- H2 Database (in-memory for demo)

**Target Platform:**
- AWS EKS (Elastic Kubernetes Service)
- Docker containers
- AWS Application Load Balancer (ALB)

---

## Prerequisites

### Required Tools

#### For Local Development:
- **Docker Desktop** (v20.10+)
  - Download: https://www.docker.com/products/docker-desktop
  - Verify: `docker --version`
- **Docker Compose** (v2.0+)
  - Usually included with Docker Desktop
  - Verify: `docker-compose --version`

#### For AWS EKS Deployment:
- **AWS CLI** (v2.x)
  - Installation: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
  - Verify: `aws --version`
  - Configure: `aws configure`
- **kubectl** (v1.24+)
  - Installation: https://kubernetes.io/docs/tasks/tools/
  - Verify: `kubectl version --client`
- **eksctl** (optional, for cluster creation)
  - Installation: https://eksctl.io/installation/
  - Verify: `eksctl version`

#### For Building:
- **Java 8 JDK** (for local builds)
  - Download: https://adoptium.net/
  - Verify: `java -version`
- **Maven 3.x** (for local builds)
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`

### AWS Prerequisites

1. **AWS Account** with appropriate permissions
2. **IAM User/Role** with the following permissions:
   - EKS cluster access
   - ECR repository access (if using ECR)
   - EC2 and VPC permissions
   - IAM permissions for service accounts
3. **EKS Cluster** (if not already created)
4. **AWS Load Balancer Controller** installed in EKS cluster
   - Installation guide: https://docs.aws.amazon.com/eks/latest/userguide/aws-load-balancer-controller.html

### External Services

The application requires the following external services:
- **Redis** (for session management)
- **AWS S3** (for file storage)
- **Payment Service** (external API)
- **Inventory Service** (external API)
- **Notification Service** (external API)

---

## Local Development with Docker

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd ResortsliteComp
```

### Step 2: Review Configuration

Edit `src/main/resources/application.properties` to configure local settings:

```properties
# Application port
server.port=8080

# Redis configuration
spring.redis.host=${REDIS_HOST:localhost}
spring.redis.port=${REDIS_PORT:6379}

# AWS S3 configuration
aws.s3.bucket-name=${S3_BUCKET_NAME:resorts-lite-files}
aws.s3.region=${AWS_REGION:us-east-1}
```

### Step 3: Build and Run with Docker Compose

```bash
# Build and start the application
docker-compose up --build

# Run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Step 4: Access the Application

- **Application URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **H2 Console**: http://localhost:8080/h2-console

### Step 5: Local Testing

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Test application endpoint
curl http://localhost:8080/api/bookings
```

---

## Building and Pushing Docker Images

### Option 1: Using build-push.sh (Linux/macOS)

```bash
# Make script executable
chmod +x scripts/build-push.sh

# Run the script
./scripts/build-push.sh
```

The script will prompt you for:
1. **Registry Type**: AWS ECR or Docker Hub
2. **Registry Details**: Region, Account ID, Repository Name (for ECR) or Username/Password (for Docker Hub)
3. **Image Tag**: Version tag (default: latest)

### Option 2: Using build-push.bat (Windows)

```cmd
# Run the script
scripts\build-push.bat
```

Follow the same prompts as the Linux/macOS version.

### Manual Build and Push

#### AWS ECR:

```bash
# Set variables
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
ECR_REPO=resortslite
IMAGE_TAG=latest

# Authenticate with ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com

# Create ECR repository (if not exists)
aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION

# Build image
docker build -t $ECR_REPO:$IMAGE_TAG .

# Tag image
docker tag $ECR_REPO:$IMAGE_TAG \
  $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG

# Push image
docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO:$IMAGE_TAG
```

#### Docker Hub:

```bash
# Set variables
DOCKER_USERNAME=yourusername
IMAGE_NAME=resortslite
IMAGE_TAG=latest

# Login to Docker Hub
docker login -u $DOCKER_USERNAME

# Build image
docker build -t $DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG .

# Push image
docker push $DOCKER_USERNAME/$IMAGE_NAME:$IMAGE_TAG
```

---

## AWS EKS Deployment

### Prerequisites Check

Before deploying, ensure:
1. EKS cluster is running
2. kubectl is configured for your cluster
3. AWS Load Balancer Controller is installed
4. Docker image is pushed to registry

### Step 1: Configure kubectl for EKS

```bash
# Update kubeconfig
aws eks update-kubeconfig --region us-east-1 --name your-cluster-name

# Verify connection
kubectl cluster-info
kubectl get nodes
```

### Step 2: Deploy Using deploy-image.sh (Linux/macOS)

```bash
# Make script executable
chmod +x scripts/deploy-image.sh

# Run deployment script
./scripts/deploy-image.sh
```

The script will prompt you for:
1. **AWS Region**: Your EKS cluster region
2. **EKS Cluster Name**: Name of your EKS cluster
3. **Docker Image URI**: Full image path with tag
4. **Configuration Values**:
   - Redis Host, Port, Password
   - S3 Bucket Name, AWS Region
   - External Service Endpoints

### Step 3: Deploy Using deploy-image.bat (Windows)

```cmd
# Run deployment script
scripts\deploy-image.bat
```

Follow the same prompts as the Linux/macOS version.

### Step 4: Manual Deployment

If you prefer manual deployment:

```bash
# 1. Update deployment.yaml with your image URI
sed -i 's|{{IMAGE_URI}}|123456789.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest|g' \
  kubernetes/deployment.yaml

# 2. Update environment variables in deployment.yaml
# Edit kubernetes/deployment.yaml and replace {{PLACEHOLDER}} values

# 3. Apply manifests
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/deployment.yaml
kubectl apply -f kubernetes/service.yaml
kubectl apply -f kubernetes/ingress.yaml

# 4. Wait for deployment
kubectl rollout status deployment/resortslite -n resortslite

# 5. Verify deployment
kubectl get pods,svc,ingress -n resortslite
```

### Step 5: Access the Application

```bash
# Get ingress URL
kubectl get ingress resortslite-ingress -n resortslite

# The output will show the Load Balancer hostname
# Example: abc123-1234567890.us-east-1.elb.amazonaws.com

# Access the application
curl http://<load-balancer-hostname>/actuator/health
```

**Note**: It may take 5-10 minutes for the AWS Load Balancer to become fully operational.

---

## Configuration Management

### Environment Variables

The application uses the following environment variables:

#### Spring Configuration:
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default: production)

#### Database Configuration:
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

#### Redis Configuration:
- `REDIS_HOST`: Redis server hostname
- `REDIS_PORT`: Redis server port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

#### AWS S3 Configuration:
- `S3_BUCKET_NAME`: S3 bucket name for file storage
- `AWS_REGION`: AWS region for S3
- `AWS_ACCESS_KEY_ID`: AWS access key (if not using IAM roles)
- `AWS_SECRET_ACCESS_KEY`: AWS secret key (if not using IAM roles)

#### External Service Endpoints:
- `APP_PAYMENT_ENDPOINT`: Payment service URL
- `APP_INVENTORY_ENDPOINT`: Inventory service URL
- `APP_NOTIFICATION_ENDPOINT`: Notification service URL

#### JVM Options:
- `JAVA_OPTS`: JVM options (default: -Xmx512m -Xms256m)

### Updating Configuration

#### For Kubernetes Deployment:

1. Edit `kubernetes/deployment.yaml`
2. Update environment variables in the `env` section
3. Apply changes:
   ```bash
   kubectl apply -f kubernetes/deployment.yaml
   kubectl rollout restart deployment/resortslite -n resortslite
   ```

#### Using Kubernetes ConfigMaps:

```bash
# Create ConfigMap
kubectl create configmap resortslite-config \
  --from-literal=REDIS_HOST=redis.example.com \
  --from-literal=S3_BUCKET_NAME=my-bucket \
  -n resortslite

# Update deployment to use ConfigMap
# Add to deployment.yaml under containers.env:
# - name: REDIS_HOST
#   valueFrom:
#     configMapKeyRef:
#       name: resortslite-config
#       key: REDIS_HOST
```

#### Using Kubernetes Secrets:

```bash
# Create Secret
kubectl create secret generic resortslite-secrets \
  --from-literal=REDIS_PASSWORD=mypassword \
  --from-literal=AWS_SECRET_ACCESS_KEY=mysecret \
  -n resortslite

# Update deployment to use Secret
# Add to deployment.yaml under containers.env:
# - name: REDIS_PASSWORD
#   valueFrom:
#     secretKeyRef:
#       name: resortslite-secrets
#       key: REDIS_PASSWORD
```

---

## Monitoring and Health Checks

### Health Check Endpoints

The application exposes Spring Boot Actuator endpoints:

- **Health Check**: `/actuator/health`
- **Application Info**: `/actuator/info`

### Kubernetes Health Probes

The deployment includes:

#### Liveness Probe:
- Checks if the application is running
- Path: `/actuator/health`
- Initial delay: 60 seconds
- Period: 10 seconds
- Failure threshold: 3

#### Readiness Probe:
- Checks if the application is ready to serve traffic
- Path: `/actuator/health`
- Initial delay: 30 seconds
- Period: 10 seconds
- Failure threshold: 3

### Viewing Logs

```bash
# View all pods in namespace
kubectl get pods -n resortslite

# View logs for a specific pod
kubectl logs <pod-name> -n resortslite

# Follow logs in real-time
kubectl logs -f <pod-name> -n resortslite

# View logs for all pods with label
kubectl logs -n resortslite -l app=resortslite --tail=100

# View previous container logs (if pod restarted)
kubectl logs <pod-name> -n resortslite --previous
```

### Monitoring Resources

```bash
# View resource usage
kubectl top pods -n resortslite
kubectl top nodes

# Describe pod for detailed information
kubectl describe pod <pod-name> -n resortslite

# View events
kubectl get events -n resortslite --sort-by='.lastTimestamp'
```

---

## Troubleshooting

### Common Issues

#### 1. Pod Not Starting

**Symptoms**: Pod stuck in `Pending`, `CrashLoopBackOff`, or `ImagePullBackOff` state

**Diagnosis**:
```bash
kubectl get pods -n resortslite
kubectl describe pod <pod-name> -n resortslite
kubectl logs <pod-name> -n resortslite
```

**Solutions**:
- **ImagePullBackOff**: Check image URI, registry authentication
- **CrashLoopBackOff**: Check application logs for errors
- **Pending**: Check resource availability, node capacity

#### 2. Application Not Accessible

**Symptoms**: Cannot access application via Load Balancer URL

**Diagnosis**:
```bash
kubectl get ingress -n resortslite
kubectl describe ingress resortslite-ingress -n resortslite
kubectl get svc -n resortslite
```

**Solutions**:
- Verify AWS Load Balancer Controller is installed
- Check ingress annotations
- Verify security groups allow traffic
- Wait for Load Balancer provisioning (5-10 minutes)

#### 3. Health Check Failures

**Symptoms**: Pods restarting frequently, health probes failing

**Diagnosis**:
```bash
kubectl describe pod <pod-name> -n resortslite
kubectl logs <pod-name> -n resortslite
```

**Solutions**:
- Increase `initialDelaySeconds` for liveness probe
- Check application startup time
- Verify health endpoint is accessible
- Check JVM memory settings

#### 4. Database Connection Issues

**Symptoms**: Application logs show database connection errors

**Solutions**:
- Verify database credentials in environment variables
- Check database hostname and port
- Ensure database is accessible from EKS cluster
- Verify security groups and network policies

#### 5. Redis Connection Issues

**Symptoms**: Session management not working, Redis connection errors

**Solutions**:
- Verify Redis host and port configuration
- Check Redis password (if required)
- Ensure Redis is accessible from EKS cluster
- Test Redis connectivity: `redis-cli -h <host> -p <port> ping`

### Debugging Commands

```bash
# Execute command in pod
kubectl exec -it <pod-name> -n resortslite -- /bin/sh

# Port forward to local machine
kubectl port-forward <pod-name> -n resortslite 8080:8080

# Check pod environment variables
kubectl exec <pod-name> -n resortslite -- env

# Check DNS resolution
kubectl exec <pod-name> -n resortslite -- nslookup redis.example.com

# View deployment status
kubectl rollout status deployment/resortslite -n resortslite

# View deployment history
kubectl rollout history deployment/resortslite -n resortslite
```

### Rollback Deployment

```bash
# Rollback to previous version
kubectl rollout undo deployment/resortslite -n resortslite

# Rollback to specific revision
kubectl rollout undo deployment/resortslite -n resortslite --to-revision=2

# Check rollout status
kubectl rollout status deployment/resortslite -n resortslite
```

---

## Security Considerations

### Container Security

1. **Non-Root User**: The Dockerfile creates and uses a non-root user (`appuser`)
2. **Minimal Base Image**: Uses `eclipse-temurin:8-jdk` for smaller attack surface
3. **No Unnecessary Tools**: Dockerfile doesn't install curl, wget, or other tools

### Kubernetes Security

1. **Namespace Isolation**: Application runs in dedicated `resortslite` namespace
2. **Resource Limits**: CPU and memory limits prevent resource exhaustion
3. **Network Policies**: Consider implementing network policies to restrict traffic

### Secrets Management

**Best Practices**:
- Use Kubernetes Secrets for sensitive data
- Use AWS Secrets Manager or Parameter Store for production
- Never commit secrets to version control
- Rotate secrets regularly

**Example using AWS Secrets Manager**:
```bash
# Create secret in AWS Secrets Manager
aws secretsmanager create-secret \
  --name resortslite/redis-password \
  --secret-string "mypassword" \
  --region us-east-1

# Use External Secrets Operator to sync to Kubernetes
# https://external-secrets.io/
```

### IAM Roles for Service Accounts (IRSA)

Instead of using AWS access keys, use IRSA for S3 access:

```bash
# Create IAM policy
aws iam create-policy \
  --policy-name ResortsliteS3Policy \
  --policy-document file://s3-policy.json

# Create service account with IAM role
eksctl create iamserviceaccount \
  --name resortslite-sa \
  --namespace resortslite \
  --cluster your-cluster-name \
  --attach-policy-arn arn:aws:iam::123456789012:policy/ResortsliteS3Policy \
  --approve

# Update deployment to use service account
# Add to deployment.yaml under spec.template.spec:
# serviceAccountName: resortslite-sa
```

---

## Scaling and Performance

### Horizontal Pod Autoscaling (HPA)

Create HPA to automatically scale based on CPU/memory usage:

```bash
# Create HPA
kubectl autoscale deployment resortslite \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n resortslite

# View HPA status
kubectl get hpa -n resortslite

# Describe HPA
kubectl describe hpa resortslite -n resortslite
```

**HPA YAML**:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: resortslite-hpa
  namespace: resortslite
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: resortslite
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### Manual Scaling

```bash
# Scale to specific number of replicas
kubectl scale deployment resortslite --replicas=5 -n resortslite

# Verify scaling
kubectl get pods -n resortslite
```

### Performance Tuning

#### JVM Tuning:

Update `JAVA_OPTS` in deployment.yaml:
```yaml
- name: JAVA_OPTS
  value: "-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### Resource Limits:

Adjust resource requests and limits based on load testing:
```yaml
resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
  limits:
    cpu: "1000m"
    memory: "2Gi"
```

### Load Testing

```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Run load test
ab -n 1000 -c 10 http://<load-balancer-url>/actuator/health

# Or use hey
hey -n 1000 -c 10 http://<load-balancer-url>/actuator/health
```

---

## Additional Resources

### Documentation Links:
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)

### Useful Commands Reference:

```bash
# Kubernetes
kubectl get all -n resortslite
kubectl delete namespace resortslite
kubectl apply -f kubernetes/
kubectl rollout restart deployment/resortslite -n resortslite

# Docker
docker ps
docker logs <container-id>
docker exec -it <container-id> /bin/sh
docker-compose up -d
docker-compose down

# AWS
aws eks list-clusters
aws ecr describe-repositories
aws sts get-caller-identity
```

---

## Support and Maintenance

### Regular Maintenance Tasks:

1. **Update Dependencies**: Regularly update Spring Boot and dependencies
2. **Security Patches**: Apply security patches promptly
3. **Monitor Logs**: Review application logs for errors
4. **Resource Monitoring**: Monitor CPU, memory, and disk usage
5. **Backup**: Ensure data backups are configured
6. **Cost Optimization**: Review AWS costs and optimize resources

### Getting Help:

- Check application logs: `kubectl logs -n resortslite -l app=resortslite`
- Review Kubernetes events: `kubectl get events -n resortslite`
- Consult AWS Support for EKS-specific issues
- Review Spring Boot documentation for application issues

---

## Conclusion

This deployment guide provides comprehensive instructions for deploying ResortsLite to AWS EKS. Follow the steps carefully, and refer to the troubleshooting section if you encounter issues. For production deployments, ensure all security best practices are implemented and monitoring is configured.

**Quick Start Summary**:
1. Build image: `./scripts/build-push.sh`
2. Deploy to EKS: `./scripts/deploy-image.sh`
3. Verify: `kubectl get pods,svc,ingress -n resortslite`
4. Access: Use Load Balancer URL from ingress

For questions or issues, consult the troubleshooting section or contact your DevOps team.
