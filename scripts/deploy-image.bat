@echo off
setlocal enabledelayedexpansion

REM Deploy ResortsLite to AWS EKS (Windows)
REM This script configures kubectl and deploys the application to EKS

echo ========================================
echo ResortsLite - AWS EKS Deployment
echo ========================================
echo.

REM Prompt for AWS EKS configuration
echo AWS EKS Configuration
set /p AWS_REGION="Enter AWS Region (e.g., us-east-1): "
set /p CLUSTER_NAME="Enter EKS Cluster Name: "

if "!AWS_REGION!"=="" (
    echo AWS Region is required
    exit /b 1
)
if "!CLUSTER_NAME!"=="" (
    echo Cluster Name is required
    exit /b 1
)

REM Prompt for Docker image URI
echo.
echo Docker Image Configuration
set /p IMAGE_URI="Enter Docker Image URI (e.g., 123456789.dkr.ecr.us-east-1.amazonaws.com/resortslite:latest): "

if "!IMAGE_URI!"=="" (
    echo Docker Image URI is required
    exit /b 1
)

REM Prompt for environment-specific configuration
echo.
echo Application Configuration
echo (Press Enter to use default values)
echo.

set /p REDIS_HOST="Enter Redis Host [redis.example.com]: "
if "!REDIS_HOST!"=="" set REDIS_HOST=redis.example.com

set /p REDIS_PORT="Enter Redis Port [6379]: "
if "!REDIS_PORT!"=="" set REDIS_PORT=6379

set /p REDIS_PASSWORD="Enter Redis Password (optional): "

set /p S3_BUCKET_NAME="Enter S3 Bucket Name [resorts-lite-files]: "
if "!S3_BUCKET_NAME!"=="" set S3_BUCKET_NAME=resorts-lite-files

set /p S3_AWS_REGION="Enter AWS Region for S3 [!AWS_REGION!]: "
if "!S3_AWS_REGION!"=="" set S3_AWS_REGION=!AWS_REGION!

set /p APP_PAYMENT_ENDPOINT="Enter Payment Service Endpoint [http://payment-svc:9090/charge]: "
if "!APP_PAYMENT_ENDPOINT!"=="" set APP_PAYMENT_ENDPOINT=http://payment-svc:9090/charge

set /p APP_INVENTORY_ENDPOINT="Enter Inventory Service Endpoint [http://inventory-svc:8081/rooms]: "
if "!APP_INVENTORY_ENDPOINT!"=="" set APP_INVENTORY_ENDPOINT=http://inventory-svc:8081/rooms

set /p APP_NOTIFICATION_ENDPOINT="Enter Notification Service Endpoint [http://notify-svc:7070/send]: "
if "!APP_NOTIFICATION_ENDPOINT!"=="" set APP_NOTIFICATION_ENDPOINT=http://notify-svc:7070/send

REM Configure kubectl for EKS
echo.
echo Configuring kubectl for EKS cluster...
aws eks update-kubeconfig --region "!AWS_REGION!" --name "!CLUSTER_NAME!"

if !ERRORLEVEL! neq 0 (
    echo Failed to configure kubectl. Please check your AWS credentials and cluster name.
    exit /b 1
)

echo kubectl configured successfully

REM Verify cluster connectivity
echo.
echo Verifying cluster connectivity...
kubectl cluster-info
if !ERRORLEVEL! neq 0 (
    echo Failed to connect to cluster
    exit /b 1
)

echo Cluster connectivity verified

REM Update Kubernetes manifests with configuration values
echo.
echo Updating Kubernetes manifests...

REM Create temporary directory for processed manifests
set TEMP_DIR=%TEMP%\resortslite-deploy-%RANDOM%
mkdir "!TEMP_DIR!"
xcopy /E /I /Q kubernetes "!TEMP_DIR!" >nul

REM Replace placeholders in deployment.yaml using PowerShell
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{IMAGE_URI}}', '!IMAGE_URI!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_HOST}}', '!REDIS_HOST!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PORT}}', '!REDIS_PORT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{REDIS_PASSWORD}}', '!REDIS_PASSWORD!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{S3_BUCKET_NAME}}', '!S3_BUCKET_NAME!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{AWS_REGION}}', '!S3_AWS_REGION!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{APP_PAYMENT_ENDPOINT}}', '!APP_PAYMENT_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{APP_INVENTORY_ENDPOINT}}', '!APP_INVENTORY_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"
powershell -Command "(Get-Content '!TEMP_DIR!\deployment.yaml') -replace '{{APP_NOTIFICATION_ENDPOINT}}', '!APP_NOTIFICATION_ENDPOINT!' | Set-Content '!TEMP_DIR!\deployment.yaml'"

echo Manifests updated successfully

REM Apply Kubernetes manifests
echo.
echo Deploying to EKS...

echo Creating namespace...
kubectl apply -f "!TEMP_DIR!\namespace.yaml"

echo.
echo Creating deployment...
kubectl apply -f "!TEMP_DIR!\deployment.yaml"

echo.
echo Creating service...
kubectl apply -f "!TEMP_DIR!\service.yaml"

echo.
echo Creating ingress...
kubectl apply -f "!TEMP_DIR!\ingress.yaml"

REM Wait for deployment rollout
echo.
echo Waiting for deployment to complete...
kubectl rollout status deployment/resortslite -n resortslite --timeout=5m

if !ERRORLEVEL! neq 0 (
    echo Deployment rollout failed or timed out
    echo Checking pod status...
    kubectl get pods -n resortslite
    echo.
    echo Checking pod logs...
    kubectl logs -n resortslite -l app=resortslite --tail=50
    rmdir /S /Q "!TEMP_DIR!"
    exit /b 1
)

echo Deployment completed successfully

REM Verify deployment
echo.
echo Verifying deployment...
kubectl get pods,svc,ingress -n resortslite

REM Get ingress URL
echo.
echo Retrieving application URL...
for /f "delims=" %%i in ('kubectl get ingress resortslite-ingress -n resortslite -o jsonpath^="{.status.loadBalancer.ingress[0].hostname}" 2^>nul') do set INGRESS_HOST=%%i

if not "!INGRESS_HOST!"=="" (
    echo Application URL: http://!INGRESS_HOST!
    echo Note: It may take a few minutes for the Load Balancer to become fully operational
) else (
    echo Ingress is being provisioned. Run the following command to get the URL:
    echo kubectl get ingress resortslite-ingress -n resortslite
)

REM Cleanup temporary directory
rmdir /S /Q "!TEMP_DIR!"

echo.
echo ========================================
echo Deployment Completed Successfully!
echo ========================================
echo.
echo Useful Commands:
echo   View pods:        kubectl get pods -n resortslite
echo   View logs:        kubectl logs -n resortslite -l app=resortslite
echo   View services:    kubectl get svc -n resortslite
echo   View ingress:     kubectl get ingress -n resortslite
echo   Delete deployment: kubectl delete namespace resortslite
echo.

endlocal
