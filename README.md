# Sensitive Words Service

A Spring Boot REST API for filtering and sanitizing sensitive words in text content. Built with Java 21, JWT authentication, and comprehensive metrics.

## Features

✅ **Word Management** - CRUD operations for sensitive word dictionary  
✅ **Text Sanitization** - Real-time text filtering with asterisk replacement  
✅ **JWT Authentication** - Secure API with token-based auth  
✅ **Auto-Cache Refresh** - Instant pattern updates when dictionary changes  
✅ **Case-Insensitive Matching** - Catches variations (badword, BADWORD, BadWord)  
✅ **Word Boundary Detection** - Only matches complete words, not substrings  
✅ **Performance Metrics** - Monitor sanitization duration and match counts  
✅ **H2/MSSQL Support** - Development with H2, production with SQL Server  

## Technology Stack

- **Java 21** - Latest Java features (records, virtual threads ready)
- **Spring Boot 3.3.0** - Full dependency injection and auto-configuration
- **Spring Security** - JWT token validation and CORS handling
- **Spring Data JPA** - Hibernate ORM for database operations
- **JJWT 0.12.3** - JSON Web Token creation and validation
- **Micrometer** - Application metrics (Timer, Counter)
- **H2 Database** - In-memory dev database
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
  "category": "profanity",
  "severity": "HIGH"
}
```
**Note:** Cache automatically refreshes after creation

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
  "category": "profanity",
  "severity": "MEDIUM"
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

### sensitive_word table
```sql
CREATE TABLE sensitive_word (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  word VARCHAR(255) UNIQUE NOT NULL,
  category VARCHAR(100),
  severity VARCHAR(50),
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP
);
```

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
- No words in database
- Solution: Create word via POST `/api/v1/sensitive-words`
- Cache refreshes automatically after creation

### Port 8080 Already in Use
- Change port: `server.port: 8081` in `application.yml`
- Or kill existing process: `lsof -ti:8080 | xargs kill`

## Development Roadmap

- [ ] Rate limiting on sanitize endpoint
- [ ] Bulk import sensitive words from CSV
- [ ] Word variants/aliases support
- [ ] Webhook notifications on pattern changes
- [ ] Audit logging for word modifications
- [ ] PostgreSQL support

## Contributing

1. Create feature branch: `git checkout -b feature/xyz`
2. Commit changes: `git commit -am 'Add feature'`
3. Push: `git push origin feature/xyz`
4. Create pull request

## License

MIT License - See LICENSE file

## Support

For issues and questions:
- 📧 Email: support@example.com
- 🐛 GitHub Issues: [Create issue](https://github.com/joseph/sensitive-words-service/issues)
- 📖 Documentation: http://localhost:8080/swagger-ui.html
