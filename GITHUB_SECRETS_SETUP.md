# GitHub Secrets Setup Guide

This guide explains how to configure GitHub Secrets so the CI/CD pipelines can access database credentials.

## Required Secrets

The pipelines expect these secrets to be configured in your GitHub repository:

| Secret Name | Purpose | Example Value |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Database connection string | `jdbc:sqlserver://your-db-server:1433;databaseName=YourDB;encrypt=true;trustServerCertificate=false` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `YourStrong@Password123` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | `my-secret-key-for-jwt-token-generation-min-256-bits-long-string` |

## Setup Steps

### 1. Navigate to Repository Settings

Go to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**

### 2. Add Each Secret

Click **New repository secret** and add:

#### Secret 1: SPRING_DATASOURCE_URL
- **Name:** `SPRING_DATASOURCE_URL`
- **Value:** Your database connection string
  - **For MS SQL Server:** 
    ```
    jdbc:sqlserver://your-mssql-server.database.windows.net:1433;databaseName=YourDB;encrypt=true;trustServerCertificate=false
    ```
  - **For local MS SQL (Docker):**
    ```
    jdbc:sqlserver://localhost:1433;databaseName=SensitiveWordsDB;encrypt=false;trustServerCertificate=true
    ```
  - **For H2 (development/testing):**
    ```
    jdbc:h2:mem:testdb
    ```

#### Secret 2: SPRING_DATASOURCE_USERNAME
- **Name:** `SPRING_DATASOURCE_USERNAME`
- **Value:** `sa` (or your database user)

#### Secret 3: SPRING_DATASOURCE_PASSWORD
- **Name:** `SPRING_DATASOURCE_PASSWORD`
- **Value:** Your database password (e.g., `YourStrong@Password123`)

#### Secret 4: JWT_SECRET
- **Name:** `JWT_SECRET`
- **Value:** A secure random string (minimum 256 bits / 32 characters)
  ```
  my-secret-key-for-jwt-token-generation-min-256-bits-long-secure-string
  ```
  Or generate one:
  ```bash
  # On Linux/Mac
  openssl rand -base64 32
  
  # On Windows PowerShell
  [System.Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
  ```

### 3. Verify Secrets Are Set

After adding all secrets, you should see them listed in **Settings → Secrets and variables → Actions**:

```
✓ SPRING_DATASOURCE_URL
✓ SPRING_DATASOURCE_USERNAME
✓ SPRING_DATASOURCE_PASSWORD
✓ JWT_SECRET
```

## How Pipelines Use Secrets

The workflows reference these secrets:

**build.yml** (lines 26-29, 34-37):
```yaml
env:
  SPRING_DATASOURCE_URL: ${{ secrets.SPRING_DATASOURCE_URL }}
  SPRING_DATASOURCE_USERNAME: ${{ secrets.SPRING_DATASOURCE_USERNAME }}
  SPRING_DATASOURCE_PASSWORD: ${{ secrets.SPRING_DATASOURCE_PASSWORD }}
  JWT_SECRET: ${{ secrets.JWT_SECRET }}
```

**quality.yml** (lines 26-29, 34-37):
```yaml
env:
  SPRING_DATASOURCE_URL: ${{ secrets.SPRING_DATASOURCE_URL }}
  SPRING_DATASOURCE_USERNAME: ${{ secrets.SPRING_DATASOURCE_USERNAME }}
  SPRING_DATASOURCE_PASSWORD: ${{ secrets.SPRING_DATASOURCE_PASSWORD }}
  JWT_SECRET: ${{ secrets.JWT_SECRET }}
```

At runtime:
1. Pipeline starts → reads secrets from GitHub
2. Secrets passed as environment variables to Maven
3. Spring Boot reads env vars → overrides application.yml defaults
4. Database connects with provided credentials

## Database Options

### Option 1: Azure SQL Database (Production)
```
SPRING_DATASOURCE_URL = jdbc:sqlserver://your-server.database.windows.net:1433;databaseName=YourDB;encrypt=true;trustServerCertificate=false
SPRING_DATASOURCE_USERNAME = your-username@your-server
SPRING_DATASOURCE_PASSWORD = YourAzurePassword123!
```

### Option 2: Local MS SQL (Docker for CI)
```
SPRING_DATASOURCE_URL = jdbc:sqlserver://localhost:1433;databaseName=SensitiveWordsDB;encrypt=false;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME = sa
SPRING_DATASOURCE_PASSWORD = YourStrong@Password123
```

### Option 3: H2 In-Memory (Fastest for Tests)
```
SPRING_DATASOURCE_URL = jdbc:h2:mem:testdb
SPRING_DATASOURCE_USERNAME = sa
SPRING_DATASOURCE_PASSWORD = (leave empty)
```

**Recommendation:** Use H2 for CI pipelines (no external dependency, fastest tests)

## Testing Secrets Setup

After adding secrets, push a test commit:

```bash
git commit --allow-empty -m "Test: CI/CD pipeline secrets setup"
git push origin master
```

Check the pipeline run:
1. Go to **Actions** tab
2. Click the latest workflow run
3. Expand **Build with Maven** step
4. Confirm no "secret not found" errors

If secrets work correctly, you'll see:
```
✓ Building...
✓ Running tests...
```

If secrets are missing, you'll see:
```
ERROR: could not initialize database
ERROR: Could not connect to datasource
```

## Troubleshooting

### "Secret not found" error
- Verify secret names match exactly (case-sensitive)
- Check no leading/trailing spaces in secret values
- Ensure you're editing the right repository (not a fork)

### "Database connection failed"
- Verify connection string is correct for your database
- Check username and password are correct
- Ensure firewall allows connection to database
- For MS SQL: verify `encrypt=false` if using local SQL instance

### Pipeline still uses H2
- If SPRING_DATASOURCE_URL secret is not set, it defaults to H2
- Add the secret to force MS SQL connection

## Security Notes

- Secrets are **encrypted** at rest on GitHub
- Secrets are **masked** in logs (won't print values)
- Secrets are only available to workflows in the default branch
- Secrets cannot be accessed by forks or pull requests from forks
- Rotate secrets regularly, especially passwords

## Next Steps

1. Add all 4 secrets to your GitHub repository
2. Push a test commit to trigger the pipeline
3. Verify the pipeline uses your database in Actions tab
4. Update `.env` for local development matching the secret values (if applicable)

For more details on GitHub Secrets, see: https://docs.github.com/en/actions/security-guides/using-secrets-in-github-actions
