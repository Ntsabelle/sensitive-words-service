# Secrets Management

This document describes how to set up database credentials and sensitive configuration for different environments.

## GitHub Secrets (CI/CD)

Configure these secrets in GitHub repository settings: **Settings > Secrets and variables > Actions**

Required secrets for CI/CD pipelines:

```
SPRING_DATASOURCE_URL          - Database connection URL
SPRING_DATASOURCE_USERNAME     - Database username
SPRING_DATASOURCE_PASSWORD     - Database password
JWT_SECRET                     - JWT signing key (min 256 bits)
```

### Example Setup (GitHub Actions)

1. Go to repository Settings
2. Click "Secrets and variables" > "Actions"
3. Click "New repository secret" for each:

```
Name: SPRING_DATASOURCE_URL
Value: jdbc:sqlserver://db-server:1433;databaseName=SensitiveWordsDB

Name: SPRING_DATASOURCE_USERNAME
Value: sa

Name: SPRING_DATASOURCE_PASSWORD
Value: your-secure-password

Name: JWT_SECRET
Value: your-256-bit-secret-key-minimum-32-characters-long
```

## Local Development

Use environment variables for local development:

### Linux/Mac
```bash
export SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
export SPRING_DATASOURCE_USERNAME=sa
export SPRING_DATASOURCE_PASSWORD=
export JWT_SECRET=my-dev-secret-key-min-256-bits

./mvnw spring-boot:run
```

### Windows (PowerShell)
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:h2:mem:testdb"
$env:SPRING_DATASOURCE_USERNAME="sa"
$env:SPRING_DATASOURCE_PASSWORD=""
$env:JWT_SECRET="my-dev-secret-key-min-256-bits"

.\mvnw spring-boot:run
```

### Windows (Command Prompt)
```cmd
set SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb
set SPRING_DATASOURCE_USERNAME=sa
set SPRING_DATASOURCE_PASSWORD=
set JWT_SECRET=my-dev-secret-key-min-256-bits

mvnw spring-boot:run
```

## Production Deployment

For production, use one of these approaches:

### Option 1: Environment Variables
Set environment variables in your deployment environment:
- Docker: Use `-e` flag or `.env` file
- Kubernetes: Use Secrets resource
- Cloud (AWS/Azure/GCP): Use managed secrets service

### Option 2: External Secrets Manager
- AWS Secrets Manager
- Azure Key Vault
- HashiCorp Vault

### Option 3: Configuration Server
- Spring Cloud Config
- Consul
- etcd

## Security Best Practices

1. **Never commit secrets** - `.gitignore` protects most files, but double-check
2. **Rotate credentials regularly** - Change DB passwords and JWT keys periodically
3. **Use strong passwords** - Minimum 20 characters with mixed case, numbers, symbols
4. **JWT Secret minimum** - Must be at least 256 bits (32 characters base64)
5. **Audit access** - Monitor who accesses secrets in GitHub/vault
6. **Use per-environment secrets** - Different credentials for dev/staging/production

## Verification

Test that secrets are properly loaded:

```bash
# Dev environment - H2 database (no credentials needed)
./mvnw test

# Production-like with environment variables
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost JWT_SECRET=test-key ./mvnw test
```

If secrets are missing, application will fail to start with clear error messages.
