# ResortsLite - AWS EKS Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Building and Pushing Docker Images](#building-and-pushing-docker-images)
5. [AWS EKS Deployment](#aws-eks-deployment)
6. [Configuration Management](#configuration-management)
7. [Monitoring and Health Checks](#monitoring-and-health-checks)
8. [Troubleshooting](#troubleshooting)
9. [Security Considerations](#security-considerations)
10. [Scaling and Management](#scaling-and-management)

---

## Overview

ResortsLite is a Spring Boot 2.7.x application built with Java 8, designed for containerized deployment on AWS EKS (Elastic Kubernetes Service). This guide provides comprehensive instructions for building, deploying, and managing the application in a Kubernetes environment.

### Technology Stack
- **Framework**: Spring Boot 2.7.18
- **Java Version**: Java 8 (1.8)
- **Build Tool**: Maven 3.x
- **Container Runtime**: Docker
- **Orchestration**: Kubernetes (AWS EKS)
- **Dependencies**: 
  - Spring Web
  - Spring JDBC
  - Spring Boot Actuator
  - Spring Session with Redis
  - AWS SDK for S3 and ElastiCache
  - H2 Database (in-memory)

### Application Features
- RESTful API for resort booking management
- Distributed session management with Redis
- File storage integration with AWS S3
- Health monitoring with Spring Boot Actuator
- External service integrations (payment, inventory, notifications)

---

## Prerequisites

### Required Tools

#### 1. Docker
- **Version**: Docker 20.10+ or later
- **Installation**: 
  - Linux: `curl -fsSL https://get.docker.com | sh`
  - macOS: Download Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop)
  - Windows: Download Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop)
- **Verification**: `docker --version`

#### 2. AWS CLI
- **Version**: AWS CLI v2
- **Installation**:
  - Linux: `curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && unzip awscliv2.zip && sudo ./aws/install`
  - macOS: `brew install awscli`
  - Windows: Download installer from [AWS CLI](https://aws.amazon.com/cli/)
- **Configuration**: `aws configure`
- **Verification**: `aws --version`

#### 3. kubectl
- **Version**: kubectl 1.24+ (compatible with your EKS cluster version)
- **Installation**:
  - Linux: `curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && chmod +x kubectl && sudo mv kubectl /usr/local/bin/`
  - macOS: `brew install kubectl`
  - Windows: `choco install kubernetes-cli`
- **Verification**: `kubectl version --client`

#### 4. eksctl (Optional but Recommended)
- **Version**: eksctl 0.140+
- **Installation**:
  - Linux/macOS: `curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp && sudo mv /tmp/eksctl /usr/local/bin`
  - Windows: `choco install eksctl`
- **Verification**: `eksctl version`

### AWS Requirements

#### IAM Permissions
Your AWS user/role must have the following permissions:
- **ECR**: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:PutImage`, `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:CreateRepository`, `ecr:DescribeRepositories`
- **EKS**: `eks:DescribeCluster`, `eks:ListClusters`, `eks:UpdateKubeconfig`
- **EC2**: `ec2:DescribeSecurityGroups`, `ec2:DescribeSubnets`, `ec2:DescribeVpcs`
- **IAM**: `iam:CreateRole`, `iam:AttachRolePolicy`, `iam:GetRole`

#### AWS Resources
- **EKS Cluster**: An existing EKS cluster or create one using eksctl
- **ECR Repository**: Will be auto-created by build-push scripts
- **VPC**: Configured VPC with public/private subnets
- **IAM Roles**: EKS cluster role and node group role

### External Services (Optional)
- **Redis**: For distributed session management (AWS ElastiCache or standalone)
- **S3 Bucket**: For file storage (will be created if doesn't exist)
- **Database**: H2 in-memory (default) or external database (PostgreSQL, MySQL)

---

## Local Development Setup

### 1. Clone the Repository
```bash
cd /path/to/project
```

### 2. Build the Application Locally
```bash
# Using Maven
mvn clean package -DskipTests

# Verify the build
ls -lh target/*.jar
```

### 3. Run Locally with Docker Compose

#### Start the Application
```bash
docker-compose up --build
```

#### Access the Application
- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **H2 Console**: http://localhost:8080/h2-console

#### Environment Variables for Local Development
Edit `docker-compose.yml` to configure:
```yaml
environment:
  REDIS_HOST: your-redis-host
  REDIS_PORT: 6379
  REDIS_PASSWORD: your-redis-password
  S3_BUCKET_NAME: your-s3-bucket
  AWS_REGION: us-east-1
  AWS_ACCESS_KEY_ID: your-access-key
  AWS_SECRET_ACCESS_KEY: your-secret-key
```

#### Stop the Application
```bash
docker-compose down
```

---

## Building and Pushing Docker Images

### Option 1: AWS ECR (Recommended for EKS)

#### Linux/macOS
```bash
cd /path/to/project
chmod +x scripts/build-push.sh
./scripts/build-push.sh
```

#### Windows
```cmd
cd \path\to\project
scripts\build-push.bat
```

#### Interactive Prompts
1. **Select Registry**: Choose `1` for AWS ECR
2. **AWS Region**: Enter your AWS region (e.g., `us-east-1`)
3. **AWS Account ID**: Enter your 12-digit AWS account ID
4. **ECR Repository Name**: Enter repository name (default: `resortslite`)
5. **Image Tag**: Enter tag (default: `latest`)

#### Script Actions
- Authenticates with AWS ECR
- Creates ECR repository if it doesn't exist
- Builds Docker image with multi-stage build
- Tags image with proper naming convention
- Pushes image to ECR

#### Example Output
```
Image: 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest
```

### Option 2: Docker Hub

#### Interactive Prompts
1. **Select Registry**: Choose `2` for Docker Hub
2. **Docker Hub Username**: Enter your username
3. **Docker Hub Password**: Enter your password or access token
4. **Repository Name**: Enter repository name (default: `resortslite`)
5. **Image Tag**: Enter tag (default: `latest`)

#### Example Output
```
Image: yourusername/resortslite:latest
```

### Manual Build (Alternative)
```bash
# Build image
docker build -t resortslite:latest .

# Tag for ECR
docker tag resortslite:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push to ECR
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest
```

---

## AWS EKS Deployment

### Prerequisites
1. **EKS Cluster**: Ensure you have an EKS cluster running
2. **kubectl Configuration**: Configure kubectl to access your cluster
3. **Docker Image**: Build and push image to ECR (see previous section)
4. **AWS Load Balancer Controller**: Install for ingress support

### Step 1: Create EKS Cluster (If Not Exists)

#### Using eksctl
```bash
eksctl create cluster \
  --name resortslite-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 2 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed
```

#### Verify Cluster
```bash
aws eks list-clusters --region us-east-1
kubectl get nodes
```

### Step 2: Install AWS Load Balancer Controller

#### Create IAM Policy
```bash
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.5.4/docs/install/iam_policy.json

aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json
```

#### Create IAM Service Account
```bash
eksctl create iamserviceaccount \
  --cluster=resortslite-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::ACCOUNT_ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --approve
```

#### Install Controller with Helm
```bash
helm repo add eks https://aws.github.io/eks-charts
helm repo update

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=resortslite-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

### Step 3: Deploy Application to EKS

#### Linux/macOS
```bash
chmod +x scripts/deploy-image.sh
./scripts/deploy-image.sh
```

#### Windows
```cmd
scripts\deploy-image.bat
```

#### Interactive Prompts
1. **AWS Region**: Enter your AWS region (e.g., `us-east-1`)
2. **EKS Cluster Name**: Enter your cluster name
3. **Docker Image URI**: Enter full image URI with tag
   - Example: `123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest`

#### Environment Variable Configuration
The script will prompt for the following configuration values:

**Database Configuration:**
- `DB_URL`: Database connection URL (default: H2 in-memory)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

**External Services:**
- `PAYMENT_ENDPOINT`: Payment service URL
- `INVENTORY_ENDPOINT`: Inventory service URL
- `NOTIFICATION_ENDPOINT`: Notification service URL

**Redis Configuration:**
- `REDIS_HOST`: Redis server hostname
- `REDIS_PORT`: Redis server port (default: 6379)
- `REDIS_PASSWORD`: Redis password

**AWS S3 Configuration:**
- `S3_BUCKET_NAME`: S3 bucket for file storage
- `AWS_REGION`: AWS region for S3

#### Deployment Process
1. Configures kubectl for EKS cluster
2. Verifies cluster connectivity
3. Updates Kubernetes manifests with provided values
4. Creates namespace: `resortslite`
5. Deploys application with 2 replicas
6. Creates ClusterIP service
7. Creates ALB ingress
8. Waits for deployment rollout
9. Displays deployment status and access URLs

### Step 4: Verify Deployment

#### Check Pods
```bash
kubectl get pods -n resortslite
```

Expected output:
```
NAME                           READY   STATUS    RESTARTS   AGE
resortslite-xxxxxxxxxx-xxxxx   1/1     Running   0          2m
resortslite-xxxxxxxxxx-xxxxx   1/1     Running   0          2m
```

#### Check Services
```bash
kubectl get svc -n resortslite
```

#### Check Ingress
```bash
kubectl get ingress -n resortslite
```

#### View Logs
```bash
kubectl logs -n resortslite -l app=resortslite -f
```

### Step 5: Access the Application

#### Get Ingress URL
```bash
kubectl get ingress resortslite-ingress -n resortslite -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

#### Test Health Endpoint
```bash
curl http://INGRESS_URL/actuator/health
```

Expected response:
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

### Environment Variables

The application uses environment variables for configuration. These are defined in `kubernetes/deployment.yaml`:

#### Application Configuration
- `SERVER_PORT`: Application port (default: 8080)
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default: production)

#### Database Configuration
- `DB_URL`: JDBC connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

#### External Services
- `PAYMENT_ENDPOINT`: Payment service endpoint
- `INVENTORY_ENDPOINT`: Inventory service endpoint
- `NOTIFICATION_ENDPOINT`: Notification service endpoint

#### Redis Configuration
- `REDIS_HOST`: Redis server hostname
- `REDIS_PORT`: Redis server port
- `REDIS_PASSWORD`: Redis password

#### AWS Configuration
- `S3_BUCKET_NAME`: S3 bucket name
- `AWS_REGION`: AWS region

### Using Kubernetes Secrets

For sensitive data, use Kubernetes secrets instead of plain environment variables:

#### Create Secret
```bash
kubectl create secret generic resortslite-secrets \
  --from-literal=db-password=your-db-password \
  --from-literal=redis-password=your-redis-password \
  -n resortslite
```

#### Update Deployment
Edit `kubernetes/deployment.yaml`:
```yaml
env:
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: resortslite-secrets
      key: db-password
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: resortslite-secrets
      key: redis-password
```

### Using ConfigMaps

For non-sensitive configuration:

#### Create ConfigMap
```bash
kubectl create configmap resortslite-config \
  --from-literal=payment-endpoint=http://payment-svc:9090/charge \
  --from-literal=inventory-endpoint=http://inventory-svc:8081/rooms \
  -n resortslite
```

#### Update Deployment
```yaml
env:
- name: PAYMENT_ENDPOINT
  valueFrom:
    configMapKeyRef:
      name: resortslite-config
      key: payment-endpoint
```

---

## Monitoring and Health Checks

### Spring Boot Actuator Endpoints

The application exposes the following actuator endpoints:

- **Health**: `/actuator/health` - Application health status
- **Info**: `/actuator/info` - Application information

### Kubernetes Health Probes

#### Liveness Probe
Checks if the application is running:
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

#### Readiness Probe
Checks if the application is ready to serve traffic:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### Monitoring Commands

#### View Pod Status
```bash
kubectl get pods -n resortslite -w
```

#### View Pod Logs
```bash
# All pods
kubectl logs -n resortslite -l app=resortslite --tail=100

# Specific pod
kubectl logs -n resortslite resortslite-xxxxxxxxxx-xxxxx -f

# Previous container logs (if crashed)
kubectl logs -n resortslite resortslite-xxxxxxxxxx-xxxxx --previous
```

#### Describe Pod
```bash
kubectl describe pod -n resortslite resortslite-xxxxxxxxxx-xxxxx
```

#### View Events
```bash
kubectl get events -n resortslite --sort-by='.lastTimestamp'
```

### Application Metrics

For production monitoring, consider integrating:
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **AWS CloudWatch**: AWS-native monitoring
- **Datadog/New Relic**: APM solutions

---

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting

**Symptoms**: Pods stuck in `Pending`, `CrashLoopBackOff`, or `ImagePullBackOff`

**Diagnosis**:
```bash
kubectl describe pod -n resortslite POD_NAME
kubectl logs -n resortslite POD_NAME
```

**Solutions**:
- **ImagePullBackOff**: Verify image URI and ECR permissions
- **CrashLoopBackOff**: Check application logs for startup errors
- **Pending**: Check node resources and pod resource requests

#### 2. Service Not Accessible

**Symptoms**: Cannot access application via ingress URL

**Diagnosis**:
```bash
kubectl get ingress -n resortslite
kubectl describe ingress resortslite-ingress -n resortslite
kubectl get svc -n resortslite
```

**Solutions**:
- Verify AWS Load Balancer Controller is installed
- Check security groups allow traffic on port 80/443
- Verify ingress annotations are correct
- Check service selector matches pod labels

#### 3. Health Check Failures

**Symptoms**: Pods restarting frequently, readiness probe failures

**Diagnosis**:
```bash
kubectl logs -n resortslite POD_NAME
kubectl describe pod -n resortslite POD_NAME
```

**Solutions**:
- Increase `initialDelaySeconds` for slow startup
- Verify `/actuator/health` endpoint is accessible
- Check Redis connectivity if health check includes Redis
- Review application logs for errors

#### 4. Database Connection Issues

**Symptoms**: Application logs show database connection errors

**Solutions**:
- Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables
- Check database is accessible from EKS cluster
- Verify security groups allow database traffic
- Test connection from a debug pod

#### 5. Redis Connection Issues

**Symptoms**: Session management failures, Redis health check fails

**Solutions**:
- Verify `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` are correct
- Check Redis is accessible from EKS cluster
- Verify security groups allow Redis traffic (port 6379)
- Test connection: `redis-cli -h REDIS_HOST -p REDIS_PORT -a REDIS_PASSWORD ping`

### Debug Commands

#### Execute Shell in Pod
```bash
kubectl exec -it -n resortslite POD_NAME -- /bin/sh
```

#### Port Forward for Local Testing
```bash
kubectl port-forward -n resortslite POD_NAME 8080:8080
```

#### Check Resource Usage
```bash
kubectl top pods -n resortslite
kubectl top nodes
```

#### View Deployment History
```bash
kubectl rollout history deployment/resortslite -n resortslite
```

---

## Security Considerations

### Container Security

1. **Non-Root User**: Application runs as non-root user `appuser`
2. **Minimal Base Image**: Uses `eclipse-temurin:8-jdk` for smaller attack surface
3. **No Unnecessary Tools**: No curl, wget, or debugging tools in production image

### Kubernetes Security

1. **Namespace Isolation**: Application runs in dedicated `resortslite` namespace
2. **Resource Limits**: CPU and memory limits prevent resource exhaustion
3. **Network Policies**: Consider implementing network policies for pod-to-pod communication
4. **RBAC**: Use Role-Based Access Control for kubectl access

### Secrets Management

1. **Use Kubernetes Secrets**: Store sensitive data in secrets, not environment variables
2. **AWS Secrets Manager**: Consider using AWS Secrets Manager for production
3. **Encrypt Secrets**: Enable encryption at rest for Kubernetes secrets
4. **Rotate Credentials**: Regularly rotate database and Redis passwords

### Network Security

1. **Private Subnets**: Deploy pods in private subnets
2. **Security Groups**: Restrict ingress/egress traffic
3. **TLS/SSL**: Enable HTTPS on ingress (add certificate ARN annotation)
4. **WAF**: Consider AWS WAF for application-level protection

### Image Security

1. **Scan Images**: Use AWS ECR image scanning or Trivy
2. **Signed Images**: Consider image signing with Docker Content Trust
3. **Private Registry**: Use private ECR repositories
4. **Vulnerability Patching**: Regularly update base images and dependencies

---

## Scaling and Management

### Horizontal Pod Autoscaling (HPA)

#### Create HPA
```bash
kubectl autoscale deployment resortslite \
  --cpu-percent=70 \
  --min=2 \
  --max=10 \
  -n resortslite
```

#### HPA Manifest
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

#### View HPA Status
```bash
kubectl get hpa -n resortslite
```

### Manual Scaling

#### Scale Up
```bash
kubectl scale deployment/resortslite --replicas=5 -n resortslite
```

#### Scale Down
```bash
kubectl scale deployment/resortslite --replicas=2 -n resortslite
```

### Rolling Updates

#### Update Image
```bash
kubectl set image deployment/resortslite \
  resortslite=123456789012.dkr.ecr.us-east-1.amazonaws.com/resortslite:v2.0 \
  -n resortslite
```

#### Monitor Rollout
```bash
kubectl rollout status deployment/resortslite -n resortslite
```

#### Pause Rollout
```bash
kubectl rollout pause deployment/resortslite -n resortslite
```

#### Resume Rollout
```bash
kubectl rollout resume deployment/resortslite -n resortslite
```

### Rollback

#### Rollback to Previous Version
```bash
kubectl rollout undo deployment/resortslite -n resortslite
```

#### Rollback to Specific Revision
```bash
kubectl rollout undo deployment/resortslite --to-revision=2 -n resortslite
```

#### View Rollout History
```bash
kubectl rollout history deployment/resortslite -n resortslite
```

### Resource Management

#### Update Resource Limits
Edit `kubernetes/deployment.yaml`:
```yaml
resources:
  requests:
    cpu: "500m"
    memory: "1Gi"
  limits:
    cpu: "1000m"
    memory: "2Gi"
```

Apply changes:
```bash
kubectl apply -f kubernetes/deployment.yaml
```

### Cluster Autoscaling

#### Enable Cluster Autoscaler
```bash
eksctl create iamserviceaccount \
  --cluster=resortslite-cluster \
  --namespace=kube-system \
  --name=cluster-autoscaler \
  --attach-policy-arn=arn:aws:iam::aws:policy/AutoScalingFullAccess \
  --approve

kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/cluster-autoscaler/cloudprovider/aws/examples/cluster-autoscaler-autodiscover.yaml
```

---

## Java-Specific Considerations

### JVM Memory Configuration

The application uses the following JVM options:
```
-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
```

**Explanation**:
- `-Xmx512m`: Maximum heap size 512MB
- `-Xms256m`: Initial heap size 256MB
- `-XX:+UseContainerSupport`: Enable container awareness
- `-XX:MaxRAMPercentage=75.0`: Use 75% of container memory

**Tuning Recommendations**:
- For production, adjust based on actual memory usage
- Monitor with `kubectl top pods -n resortslite`
- Consider using `-XX:+UseG1GC` for better garbage collection

### Spring Boot Profiles

The application supports multiple Spring profiles:
- `default`: Local development
- `docker`: Docker Compose deployment
- `production`: Kubernetes/EKS deployment

Activate profile via environment variable:
```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "production"
```

### Graceful Shutdown

Spring Boot handles graceful shutdown automatically. Kubernetes sends SIGTERM, and Spring Boot:
1. Stops accepting new requests
2. Completes in-flight requests
3. Closes connections
4. Shuts down

Configure timeout in `application.properties`:
```properties
spring.lifecycle.timeout-per-shutdown-phase=30s
```

---

## Additional Resources

### Documentation
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/)
- [AWS EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Documentation](https://docs.docker.com/)

### Tools
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [eksctl Documentation](https://eksctl.io/)
- [AWS CLI Reference](https://docs.aws.amazon.com/cli/)

### Support
For issues or questions:
1. Check application logs: `kubectl logs -n resortslite -l app=resortslite`
2. Review Kubernetes events: `kubectl get events -n resortslite`
3. Consult AWS EKS troubleshooting guide
4. Contact your DevOps team

---

## Appendix

### Complete Deployment Checklist

- [ ] Prerequisites installed (Docker, AWS CLI, kubectl, eksctl)
- [ ] AWS credentials configured
- [ ] EKS cluster created and accessible
- [ ] AWS Load Balancer Controller installed
- [ ] Docker image built and pushed to ECR
- [ ] Kubernetes manifests updated with image URI
- [ ] Environment variables configured
- [ ] Secrets created for sensitive data
- [ ] Application deployed to EKS
- [ ] Pods running and healthy
- [ ] Service accessible via ingress
- [ ] Health checks passing
- [ ] Monitoring configured
- [ ] Backup and disaster recovery plan in place

### Quick Reference Commands

```bash
# Build and push
./scripts/build-push.sh

# Deploy to EKS
./scripts/deploy-image.sh

# Check status
kubectl get all -n resortslite

# View logs
kubectl logs -n resortslite -l app=resortslite -f

# Scale
kubectl scale deployment/resortslite --replicas=3 -n resortslite

# Rollback
kubectl rollout undo deployment/resortslite -n resortslite

# Delete deployment
kubectl delete namespace resortslite
```

---

**Document Version**: 1.0  
**Last Updated**: 2024  
**Maintained By**: DevOps Team
