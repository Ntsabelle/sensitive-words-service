# Sensitive Words Service

A Spring Boot REST API for filtering and sanitizing sensitive words in text content. Built with Java 21, JWT authentication, distributed rate limiting, and comprehensive metrics.

## Features

- Word Management - CRUD operations for sensitive word dictionary  
- Preloaded Word List - Auto-seeds the SQL-keyword sensitive word list on first startup  
- Text Sanitization - Real-time text filtering with asterisk replacement  
- JWT Authentication - Secure API with token-based auth (256-bit HS256)
- Auto-Cache Refresh - Instant pattern updates when dictionary changes  
- Case-Insensitive Matching - Catches variations (badword, BADWORD, BadWord)  
- Word Boundary Detection - Only matches complete words, not substrings  
- Performance Metrics - Monitor sanitization duration and match counts  
- Distributed Rate Limiting - Redis-backed for multi-instance deployments  
- H2/MSSQL Support - Development with H2, production with SQL Server
- Docker & Docker Compose - Full containerization with Redis + MSSQL orchestration

## Technology Stack

- **Java 21** - Latest Java features (records, virtual threads ready)
- **Spring Boot 3.3.0** - Full dependency injection and auto-configuration
- **Spring Security** - JWT token validation and CORS handling
- **Spring Data JPA** - Hibernate ORM for database operations
- **Spring Data Redis** - Distributed cache and rate limiting
- **JJWT 0.12.3** - JSON Web Token creation and validation with 256-bit secret validation
- **Micrometer** - Application metrics (Timer, Counter)
- **Bucket4j** - Token bucket rate limiting (in-memory + Redis-backed)
- **Caffeine** - Local caching for user lookups
- **H2 Database** - In-memory dev database
- **Redis** - Distributed rate limiting and state synchronization
- **Springdoc OpenAPI** - Swagger UI documentation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Installation

```bash
# Clone repository
git clone <repo-url>
cd sensitive-words-service

# Build
./mvnw clean package

# Run
java -jar target/sensitive-words-service-0.0.1-SNAPSHOT.jar
```

Service starts on **http://localhost:8080**

### Docker Setup (Production with Redis & MSSQL)

The recommended production setup uses Docker Compose to orchestrate:
- **Redis** - Distributed rate limiting across multiple instances
- **MS SQL Server** - Production-grade database
- **Spring Boot App** - Application container

```bash
# 1. Copy environment file and set secrets
cp .env.example .env
# Edit .env with your values:
#   SA_PASSWORD=YourSecurePassword
#   JWT_SECRET=<base64-encoded 256-bit key>
# SA_PASSWORD is read by both the mssql container (as its SA login) and the
# app container (as its datasource password) - keep it the same value in both.

# 2. Start all services
# The app waits for redis and mssql to report healthy (not just "started")
# before connecting, so first boot can take ~30s while SQL Server initializes.
docker-compose up -d

# 3. View logs
docker-compose logs -f app

# 4. Monitor services
docker ps  # See all running containers
redis-cli -h localhost  # Connect to Redis (if installed locally)

# 5. Stop services
docker-compose down

# 6. Full reset (delete all data)
docker-compose down -v
docker-compose up -d
```

Services:
- **App**: http://localhost:8080
- **MSSQL**: localhost:1433 (User: sa)
- **Redis**: localhost:6379 (rate limiting backend)
- **Health Check**: http://localhost:8080/actuator/health

### Single Instance (Development with H2)

For quick local development:

```bash
# Build and run locally
./mvnw clean package
java -jar target/sensitive-words-service-0.0.1-SNAPSHOT.jar
```

Service starts on **http://localhost:8080**
Database: H2 in-memory (resets on restart)

## API Endpoints

### Authentication (Public)

#### Register User
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```
**Response:** `201 Created`

#### Login
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "expiresIn": 120000
}
```

### Sensitive Words (Protected - Requires JWT)

#### List All Words
```
GET /api/v1/sensitive-words?page=0&size=20
Authorization: Bearer <token>
```

