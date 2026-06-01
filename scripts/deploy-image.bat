@echo off
setlocal enabledelayedexpansion

REM ============================================
REM AWS EKS Deployment Script for ResortsLite
REM ============================================

echo ==========================================
echo AWS EKS Deployment Script
echo ==========================================
echo.

REM Prompt for AWS EKS configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS Cluster Name: "
set /p IMAGE_URI="Enter Docker Image URI (with tag): "

echo.
echo === Application Configuration ===
echo Please provide values for environment variables (press Enter to use defaults)
echo.

REM Database Configuration
set /p DB_URL="Enter DB_URL (default: jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1): "
if "!DB_URL!"=="" set DB_URL=jdbc:h2:mem:resortdb;DB_CLOSE_DELAY=-1

set /p DB_USERNAME="Enter DB_USERNAME (default: sa): "
if "!DB_USERNAME!"=="" set DB_USERNAME=sa

set /p DB_PASSWORD="Enter DB_PASSWORD (default: empty): "
if "!DB_PASSWORD!"=="" set DB_PASSWORD=

REM External Service Endpoints
set /p PAYMENT_ENDPOINT="Enter PAYMENT_ENDPOINT (default: http://payment-svc.internal:9090/charge): "
if "!PAYMENT_ENDPOINT!"=="" set PAYMENT_ENDPOINT=http://payment-svc.internal:9090/charge

set /p INVENTORY_ENDPOINT="Enter INVENTORY_ENDPOINT (default: http://inventory-svc.internal:8081/rooms): "
if "!INVENTORY_ENDPOINT!"=="" set INVENTORY_ENDPOINT=http://inventory-svc.internal:8081/rooms

set /p NOTIFICATION_ENDPOINT="Enter NOTIFICATION_ENDPOINT (default: http://notify.internal:7070/send): "
if "!NOTIFICATION_ENDPOINT!"=="" set NOTIFICATION_ENDPOINT=http://notify.internal:7070/send

REM Redis Configuration
set /p REDIS_HOST="Enter REDIS_HOST (default: localhost): "
if "!REDIS_HOST!"=="" set REDIS_HOST=localhost

set /p REDIS_PORT="Enter REDIS_PORT (default: 6379): "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p REDIS_PASSWORD="Enter REDIS_PASSWORD (default: empty): "
if "!REDIS_PASSWORD!"=="" set REDIS_PASSWORD=

REM AWS S3 Configuration
set /p S3_BUCKET_NAME="Enter S3_BUCKET_NAME (default: resortslite-reports): "
if "!S3_BUCKET_NAME!"=="" set S3_BUCKET_NAME=resortslite-reports

set /p S3_AWS_REGION="Enter AWS_REGION for S3 (default: %AWS_REGION%): "
if "!S3_AWS_REGION!"=="" set S3_AWS_REGION=!AWS_REGION!

echo.
echo ==========================================
echo Configuring kubectl for EKS
echo ==========================================
echo.

REM Configure kubectl to use EKS cluster
aws eks update-kubeconfig --region "!AWS_REGION!" --name "!CLUSTER_NAME!"

if !ERRORLEVEL! neq 0 (
    echo ERROR: Failed to configure kubectl for EKS cluster
    exit /b 1
)

REM Verify cluster connectivity
echo Verifying cluster connectivity...
kubectl cluster-info

if !ERRORLEVEL! neq 0 (
    echo ERROR: Cannot connect to Kubernetes cluster
    exit /b 1
)

echo.
echo ==========================================
echo Updating Kubernetes Manifests
echo ==========================================
echo.

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\k8s-deploy-%RANDOM%
mkdir "!TEMP_DIR!"
xcopy /E /I /Q kubernetes "!TEMP_DIR!" >nul

REM Update deployment.yaml with actual values using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_URL}}', '!DB_URL!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_USERNAME}}', '!DB_USERNAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{DB_PASSWORD}}', '!DB_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{PAYMENT_ENDPOINT}}', '!PAYMENT_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{INVENTORY_ENDPOINT}}', '!INVENTORY_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{NOTIFICATION_ENDPOINT}}', '!NOTIFICATION_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{S3_BUCKET_NAME}}', '!S3_BUCKET_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{AWS_REGION}}', '!S3_AWS_REGION!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Manifests updated successfully

echo.
echo ==========================================
echo Deploying to AWS EKS
echo ==========================================
echo.

REM Apply Kubernetes manifests in order
echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

echo.
echo Deploying application...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

echo.
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

echo.
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

echo.
echo ==========================================
echo Waiting for Deployment Rollout
echo ==========================================
echo.

REM Wait for deployment to complete
kubectl rollout status deployment/resortslite -n resortslite --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo ERROR: Deployment rollout failed
    echo.
    echo Checking pod status...
    kubectl get pods -n resortslite
    echo.
    echo Checking pod logs...
    kubectl logs -n resortslite -l app=resortslite --tail=50
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo.
echo ==========================================
echo Deployment Status
echo ==========================================
echo.

REM Display deployment status
kubectl get pods,svc,ingress -n resortslite

echo.
echo ==========================================
echo Deployment Completed Successfully!
echo ==========================================
echo.

REM Get ingress URL
for /f "delims=" %%i in ('kubectl get ingress resortslite-ingress -n resortslite -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_URL=%%i
if "!INGRESS_URL!"=="" set INGRESS_URL=Pending...

echo Application Details:
echo   Namespace: resortslite
echo   Deployment: resortslite
echo   Service: resortslite-service
echo   Ingress URL: !INGRESS_URL!
echo.
echo Health Check: http://!INGRESS_URL!/actuator/health
echo.
echo To view logs:
echo   kubectl logs -n resortslite -l app=resortslite -f
echo.
echo To scale deployment:
echo   kubectl scale deployment/resortslite -n resortslite --replicas=3
echo.
echo To rollback deployment:
echo   kubectl rollout undo deployment/resortslite -n resortslite
echo.

REM Cleanup temporary directory
rmdir /S /Q "!TEMP_DIR!"
