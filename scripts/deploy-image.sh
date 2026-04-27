#!/bin/bash

# Deploy ResortsLite to AWS EKS
# This script configures kubectl and deploys the application to EKS

set -e
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}ResortsLite - AWS EKS Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Prompt for AWS EKS configuration
echo -e "${YELLOW}AWS EKS Configuration${NC}"
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter EKS Cluster Name: " CLUSTER_NAME

if [ -z "$AWS_REGION" ] || [ -z "$CLUSTER_NAME" ]; then
    echo -e "${RED}AWS Region and Cluster Name are required${NC}"
    exit 1
fi

# Prompt for Docker image URI
echo ""
echo -e "${YELLOW}Docker Image Configuration${NC}"
read -p "Enter Docker Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest): " IMAGE_URI

if [ -z "$IMAGE_URI" ]; then
    echo -e "${RED}Docker Image URI is required${NC}"
    exit 1
fi

# Prompt for environment-specific configuration
echo ""
echo -e "${YELLOW}Application Configuration${NC}"
echo -e "${YELLOW}(Press Enter to use default values)${NC}"
echo ""

read -p "Enter Redis Host [redis.example.com]: " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-redis.example.com}

read -p "Enter Redis Port [6379]: " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -sp "Enter Redis Password (optional): " REDIS_PASSWORD
echo ""

read -p "Enter S3 Bucket Name [resorts-lite-files]: " S3_BUCKET_NAME
S3_BUCKET_NAME=${S3_BUCKET_NAME:-resorts-lite-files}

read -p "Enter AWS Region for S3 [$AWS_REGION]: " S3_AWS_REGION
S3_AWS_REGION=${S3_AWS_REGION:-$AWS_REGION}

read -p "Enter Payment Service Endpoint [http://payment-svc:9090/charge]: " APP_PAYMENT_ENDPOINT
APP_PAYMENT_ENDPOINT=${APP_PAYMENT_ENDPOINT:-http://payment-svc:9090/charge}

read -p "Enter Inventory Service Endpoint [http://inventory-svc:8081/rooms]: " APP_INVENTORY_ENDPOINT
APP_INVENTORY_ENDPOINT=${APP_INVENTORY_ENDPOINT:-http://inventory-svc:8081/rooms}

read -p "Enter Notification Service Endpoint [http://notify-svc:7070/send]: " APP_NOTIFICATION_ENDPOINT
APP_NOTIFICATION_ENDPOINT=${APP_NOTIFICATION_ENDPOINT:-http://notify-svc:7070/send}

# Configure kubectl for EKS
echo ""
echo -e "${YELLOW}Configuring kubectl for EKS cluster...${NC}"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to configure kubectl. Please check your AWS credentials and cluster name.${NC}"
    exit 1
fi

echo -e "${GREEN}kubectl configured successfully${NC}"

# Verify cluster connectivity
echo ""
echo -e "${YELLOW}Verifying cluster connectivity...${NC}"
kubectl cluster-info || {
    echo -e "${RED}Failed to connect to cluster${NC}"
    exit 1
}

echo -e "${GREEN}Cluster connectivity verified${NC}"

# Update Kubernetes manifests with configuration values
echo ""
echo -e "${YELLOW}Updating Kubernetes manifests...${NC}"

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Replace placeholders in deployment.yaml
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{S3_BUCKET_NAME}}|$S3_BUCKET_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{AWS_REGION}}|$S3_AWS_REGION|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{APP_PAYMENT_ENDPOINT}}|$APP_PAYMENT_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{APP_INVENTORY_ENDPOINT}}|$APP_INVENTORY_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{APP_NOTIFICATION_ENDPOINT}}|$APP_NOTIFICATION_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"

echo -e "${GREEN}Manifests updated successfully${NC}"

# Apply Kubernetes manifests
echo ""
echo -e "${YELLOW}Deploying to EKS...${NC}"

echo -e "${YELLOW}Creating namespace...${NC}"
kubectl apply -f "$TEMP_DIR/namespace.yaml"

echo ""
echo -e "${YELLOW}Creating deployment...${NC}"
kubectl apply -f "$TEMP_DIR/deployment.yaml"

echo ""
echo -e "${YELLOW}Creating service...${NC}"
kubectl apply -f "$TEMP_DIR/service.yaml"

echo ""
echo -e "${YELLOW}Creating ingress...${NC}"
kubectl apply -f "$TEMP_DIR/ingress.yaml"

# Wait for deployment rollout
echo ""
echo -e "${YELLOW}Waiting for deployment to complete...${NC}"
kubectl rollout status deployment/resortslite -n resortslite --timeout=5m

if [ $? -ne 0 ]; then
    echo -e "${RED}Deployment rollout failed or timed out${NC}"
    echo -e "${YELLOW}Checking pod status...${NC}"
    kubectl get pods -n resortslite
    echo ""
    echo -e "${YELLOW}Checking pod logs...${NC}"
    kubectl logs -n resortslite -l app=resortslite --tail=50
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo -e "${GREEN}Deployment completed successfully${NC}"

# Verify deployment
echo ""
echo -e "${YELLOW}Verifying deployment...${NC}"
kubectl get pods,svc,ingress -n resortslite

# Get ingress URL
echo ""
echo -e "${YELLOW}Retrieving application URL...${NC}"
INGRESS_HOST=$(kubectl get ingress resortslite-ingress -n resortslite -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null)

if [ -n "$INGRESS_HOST" ]; then
    echo -e "${GREEN}Application URL: http://$INGRESS_HOST${NC}"
    echo -e "${YELLOW}Note: It may take a few minutes for the Load Balancer to become fully operational${NC}"
else
    echo -e "${YELLOW}Ingress is being provisioned. Run the following command to get the URL:${NC}"
    echo "kubectl get ingress resortslite-ingress -n resortslite"
fi

# Cleanup temporary directory
rm -rf "$TEMP_DIR"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Completed Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${YELLOW}Useful Commands:${NC}"
echo "  View pods:        kubectl get pods -n resortslite"
echo "  View logs:        kubectl logs -n resortslite -l app=resortslite"
echo "  View services:    kubectl get svc -n resortslite"
echo "  View ingress:     kubectl get ingress -n resortslite"
echo "  Delete deployment: kubectl delete namespace resortslite"
echo ""
