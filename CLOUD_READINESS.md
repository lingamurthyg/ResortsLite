# ResortsLite - Cloud-Ready Application

## Cloud Readiness Fixes Applied

This application has been transformed to be fully cloud-ready for Azure deployment. All cloud compatibility blockers have been resolved.

### Fixed Issues Summary

#### 1. File System & Local Storage Dependencies (Critical)
- **cr-java-0061**: Hard-coded file paths replaced with Azure Blob Storage
- **cr-java-0062**: Local file write operations migrated to Azure Blob Storage
- **cr-java-0063**: Java.io.File usage replaced with Azure Blob Storage SDK

**Files Modified**: `ReportService.java`
**Solution**: All file operations now use Azure Blob Storage with connection string configuration.

#### 2. Configuration Management (Critical)
- **cr-java-0069**: Hard-coded database credentials externalized to Azure Key Vault
- **cr-java-0071**: Hard-coded environment URLs externalized to Azure App Configuration
- **cr-java-0111**: Clock/time dependencies replaced with Azure Service Bus scheduled messages

**Files Modified**: `BookingService.java`, `ReportService.java`, `BookingController.java`, `application.properties`
**Solution**: All credentials and configuration externalized to Azure Key Vault and App Configuration.

#### 3. Networking & Communication (Critical)
- **cr-java-0077**: Hard-coded ports replaced with environment variables

**Files Modified**: `ReportService.java`, `application.properties`
**Solution**: Port configuration now uses `${PORT:8080}` for dynamic assignment.

#### 4. State Management & Session Issues (High)
- **cr-java-0065**: HTTP session state storage replaced with Azure Cache for Redis
- **cr-java-0067**: In-memory caching without TTL replaced with Azure Cache for Redis with TTL

**Files Modified**: `BookingController.java`
**Solution**: All session and cache operations now use distributed Redis cache with TTL policies.

#### 5. Security & Authentication (High)
- **cr-java-0090**: File-based authentication replaced with Azure Active Directory (Entra ID)

**Files Modified**: `BookingService.java`
**Solution**: Authentication now uses Azure AD with MSAL4J library.

### Azure Services Required

1. **Azure Blob Storage**: For file storage and report generation
2. **Azure Key Vault**: For secrets and credentials management
3. **Azure Cache for Redis**: For distributed caching and session management
4. **Azure Service Bus**: For distributed task scheduling
5. **Azure Active Directory**: For authentication and authorization
6. **Azure App Configuration** (Optional): For centralized configuration management

### Configuration

#### Environment Variables

Copy `.env.template` to `.env` and configure your Azure resources:

```bash
cp .env.template .env
# Edit .env with your Azure resource values
```

Required environment variables:
- `AZURE_STORAGE_CONNECTION_STRING`: Azure Storage account connection string
- `AZURE_KEYVAULT_URI`: Azure Key Vault URI
- `AZURE_REDIS_HOST`: Azure Cache for Redis hostname
- `AZURE_REDIS_PASSWORD`: Azure Cache for Redis access key
- `AZURE_SERVICEBUS_CONNECTION_STRING`: Azure Service Bus connection string
- `AZURE_AD_CLIENT_ID`: Azure AD application client ID
- `AZURE_AD_TENANT_ID`: Azure AD tenant ID

#### Azure Key Vault Secrets

Store the following secrets in Azure Key Vault:
- `azure-ad-client-secret`: Azure AD application client secret
- `db-password`: Database password (if using external database)
- Any other sensitive configuration values

### Deployment

#### Local Development

1. Configure environment variables in `.env` file
2. Ensure Azure resources are provisioned
3. Run the application:

```bash
mvn spring-boot:run
```

#### Azure App Service Deployment

1. Configure application settings in Azure Portal with environment variables
2. Enable managed identity for the App Service
3. Grant managed identity access to:
   - Azure Key Vault (Get, List secrets)
   - Azure Blob Storage (Contributor)
   - Azure Cache for Redis (Contributor)
   - Azure Service Bus (Sender)

4. Deploy using Azure CLI:

```bash
az webapp deploy --resource-group <rg-name> --name <app-name> --src-path target/resortsLite-1.0.0.jar
```

#### Azure Container Apps Deployment

1. Build container image (handled separately)
2. Configure environment variables in Container Apps
3. Enable managed identity
4. Deploy container

### Architecture Changes

#### Before (Cloud-Incompatible)
- Hard-coded file paths (`/var/legacy/reports/`)
- Local file system writes
- HTTP session state storage
- In-memory cache without TTL
- Hard-coded credentials in source code
- Hard-coded ports and URLs
- File-based authentication
- Local timers (java.util.Timer)

#### After (Cloud-Native)
- Azure Blob Storage for all file operations
- Distributed Redis cache with TTL
- Stateless architecture (no session dependencies)
- Azure Key Vault for secrets management
- Environment-based configuration
- Azure AD authentication
- Azure Service Bus for distributed scheduling
- Dynamic port assignment

### Code Quality Improvements

1. **Security**: All credentials externalized, SQL injection prevention with parameterized queries
2. **Scalability**: Stateless design enables horizontal scaling
3. **Reliability**: Distributed cache and storage prevent data loss
4. **Maintainability**: Configuration externalized for easy environment management
5. **Compliance**: Follows 12-factor app principles and cloud-native patterns

### Testing

Run tests with Azure emulators or test resources:

```bash
mvn test
```

### Monitoring

Application exposes health endpoints for monitoring:
- `/actuator/health`: Application health status
- `/actuator/info`: Application information
- `/actuator/metrics`: Application metrics

### Dependencies Added

- `azure-storage-blob`: Azure Blob Storage SDK
- `azure-security-keyvault-secrets`: Azure Key Vault SDK
- `azure-identity`: Azure authentication
- `azure-messaging-servicebus`: Azure Service Bus SDK
- `spring-boot-starter-data-redis`: Redis support
- `msal4j`: Microsoft Authentication Library
- `spring-cloud-azure-starter-appconfiguration`: Azure App Configuration

### Migration Notes

- All file paths are now stored in Azure Blob Storage
- Session data is stored in Redis (ensure Redis is configured)
- Authentication requires Azure AD setup
- Scheduled tasks use Azure Service Bus (configure queue)
- All secrets must be stored in Azure Key Vault before deployment

### Support

For issues or questions, refer to Azure documentation:
- [Azure Blob Storage](https://docs.microsoft.com/azure/storage/blobs/)
- [Azure Key Vault](https://docs.microsoft.com/azure/key-vault/)
- [Azure Cache for Redis](https://docs.microsoft.com/azure/azure-cache-for-redis/)
- [Azure Service Bus](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Azure Active Directory](https://docs.microsoft.com/azure/active-directory/)
