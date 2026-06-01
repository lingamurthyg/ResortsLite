#!/bin/bash
set -e
set -o pipefail

# ============================================
# AWS EKS Deployment Script for ResortsLite
# ============================================

echo "=========================================="
echo "AWS EKS Deployment Script"
echo "=========================================="
echo ""

# Prompt for AWS EKS configuration
read -p "Enter AWS Region (e.g., us-east-1): " AWS_REGION
read -p "Enter EKS Cluster Name: " CLUSTER_NAME
read -p "Enter Docker Image URI (with tag): " IMAGE_URI

echo ""
echo "=== Application Configuration ==="
echo "Please provide values for environment variables (press Enter to use defaults)"
echo ""

# Database Configuration
read -p "Enter DB_URL (default: jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1): " DB_URL
DB_URL=${DB_URL:-jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1}

read -p "Enter DB_USERNAME (default: sa): " DB_USERNAME
DB_USERNAME=${DB_USERNAME:-sa}

read -sp "Enter DB_PASSWORD (default: empty): " DB_PASSWORD
echo ""
DB_PASSWORD=${DB_PASSWORD:-}

# External Service Endpoints
read -p "Enter PAYMENT_ENDPOINT (default: http://payment-svc.internal:9090/charge): " PAYMENT_ENDPOINT
PAYMENT_ENDPOINT=${PAYMENT_ENDPOINT:-http://payment-svc.internal:9090/charge}

read -p "Enter INVENTORY_ENDPOINT (default: http://inventory-svc.internal:8081/rooms): " INVENTORY_ENDPOINT
INVENTORY_ENDPOINT=${INVENTORY_ENDPOINT:-http://inventory-svc.internal:8081/rooms}

read -p "Enter NOTIFICATION_ENDPOINT (default: http://notify.internal:7070/send): " NOTIFICATION_ENDPOINT
NOTIFICATION_ENDPOINT=${NOTIFICATION_ENDPOINT:-http://notify.internal:7070/send}

# Redis Configuration
read -p "Enter REDIS_HOST (default: localhost): " REDIS_HOST
REDIS_HOST=${REDIS_HOST:-localhost}

read -p "Enter REDIS_PORT (default: 6379): " REDIS_PORT
REDIS_PORT=${REDIS_PORT:-6379}

read -sp "Enter REDIS_PASSWORD (default: empty): " REDIS_PASSWORD
echo ""
REDIS_PASSWORD=${REDIS_PASSWORD:-}

# AWS S3 Configuration
read -p "Enter S3_BUCKET_NAME (default: resortslite-reports): " S3_BUCKET_NAME
S3_BUCKET_NAME=${S3_BUCKET_NAME:-resortslite-reports}

read -p "Enter AWS_REGION for S3 (default: $AWS_REGION): " S3_AWS_REGION
S3_AWS_REGION=${S3_AWS_REGION:-$AWS_REGION}

echo ""
echo "=========================================="
echo "Configuring kubectl for EKS"
echo "=========================================="
echo ""

# Configure kubectl to use EKS cluster
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to configure kubectl for EKS cluster"
    exit 1
fi

# Verify cluster connectivity
echo "Verifying cluster connectivity..."
kubectl cluster-info

if [ $? -ne 0 ]; then
    echo "ERROR: Cannot connect to Kubernetes cluster"
    exit 1
fi

echo ""
echo "=========================================="
echo "Updating Kubernetes Manifests"
echo "=========================================="
echo ""

# Create temporary directory for processed manifests
TEMP_DIR=$(mktemp -d)
cp -r kubernetes/* "$TEMP_DIR/"

# Update deployment.yaml with actual values
sed -i "s|{{IMAGE_URI}}|$IMAGE_URI|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_URL}}|$DB_URL|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_USERNAME}}|$DB_USERNAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{DB_PASSWORD}}|$DB_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{PAYMENT_ENDPOINT}}|$PAYMENT_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{INVENTORY_ENDPOINT}}|$INVENTORY_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{NOTIFICATION_ENDPOINT}}|$NOTIFICATION_ENDPOINT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_HOST}}|$REDIS_HOST|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PORT}}|$REDIS_PORT|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{REDIS_PASSWORD}}|$REDIS_PASSWORD|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{S3_BUCKET_NAME}}|$S3_BUCKET_NAME|g" "$TEMP_DIR/deployment.yaml"
sed -i "s|{{AWS_REGION}}|$S3_AWS_REGION|g" "$TEMP_DIR/deployment.yaml"

echo "Manifests updated successfully"

echo ""
echo "=========================================="
echo "Deploying to AWS EKS"
echo "=========================================="
echo ""

# Apply Kubernetes manifests in order
echo "Creating namespace..."
kubectl apply -f "$TEMP_DIR/namespace.yaml"

echo ""
echo "Deploying application..."
kubectl apply -f "$TEMP_DIR/deployment.yaml"

echo ""
echo "Creating service..."
kubectl apply -f "$TEMP_DIR/service.yaml"

echo ""
echo "Creating ingress..."
kubectl apply -f "$TEMP_DIR/ingress.yaml"

echo ""
echo "=========================================="
echo "Waiting for Deployment Rollout"
echo "=========================================="
echo ""

# Wait for deployment to complete
kubectl rollout status deployment/resortslite -n resortslite --timeout=5m

if [ $? -ne 0 ]; then
    echo "ERROR: Deployment rollout failed"
    echo ""
    echo "Checking pod status..."
    kubectl get pods -n resortslite
    echo ""
    echo "Checking pod logs..."
    kubectl logs -n resortslite -l app=resortslite --tail=50
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo ""
echo "=========================================="
echo "Deployment Status"
echo "=========================================="
echo ""

# Display deployment status
kubectl get pods,svc,ingress -n resortslite

echo ""
echo "=========================================="
echo "Deployment Completed Successfully!"
echo "=========================================="
echo ""

# Get ingress URL
INGRESS_URL=$(kubectl get ingress resortslite-ingress -n resortslite -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "Pending...")

echo "Application Details:"
echo "  Namespace: resortslite"
echo "  Deployment: resortslite"
echo "  Service: resortslite-service"
echo "  Ingress URL: $INGRESS_URL"
echo ""
echo "Health Check: http://$INGRESS_URL/actuator/health"
echo ""
echo "To view logs:"
echo "  kubectl logs -n resortslite -l app=resortslite -f"
echo ""
echo "To scale deployment:"
echo "  kubectl scale deployment/resortslite -n resortslite --replicas=3"
echo ""
echo "To rollback deployment:"
echo "  kubectl rollout undo deployment/resortslite -n resortslite"
echo ""

# Cleanup temporary directory
rm -rf "$TEMP_DIR"
