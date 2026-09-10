# Swagger/OpenAPI Annotations Reference Guide

**Shows how each annotation in source code generates the contract**

---

## 🏷️ CLASS-LEVEL ANNOTATIONS

### 1. @RestController
**Purpose**: Mark class as REST controller  
**In Code**: `public class SanitizeController {`  
**In Contract**: Auto-generates endpoint operations

### 2. @RequestMapping
**Purpose**: Base path for all endpoints  
**In Code**: `@RequestMapping("/api/v1/sanitize")`  
**In Contract**: 
```json
"paths": {
  "/api/v1/sanitize": { ... }
}
```

### 3. @Tag
**Purpose**: Group related endpoints  
**In Code**: `@Tag(name = "Sanitize", description = "Mask sensitive words...")`  
**In Contract**:
```json
"tags": [
  {
    "name": "Sanitize",
    "description": "Mask sensitive words in arbitrary text"
  }
]
```

### 4. @SecurityRequirement (Class-level)
**Purpose**: Mark all endpoints as requiring authentication  
**In Code**: `@SecurityRequirement(name = "bearer-jwt")`  
**In Contract**: Applied to all operations in class

---

## 🔌 METHOD-LEVEL ANNOTATIONS

### 1. @PostMapping / @GetMapping / @PutMapping / @DeleteMapping
**Purpose**: Define HTTP method and sub-path  
**In Code**: 
```java
@PostMapping          // → POST /api/v1/sanitize
@GetMapping("/{id}")  // → GET /api/v1/sanitize/{id}
@PutMapping("/{id}")  // → PUT /api/v1/sanitize/{id}
@DeleteMapping("/{id}") // → DELETE /api/v1/sanitize/{id}
```

**In Contract**:
```json
"paths": {
  "/api/v1/sanitize": {
    "post": { ... },
    "get": { ... },
    "put": { ... },
    "delete": { ... }
  }
}
```

### 2. @Operation
**Purpose**: Document endpoint purpose and behavior  
**In Code**:
```java
@Operation(
    summary = "Sanitize text by masking sensitive words",
    description = "Replace all occurrences of sensitive words with asterisks (*). Matching is case-insensitive.",
    tags = {"Sanitize"}
)
```

**In Contract**:
```json
{
  "summary": "Sanitize text by masking sensitive words",
  "description": "Replace all occurrences of sensitive words with asterisks (*). Matching is case-insensitive.",
  "tags": ["Sanitize"]
}
```

### 3. @ApiResponse
**Purpose**: Document single HTTP response  
**In Code**:
```java
@ApiResponse(
    responseCode = "200",
    description = "Text sanitized successfully",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SanitizeResponse.class)
    )
)
```

**In Contract**:
```json
"responses": {
  "200": {
    "description": "Text sanitized successfully",
    "content": {
      "application/json": {
        "schema": {
          "$ref": "#/components/schemas/SanitizeResponse"
        }
      }
    }
  }
}
```

### 4. @ResponseStatus
**Purpose**: Set HTTP status code for success  
**In Code**: `@ResponseStatus(HttpStatus.CREATED)`  
**In Contract**: Affects default response code

### 5. @SecurityRequirement (Method-level)
**Purpose**: Require authentication for this endpoint  
**In Code**: `@SecurityRequirement(name = "bearer-jwt")`  
**In Contract**:
```json
"security": [
  {
    "bearer-jwt": []
  }
]
```

---

## 📥 REQUEST PARAMETER ANNOTATIONS

### 1. @RequestBody
**Purpose**: Document request body  
**In Code**:
```java
@RequestBody(
    description = "Text to sanitize",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = SanitizeRequest.class)
    )
)
@Valid SanitizeRequest request
```

**In Contract**:
```json
"requestBody": {
  "description": "Text to sanitize",
  "required": true,
  "content": {
    "application/json": {
      "schema": {
        "$ref": "#/components/schemas/SanitizeRequest"
      }
    }
  }
}
```

### 2. @PathVariable + @Parameter
**Purpose**: Document URL path parameter  
**In Code**:
```java
@PathVariable
@Parameter(
    description = "Unique identifier of the sensitive word",
    example = "1"
)
Long id
```

**In Contract**:
```json
"parameters": [
  {
    "name": "id",
    "in": "path",
    "description": "Unique identifier of the sensitive word",
    "required": true,
    "schema": {
      "type": "integer",
      "format": "int64",
      "example": 1
    }
  }
]
```

### 3. @RequestParam + @Parameter
**Purpose**: Document query string parameter  
**In Code**:
```java
@RequestParam(required = false)
@Parameter(
    description = "Filter by active status",
    example = "true"
)
Boolean active
```

**In Contract**:
```json
"parameters": [
  {
    "name": "active",
    "in": "query",
    "description": "Filter by active status",
    "required": false,
    "schema": {
      "type": "boolean",
      "example": true
    }
  }
]
```

### 4. @Parameter
**Purpose**: Document any parameter (path, query, header)  
**In Code**:
```java
@Parameter(
    description = "The text to sanitize",
    example = "This is a badword text"
)
```

**In Contract**:
```json
{
  "description": "The text to sanitize",
  "example": "This is a badword text"
}
```

---

## 📊 DATA MODEL ANNOTATIONS

### 1. @Schema (Class-level)
**Purpose**: Document the entire DTO  
**In Code**:
```java
@Schema(
    description = "Response containing sanitized text",
    example = """
    {
      "originalText": "...",
      "sanitizedText": "...",
      "matchCount": 1
    }"""
)
public class SanitizeResponse {
    // ...
}
```

**In Contract**:
```json
"components": {
  "schemas": {
    "SanitizeResponse": {
      "type": "object",
      "description": "Response containing sanitized text",
      "example": { ... }
    }
  }
}
```