#### Create Word
```
POST /api/v1/sensitive-words
Authorization: Bearer <token>
Content-Type: application/json

{
  "word": "badword",
  "active": true
}
```
**Note:** Cache automatically refreshes after creation. `active` defaults to `true` if omitted.

#### Get Word by ID
```
GET /api/v1/sensitive-words/{id}
Authorization: Bearer <token>
```

#### Update Word
```
PUT /api/v1/sensitive-words/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "word": "newword",
  "active": false
}
```
**Note:** Cache automatically refreshes after update

#### Delete Word
```
DELETE /api/v1/sensitive-words/{id}
Authorization: Bearer <token>
```
**Note:** Cache automatically refreshes after deletion

### Sanitization (Protected - Requires JWT)

#### Sanitize Text
```
POST /api/v1/sanitize
Authorization: Bearer <token>
Content-Type: application/json

{
  "text": "This is a badword text"
}
```
**Response:**
```json
{
  "sanitizedText": "This is a ******* text",
  "matchCount": 1
}
```

### Health & Monitoring (Public)

#### Health Check
```
GET /actuator/health
```

#### Metrics
```
GET /actuator/metrics
GET /actuator/metrics/sensitive.words.sanitization.duration
GET /actuator/metrics/sanitize.words.match.total
```

#### Swagger UI
```
http://localhost:8080/swagger-ui.html
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
jwt:
  secret: my-secret-key-for-jwt-token-generation-min-256-bits
  expiration: 120000  # 2 minutes (in milliseconds)
  # For production: 86400000 (24 hours)

spring:
  datasource:
    url: jdbc:h2:mem:testdb  # Development
    # For production MSSQL:
    # url: jdbc:sqlserver://localhost\SQLEXPRESS;databaseName=SensitiveWordsDB
    
  jpa:
    hibernate:
      ddl-auto: create  # Auto-create schema
      # For production: validate
```

## How Sanitization Works

1. **Pattern Compilation** - On startup, service loads all active words from database
2. **Regex Building** - Creates case-insensitive regex: `\b(?:word1|word2|word3)\b`
3. **Text Matching** - Scans input text for word boundaries using compiled pattern
4. **Replacement** - Replaces matched words with asterisks (same length as original)
5. **Metrics** - Records sanitization time and match count

### Example Flow
```
Input:  "Hello badword and BADWORD here"
Regex:  \b(?:badword)\b (case-insensitive)
Matches: "badword" (position 6-13), "BADWORD" (position 18-25)
Output: "Hello ******* and ******* here"
Count:  2
```

## Data Seeding

On startup, `SensitiveWordSeeder` (an `ApplicationRunner`) preloads the `sensitive_words` table from `src/main/resources/seed-sensitive-words.json` — a list of SQL keywords the company has designated as sensitive (e.g. `SELECT`, `DROP`, `SELECT * FROM`).

- Seeding only runs **if the table is empty** — it never duplicates or overwrites existing words.
- **Dev (H2, `ddl-auto=create`):** the table is recreated empty on every boot, so the seed list is reloaded every restart.
- **Prod (MSSQL, `ddl-auto=validate`):** the table persists across restarts, so seeding effectively runs once — the first time the service starts against a fresh database — and is a no-op afterwards.
- The sanitization cache is refreshed automatically once seeding completes, so the API is ready to mask words immediately.

To change the preloaded list, edit `seed-sensitive-words.json` (a JSON array of strings) before first startup against a fresh database. Adding to it later won't affect an already-seeded database — use the CRUD API to add words to an existing deployment.

## Cache Refresh Strategy

The sanitization pattern is **cached in memory** for performance:
- Compiled regex pattern stored in `AtomicReference`
- Cache hash checked on each refresh
- Pattern only rebuilt if word list changes
- Automatic refresh triggered by:
  - POST `/api/v1/sensitive-words` (create)
  - PUT `/api/v1/sensitive-words/{id}` (update)
  - DELETE `/api/v1/sensitive-words/{id}` (delete)

**Performance:** ~0.5ms per sanitization on ~100 words

