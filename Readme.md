# SSH Platform as a Service

Enterprise-grade SSH gateway with container orchestration, JWT authentication, and real-time container management via REST API.

## Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   SSH Client    │────│  SSH Gateway    │────│    Container    │
│   (port 2222)   │    │   (Go/SSH)     │    │  (Docker/SSH)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                │
                       ┌─────────────────┐    ┌─────────────────┐
                       │  REST API       │────│   PostgreSQL    │
                       │ (Spring Boot)   │    │   Database      │
                       └─────────────────┘    └─────────────────┘
```

## Core Features

### SSH Gateway (Go)
- SSH protocol server with password authentication
- Real-time container selection menu
- Transparent SSH proxy to Docker containers
- JWT-based user authentication via Spring Boot API
- Modular architecture with separation of concerns

### REST API (Java/Spring Boot)
- JWT authentication with access/refresh tokens
- Container lifecycle management (create, start, stop, delete)
- User management with role-based access control
- Docker API integration for real container orchestration
- PostgreSQL persistence with audit logging

### Container Management
- Support for multiple Linux distributions (Ubuntu, Alpine, Debian)
- Automatic SSH server installation and configuration
- Dynamic port allocation (8000-9000 range)
- Resource limits (CPU, memory) enforcement
- Container status monitoring and health checks

## Technical Stack

### Backend
- **Java 17** with Spring Boot 3.2
- **PostgreSQL** for data persistence
- **Redis** for caching (optional)
- **Docker API** for container management
- **JWT** for stateless authentication
- **Spring Security** with CORS support

### SSH Gateway
- **Go 1.21** with golang.org/x/crypto/ssh
- **Modular architecture** with internal packages
- **HTTP client** for API integration
- **RSA key management** for SSH host keys
- **Concurrent connection handling**

## Project Structure

```
ssh-platform-project/
├── backend/                    # Spring Boot REST API
│   ├── src/main/java/com/platform/
│   │   ├── config/            # Security, Docker configuration
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── model/            # JPA entities and enums
│   │   ├── repository/       # Data access layer
│   │   ├── dto/              # Request/Response objects
│   │   ├── security/         # JWT utilities
│   │   └── exception/        # Custom exceptions
│   ├── pom.xml
│   └── docker-compose.yml    # PostgreSQL + Redis
├── ssh-gateway/              # SSH Gateway in Go
│   ├── internal/
│   │   ├── config/          # Configuration management
│   │   ├── api/             # Spring Boot API client
│   │   └── gateway/         # SSH server and proxy
│   ├── pkg/crypto/          # RSA key generation
│   ├── main.go              # Application entry point
│   └── go.mod
└── README.md
```

## Quick Start

### Prerequisites
- Java 17+
- Go 1.21+
- Docker & Docker Compose
- Maven 3.6+

### 1. Start Infrastructure
```bash
cd backend
docker-compose up -d postgres redis
```

### 2. Start REST API
```bash
mvn spring-boot:run
```

### 3. Start SSH Gateway
```bash
cd ../ssh-gateway
go run main.go
```

### 4. Test the System
```bash
# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# Create container
curl -X POST http://localhost:8080/api/containers \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"my-alpine","image":"alpine:3.18"}'

# Connect via SSH Gateway
ssh -p 2222 testuser@localhost
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Token refresh
- `GET /api/auth/me` - Current user info

### Container Management
- `GET /api/containers` - List user containers
- `POST /api/containers` - Create new container
- `POST /api/containers/{id}/start` - Start container
- `POST /api/containers/{id}/stop` - Stop container
- `DELETE /api/containers/{id}` - Delete container
- `GET /api/containers/stats` - User container statistics

### System
- `GET /api/containers/images` - List allowed container images
- `GET /actuator/health` - Application health check

## Security Features

### Authentication & Authorization
- JWT-based stateless authentication
- BCrypt password hashing
- Role-based access control (USER, ADMIN)
- Token expiration and refresh mechanism

### Network Security
- SSH host key verification
- Container isolation with resource limits
- Dynamic port allocation to avoid conflicts
- CORS configuration for web clients

### Container Security
- Allowed image whitelist
- Resource constraints (CPU, memory)
- User-based container isolation
- SSH access with password authentication

## Configuration

### Backend (application.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/platform
    username: platform
    password: platform123

jwt:
  secret: your-secret-key
  expiration: 3600000  # 1 hour
  refresh-expiration: 86400000  # 24 hours

docker:
  host: unix:///var/run/docker.sock
```

### SSH Gateway
The SSH Gateway uses default configuration or loads from `config.json`:
```json
{
  "ssh_port": 2222,
  "api_base_url": "http://localhost:8080/api",
  "host_key_path": "host_key"
}
```

## Development

### Adding New Container Images
Update the `ALLOWED_IMAGES` list in `ContainerService.java`:
```java
private static final List<String> ALLOWED_IMAGES = List.of(
    "ubuntu:22.04",
    "alpine:3.18",
    "your-new-image:tag"
);
```

### Extending SSH Gateway
Add new modules in `internal/` directory following the established pattern:
- `internal/monitoring/` - Health checks and metrics
- `internal/auth/` - Advanced authentication methods
- `internal/tunnel/` - Port forwarding implementation

## Performance Characteristics

### Tested Limits
- **Concurrent SSH connections**: 50+
- **Container startup time**: < 30 seconds
- **API response time**: < 300ms
- **Authentication**: < 200ms
- **SSH proxy latency**: < 100ms

### Resource Usage
- **Memory**: ~256MB (API) + ~64MB (SSH Gateway)
- **CPU**: < 50% single core under normal load
- **Storage**: Minimal (containers use Docker storage)

## Monitoring & Logging

### Application Metrics
Available via Spring Boot Actuator:
- `/actuator/health` - Application health
- `/actuator/metrics` - Performance metrics
- `/actuator/info` - Application information

### Logging
- Structured logging with correlation IDs
- SSH connection audit trail
- Container lifecycle events
- Authentication attempt logging

## Known Limitations

1. **SSH Key Authentication**: Currently supports password-only
2. **Port Forwarding**: Not yet implemented
3. **Container Persistence**: Containers use ephemeral storage
4. **Multi-node**: Single instance deployment only
5. **Windows SSH Server**: Limited compatibility with Windows containers

## Contributing

1. Follow the established module structure
2. Add comprehensive tests for new features
3. Update documentation for API changes
4. Ensure security considerations are addressed
5. Test with multiple container types

## License

This project is for educational and portfolio purposes. See LICENSE file for details.

---

**Author**: Pedro Abreu  
**Technologies**: Java, Go, Spring Boot, Docker, PostgreSQL, SSH Protocol  
**Architecture**: Microservices, REST API, SSH Gateway, Container Orchestration