### 2. @Schema (Field-level)
**Purpose**: Document single field  
**In Code**:
```java
@Schema(
    description = "Total number of sensitive words found",
    example = "1"
)
private int matchCount;
```

**In Contract**:
```json
"properties": {
  "matchCount": {
    "type": "integer",
    "description": "Total number of sensitive words found",
    "example": 1
  }
}
```

### 3. Validation Annotations (Become Schema constraints)
**In Code**:
```java
@NotBlank(message = "Text must not be blank")
@Size(min = 1, max = 255)
private String word;
```

**In Contract**:
```json
{
  "type": "string",
  "minLength": 1,
  "maxLength": 255
}
```

---

## 🔐 COMPLETE EXAMPLE: FULL ANNOTATION → CONTRACT MAPPING

### Source Code
**File**: `SanitizeController.java`

```java
@RestController                                    // ← Marks as REST controller
@RequestMapping("/api/v1/sanitize")               // ← Base path
@RequiredArgsConstructor                          // ← Lombok dependency injection
@Tag(                                             // ← Tags endpoint group
    name = "Sanitize", 
    description = "Mask sensitive words in arbitrary text"
)
@SecurityRequirement(name = "bearer-jwt")         // ← All endpoints require JWT
public class SanitizeController {
    private final SanitizationService sanitizationService;

    @PostMapping                                  // ← POST method, no sub-path
    @Operation(                                   // ← Endpoint documentation
        summary = "Sanitize text by masking sensitive words",
        description = "Replace all occurrences of sensitive words with asterisks (*)"
    )
    @ApiResponse(                                 // ← 200 response
        responseCode = "200",
        description = "Text sanitized successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SanitizeResponse.class)
        )
    )
    @ApiResponse(                                 // ← 400 response
        responseCode = "400",
        description = "Bad Request - Invalid or empty text"
    )
    @ApiResponse(                                 // ← 401 response
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token"
    )
    public SanitizeResponse sanitize (
        @Valid                                    // ← Validate input
        @RequestBody(                             // ← This is request body
            description = "Text to sanitize"
        )
        SanitizeRequest request                   // ← DTO is auto-documented via @Schema
    ) {
        // Implementation
    }
}
```

### Generated Contract
**File**: `sensitive-words-api.json`

```json
{
  "paths": {
    "/api/v1/sanitize": {
      "post": {
        "tags": ["Sanitize"],
        "summary": "Sanitize text by masking sensitive words",
        "description": "Replace all occurrences of sensitive words with asterisks (*)",
        "operationId": "sanitize",
        "security": [
          {
            "bearer-jwt": []
          }
        ],
        "requestBody": {
          "description": "Text to sanitize",
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/SanitizeRequest"
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "Text sanitized successfully",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/SanitizeResponse"
                }
              }
            }
          },
          "400": {
            "description": "Bad Request - Invalid or empty text"
          },
          "401": {
            "description": "Unauthorized - Missing or invalid JWT token"
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "SanitizeRequest": {
        "type": "object",
        "description": "Request to sanitize text by masking sensitive words",
        "properties": {
          "text": {
            "type": "string",
            "description": "Raw text to be sanitized",
            "example": "This is a badword text that needs sanitization"
          }
        },
        "required": ["text"]
      },
      "SanitizeResponse": {
        "type": "object",
        "description": "Response containing sanitized text",
        "properties": {
          "originalText": {
            "type": "string",
            "description": "The original unmodified text",
            "example": "This is a badword text that needs sanitization"
          },
          "sanitizedText": {
            "type": "string",
            "description": "The sanitized text with sensitive words masked",
            "example": "This is a ******* text that needs sanitization"
          },
          "matchCount": {
            "type": "integer",
            "description": "Number of sensitive words found and replaced",
            "example": 1
          }
        }
      }
    }
  }
}
```

---

## 📋 ANNOTATION CHECKLIST

### When documenting an endpoint, use:

**Class Level**:
- [ ] `@RestController`
- [ ] `@RequestMapping(path)`
- [ ] `@Tag(name, description)`
- [ ] `@SecurityRequirement` (if protected)

**Method Level**:
- [ ] `@PostMapping/@GetMapping/@PutMapping/@DeleteMapping`
- [ ] `@Operation(summary, description)`
- [ ] `@ApiResponse` for each HTTP status (200, 201, 400, 401, 404, 500)
- [ ] `@ResponseStatus` (if non-standard success code)

**Parameters**:
- [ ] `@RequestBody` + `@Valid`
- [ ] `@PathVariable` + `@Parameter`
- [ ] `@RequestParam` + `@Parameter`
- [ ] `@Parameter(description, example)`

**DTOs**:
- [ ] `@Schema` on class (description, example)
- [ ] `@Schema` on each field (description, example)
- [ ] `@NotNull/@NotBlank` etc. for validation

---

## ✅ VERIFICATION STEPS

1. **Start the service**:
   ```bash
   java -jar target/sensitive-words-service-0.0.1-SNAPSHOT.jar
   ```

2. **Open Swagger UI**:
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. **Verify each endpoint**:
   - Expand each endpoint
   - Check "summary" matches `@Operation.summary`
   - Check "Parameters" section matches `@Parameter` annotations
   - Check "Request body" schema matches `@RequestBody`
   - Check "Responses" section has all `@ApiResponse` codes
   - Try it out to verify it works

4. **Check the raw spec**:
   ```
   http://localhost:8080/v3/api-docs
   ```
   Compare with `sensitive-words-api.json`

---

**Last Updated**: September 11, 2026  
**Springdoc Version**: 2.6.0  
**Spring Boot**: 3.3.0  
**Java**: 21