## Security

### JWT Token Validation
- **Algorithm:** HS256 (HMAC SHA-256)
- **Signature:** Validated on every request
- **Expiration:** Checked explicitly, default 2 minutes (configurable)
- **Clock Skew:** No tolerance (strict validation)

### Protected Endpoints
All `/api/v1/**` endpoints (except `/auth/**`) require valid JWT token:
```
Authorization: Bearer <token>
```

### Public Endpoints
- `/api/v1/auth/register` - User registration
- `/api/v1/auth/login` - Token generation
- `/swagger-ui.html` - API documentation
- `/actuator/health` - Health check

## Database Schema

### users table
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP
);
```

### sensitive_words table
```sql
CREATE TABLE sensitive_words (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word VARCHAR(255) UNIQUE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```
Preloaded on first startup from `seed-sensitive-words.json` — see [Data Seeding](#data-seeding).

## Testing

### Unit Tests
```bash
./mvnw test
```

Test coverage:
- **SensitiveWordServiceTest** - 11 tests (CRUD, validation, errors)
- **SanitizationServiceTest** - 10 tests (pattern matching, edge cases)
- **SensitiveWordControllerTest** - 9 tests (request/response handling)
- **SanitizeControllerTest** - 6 tests (sanitization API)
- **SensitiveWordRepositoryTest** - 15 tests (database operations)

**Total: 50 unit tests**

### Manual Testing with Bruno/Postman

Import: `Sensitive-Words-Service-JWT.postman_collection.json`

**Workflow:**
1. Register new user
2. Login to get token
3. Create sensitive words
4. Sanitize test text
5. Verify output

## Performance Metrics

Monitor via `/actuator/metrics`:

### Sanitization Duration
```
GET /actuator/metrics/sensitive.words.sanitization.duration
```
- Average time per sanitization call
- Unit: nanoseconds

### Match Count
```
GET /actuator/metrics/sanitize.words.match.total
```
- Cumulative count of matched sensitive words
- Increments on each positive match

## Troubleshooting

### Login Returns 400 - Invalid Credentials
- User database is empty after restart (H2 in-memory)
- Solution: Register user first via POST `/api/v1/auth/register`

### Token Expired (401 Unauthorized)
- Token expiration set to 2 minutes (default)
- Solution: Login again to get fresh token
- Configure: `jwt.expiration` in `application.yml`

### Sanitization Not Working
- The seed list (`seed-sensitive-words.json`) should auto-populate the table on first startup — check logs for `Preloaded N sensitive word(s)` to confirm seeding ran
- If the table is unexpectedly empty (e.g. seeding was skipped or the resource file is missing), add a word manually via POST `/api/v1/sensitive-words`
- Cache refreshes automatically after creation, update, deletion, and after seeding

### Port 8080 Already in Use
- Change port: `server.port: 8081` in `application.yml`
- Or kill existing process: `lsof -ti:8080 | xargs kill`

## Development Roadmap

- [x] Rate limiting on expensive endpoints
- [x] Distributed rate limiting with Redis
- [x] JWT secret validation
- [x] Production security profiles
- [x] Preload sensitive word list on startup (seed-sensitive-words.json)
- [ ] Bulk import sensitive words from CSV
- [ ] Word variants/aliases support
- [ ] Webhook notifications on pattern changes
- [ ] Audit logging for word modifications
- [ ] PostgreSQL support

## Performance Improvements

Added caching, concurrency, rate limiting, and distribution support:

- **User caching** - Login results cached for 10 minutes to avoid repeated database hits
- **Database indexes** - Added index on `User.username` column for faster lookups
- **Better concurrency** - Switched to ReadWriteLock so multiple sanitization requests can run in parallel
- **Connection pool tuning** - Configured leak detection and batch processing for more efficient database access
- **Distributed rate limiting** - Redis-backed bucket4j for multi-instance rate limit synchronization
- **ReadWrite locks** - Concurrent reads during sanitization, exclusive writes during cache refresh

## Rate Limiting (Distributed with Redis)

Expensive endpoints are rate limited to prevent abuse. Limits are **per-client (based on IP address)**.

### Single Instance (In-Memory)
Uses Bucket4j token buckets stored locally in each JVM.

### Multi-Instance (Distributed via Redis)
Rate limits are synchronized across all instances. Example with 3 app instances:

| Endpoint | Limit Per Instance | Total (3 instances) | Window |
|----------|------|------|--------|
| POST /api/v1/sanitize | 100 requests | 300 requests | 1 minute |
| POST /api/v1/auth/login | 5 attempts | 15 attempts | 1 minute |
| POST /api/v1/auth/register | 5 attempts | 15 attempts | 1 minute |
| POST /api/v1/sensitive-words | 20 operations | 60 operations | 1 minute |
| PUT /api/v1/sensitive-words/{id} | 20 operations | 60 operations | 1 minute |
| DELETE /api/v1/sensitive-words/{id} | 20 operations | 60 operations | 1 minute |

Exceeding the limit returns **HTTP 429 Too Many Requests**.

**Graceful Degradation:** If Redis unavailable, automatically falls back to in-memory rate limiting to prevent service disruption.

### Scaling Example

Deploy with 3+ app instances sharing Redis:

```bash
# Start with Docker Compose
docker-compose up -d

# Scale to 3 app instances
docker-compose up -d --scale app=3

# All instances share rate limit counters in Redis
# Client hits instance 1: 1/100 used
# Client hits instance 2: 2/100 used (enforced via Redis)
# Client hits instance 3: 3/100 used (enforced via Redis)
```

## Security (JWT & Production Hardening)

### JWT Token Validation
- **Algorithm:** HS256 (HMAC SHA-256)
- **Secret:** Minimum 256 bits (32 bytes) - **validated at startup**
- **Signature:** Validated on every request
- **Expiration:** Default 86400 seconds (24 hours, configurable)
- **Clock Skew:** Zero tolerance (strict validation)

**Setup secure JWT secret:**

```bash
# Generate 256-bit key (base64-encoded)
openssl rand -base64 32
# Example: ABC123def/xyz+123abc==

# Set environment variable
export JWT_SECRET="ABC123def/xyz+123abc=="

# Or in .env file
JWT_SECRET=ABC123def/xyz+123abc==
```

If secret is too short, service fails at startup with clear error message:
```
IllegalStateException: JWT secret key must be at least 256 bits (32 bytes)
```

### Production vs Development Profiles

**Development Profile** (`application.yml` or `-Dspring.profiles.active=dev`):
- ✅ H2 Console enabled (`/h2-console/**`)
- ✅ Swagger UI enabled (`/swagger-ui.html`)
- ✅ All actuator endpoints exposed
- ✅ SQL logging enabled
- ✅ In-memory rate limiting

**Production Profile** (`application-prod.yml` or `-Dspring.profiles.active=prod`):
- ❌ H2 Console disabled
- ❌ Swagger UI disabled
- ⚠️ Only `/actuator/health` public (requires auth for details)
- ❌ Metrics endpoint disabled
- ✅ Redis distributed rate limiting
- ✅ MSSQL database required
- ✅ Hibernate DDL set to validate (no schema changes)

### Endpoint Security

Protected Endpoints (require JWT):
```
ALL /api/v1/** except /auth/**
```

Public Endpoints:
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /actuator/health
```

Production-Only Endpoints (disabled in dev):
```
/h2-console/**              (DENIED in prod)
/swagger-ui.html            (DENIED in prod)
/v3/api-docs/**             (DENIED in prod)
/actuator/metrics           (DENIED in prod)
```

## Contributing

1. Create feature branch: `git checkout -b feature/xyz`
2. Commit changes: `git commit -am 'Add feature'`
3. Push: `git push origin feature/xyz`
4. Create pull request

## License

MIT License - See LICENSE file

## Support

For issues and questions:
- Email: tshedison929@gmail.com
- GitHub Issues: https://github.com/joseph/sensitive-words-service/issues
- Documentation: http://localhost:8080/swagger-ui.html
