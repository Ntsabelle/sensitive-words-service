# Docker Setup

This document describes how to run the application using Docker and Docker Compose with MS SQL Server.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose installed (included with Docker Desktop)
- 2GB RAM available for containers

## Quick Start

### 1. Build and Run with Docker Compose

```bash
cd sensitive-words-service
docker-compose up -d
```

This will:
- Start MS SQL Server 2022 on port 1433
- Start Spring Boot application on port 8080
- Create a network for communication between containers
- Create a volume for persistent database data

### 2. Verify Services are Running

```bash
docker-compose ps
```

Expected output:
```
CONTAINER ID   IMAGE                                   PORTS                    STATUS
xxx            sensitive-words-service_app             0.0.0.0:8080->8080/tcp  Up 20 seconds
yyy            mcr.microsoft.com/mssql/server:2022     0.0.0.0:1433->1433/tcp  Up 30 seconds
```

### 3. Test Application

```bash
# Health check
curl http://localhost:8080/actuator/health

# API documentation
open http://localhost:8080/swagger-ui.html
```

## Configuration

### Environment Variables

Edit `docker-compose.yml` to change:

```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:sqlserver://mssql:1433;databaseName=SensitiveWordsDB
  SPRING_DATASOURCE_USERNAME: sa
  SPRING_DATASOURCE_PASSWORD: YourStrong@Password123
  JWT_SECRET: your-secret-key
```

### Database Password

Change MS SQL password in both places:
1. `mssql.environment.SA_PASSWORD`
2. `app.environment.SPRING_DATASOURCE_PASSWORD`

**Important**: Use strong passwords in production (min 8 chars, uppercase, numbers, special chars)

## Common Commands

### Stop Services

```bash
docker-compose down
```

### Stop and Remove Data

```bash
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs

# Application only
docker-compose logs app

# Database only
docker-compose logs mssql

# Follow logs in real-time
docker-compose logs -f app
```

### Rebuild Application Image

```bash
docker-compose build --no-cache
docker-compose up -d
```

### Connect to Database

```bash
docker exec -it sensitive-words-mssql /opt/mssql-tools/bin/sqlcmd \
  -S localhost -U sa -P YourStrong@Password123
```

## Troubleshooting

### Application fails to connect to database

Check if MS SQL is healthy:
```bash
docker-compose logs mssql
```

Wait for the healthcheck to pass (usually 40-50 seconds after startup).

### Port already in use

Change ports in `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Change 8081 to desired port
  - "1434:1433"  # Change 1434 to desired port
```

### Database data persists after restart

Data is stored in named volume `mssql_data`. To remove it:
```bash
docker-compose down -v
```

### Out of memory errors

Increase Docker memory limits:
- Docker Desktop > Settings > Resources > Memory (increase to 4GB+)

## Docker Build Manually

Build application image without compose:

```bash
docker build -t sensitive-words-service:latest .
```

Run with custom network:

```bash
# Create network
docker network create sensitive-words-net

# Start MS SQL
docker run -d \
  --name mssql \
  --network sensitive-words-net \
  -e ACCEPT_EULA=Y \
  -e SA_PASSWORD=YourStrong@Password123 \
  -p 1433:1433 \
  mcr.microsoft.com/mssql/server:2022-latest

# Start application
docker run -d \
  --name app \
  --network sensitive-words-net \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:sqlserver://mssql:1433;databaseName=SensitiveWordsDB;encrypt=false;trustServerCertificate=true" \
  -e SPRING_DATASOURCE_USERNAME=sa \
  -e SPRING_DATASOURCE_PASSWORD=YourStrong@Password123 \
  -e JWT_SECRET="my-secret-key-for-jwt-token-generation-min-256-bits" \
  sensitive-words-service:latest
```

## Production Deployment

For production, use:

1. **Docker Swarm** - Native Docker clustering
2. **Kubernetes** - Container orchestration
3. **Cloud Services** - AWS ECS, Azure Container Instances, GCP Cloud Run

Key considerations:
- Use managed database service instead of containerized DB
- Store secrets in cloud secret manager
- Enable logging and monitoring
- Use health checks
- Set resource limits (CPU, memory)
- Use auto-scaling policies

## Performance Tuning

### MS SQL Configuration

```yaml
mssql:
  environment:
    MSSQL_MEMORY_LIMIT_MB: 2048  # Limit to 2GB
```

### Application JVM Tuning

```dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
```

## Persistence

Database data persists in Docker volume:
- Volume name: `sensitive-words-service_mssql_data`
- Location: Docker's data directory

Backup database:
```bash
docker exec sensitive-words-mssql /opt/mssql-tools/bin/sqlcmd \
  -S localhost -U sa -P YourStrong@Password123 \
  -Q "BACKUP DATABASE SensitiveWordsDB TO DISK='/var/opt/mssql/backup/db.bak'"
```
