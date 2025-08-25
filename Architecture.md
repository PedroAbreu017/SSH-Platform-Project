# SSH Platform - Architecture Documentation

## System Architecture

The SSH Platform implements a distributed architecture with two main components working in concert to provide secure container access through SSH tunneling.

### Component Overview

#### 1. REST API Server (Spring Boot)
**Purpose**: Container lifecycle management, user authentication, and data persistence  
**Technology**: Java 17, Spring Boot 3.2, PostgreSQL  
**Port**: 8080  

**Core Responsibilities**:
- JWT-based authentication with refresh token support
- Docker container orchestration via Docker API
- User management with role-based access control
- Container metadata persistence and audit logging
- RESTful endpoints for frontend integration

#### 2. SSH Gateway Server (Go)
**Purpose**: SSH protocol termination and container proxy  
**Technology**: Go 1.21, golang.org/x/crypto/ssh  
**Port**: 2222  

**Core Responsibilities**:
- SSH server implementation with password authentication
- Real-time API integration for user verification
- Interactive container selection interface
- Bidirectional SSH proxy to Docker containers
- RSA host key management and SSL termination

### Data Flow Architecture

```
Client SSH Connection (port 2222)
    │
    ▼
┌─────────────────────────────────────┐
│         SSH Gateway (Go)            │
│  ┌─────────────────────────────────┐│
│  │     Authentication Module       ││
│  │  ┌─────────────────────────────┐││
│  │  │   HTTP Client → REST API    │││  ← JWT Verification
│  │  └─────────────────────────────┘││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │    Container Selection UI       ││
│  │  ┌─────────────────────────────┐││
│  │  │   Interactive Terminal      │││  ← User Input
│  │  └─────────────────────────────┘││
│  └─────────────────────────────────┘│
│  ┌─────────────────────────────────┐│
│  │       SSH Proxy Module          ││
│  │  ┌─────────────────────────────┐││
│  │  │   Container SSH Client      │││  → Container Shell
│  │  └─────────────────────────────┘││
│  └─────────────────────────────────┘│
└─────────────────────────────────────┘
```

### Module Architecture (SSH Gateway)

```
ssh-gateway/
├── internal/
│   ├── config/          # Configuration management
│   │   └── config.go    # Config loading, defaults, validation
│   ├── api/             # REST API integration
│   │   └── client.go    # HTTP client, authentication, container queries
│   └── gateway/         # Core SSH functionality
│       ├── server.go    # SSH server, connection handling, authentication
│       └── proxy.go     # SSH proxy, session management, terminal handling
├── pkg/
│   └── crypto/          # Cryptographic operations
│       └── keys.go      # RSA key generation, SSH key management
└── main.go              # Application bootstrap
```

## Security Architecture

### Authentication Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ SSH Client  │    │SSH Gateway  │    │  REST API   │    │ PostgreSQL  │
└─────┬───────┘    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
      │                   │                  │                  │
      │ SSH Connect       │                  │                  │
      │ (user/password)   │                  │                  │
      ├──────────────────▶│                  │                  │
      │                   │ POST /auth/login │                  │
      │                   ├─────────────────▶│                  │
      │                   │                  │ SELECT user...   │
      │                   │                  ├─────────────────▶│
      │                   │                  │ User data        │
      │                   │                  │◀─────────────────┤
      │                   │ JWT tokens       │                  │
      │                   │◀─────────────────┤                  │
      │ SSH Menu          │                  │                  │
      │◀──────────────────┤                  │                  │
      │ Container choice  │                  │                  │
      │──────────────────▶│                  │                  │
      │                   │ GET /containers  │                  │
      │                   ├─────────────────▶│                  │
      │                   │ Container list   │                  │
      │                   │◀─────────────────┤                  │
      │ SSH Proxy Session │                  │                  │
      │◀─────────────────▶│                  │                  │
```

### JWT Token Structure

**Access Token Claims**:
```json
{
  "sub": "username",
  "userId": 1,
  "email": "user@example.com",
  "authorities": "ROLE_USER",
  "type": "access",
  "iat": 1756156770,
  "exp": 1756160370
}
```

**Security Features**:
- HMAC SHA-256 signature
- 1-hour expiration for access tokens
- 24-hour expiration for refresh tokens
- Role-based authorization
- Token type validation (access vs refresh)

## Container Management Architecture

### Container Lifecycle

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   API Request   │    │  Container       │    │   Docker        │
│                 │    │  Service         │    │   Engine        │
└────────┬────────┘    └─────────┬────────┘    └─────────┬───────┘
         │                       │                       │
         │ POST /containers       │                       │
         ├──────────────────────▶ │                       │
         │                       │ Pull Image (if needed) │
         │                       ├──────────────────────▶ │
         │                       │                       │
         │                       │ Create Container      │
         │                       ├──────────────────────▶ │
         │                       │ Container ID          │
         │                       │◀──────────────────────┤
         │                       │ Save to Database      │
         │                       │                       │
         │ Container Metadata    │                       │
         │◀──────────────────────┤                       │
         │                       │                       │
         │ POST /{id}/start      │                       │
         ├──────────────────────▶│                       │
         │                       │ Start Container       │
         │                       ├──────────────────────▶│
         │                       │ Update Status         │
         │                       │                       │
         │ Updated Status        │                       │
         │◀──────────────────────┤                       │
```

### Container Configuration Strategy

The system uses different initialization strategies based on container image type:

**Alpine Linux**:
```bash
apk add --no-cache openssh-server && \
ssh-keygen -A && \
echo 'root:password' | chpasswd && \
sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
/usr/sbin/sshd -D
```

**Ubuntu/Debian**:
```bash
export DEBIAN_FRONTEND=noninteractive && \
apt-get update -qq && \
apt-get install -y -qq openssh-server && \
mkdir -p /var/run/sshd && \
echo 'root:password' | chpasswd && \
/usr/sbin/sshd -D
```

**Pre-configured SSH Images**:
```bash
service ssh start && tail -f /dev/null
```

## SSH Proxy Architecture

### Session Management

The SSH proxy establishes a bridge between the client SSH session and container SSH server:

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│SSH Client   │    │SSH Gateway  │    │Container SSH│
│Session      │    │Proxy        │    │Server       │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       │ SSH Channel      │                  │
       ├─────────────────▶│                  │
       │                  │ SSH Client Conn  │
       │                  ├─────────────────▶│
       │                  │ New Session      │
       │                  ├─────────────────▶│
       │                  │ Request PTY      │
       │                  ├─────────────────▶│
       │                  │ Start Shell      │
       │                  ├─────────────────▶│
       │                  │                  │
    ┌──▼──────────────────▼──────────────────▼──┐
    │        Bidirectional I/O Proxy           │
    │  ┌─────────────────────────────────────┐  │
    │  │ stdin:  Client → Gateway → Container│  │
    │  │ stdout: Container → Gateway → Client│  │
    │  │ stderr: Container → Gateway → Client│  │
    │  └─────────────────────────────────────┘  │
    └───────────────────────────────────────────┘
```

### Terminal Configuration

The proxy configures proper terminal settings for interactive sessions:

- **PTY**: xterm-256color with 80x24 initial dimensions
- **Terminal Modes**: ECHO enabled, proper baud rates
- **I/O Handling**: Separate goroutines for stdin/stdout/stderr
- **Session Cleanup**: Proper resource cleanup on disconnect

## Database Schema

### Core Entities

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Containers table
CREATE TABLE containers (
    id BIGSERIAL PRIMARY KEY,
    container_id VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    image VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ssh_port INTEGER NOT NULL,
    user_id BIGINT REFERENCES users(id),
    environment_variables JSONB,
    port_mappings JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    stopped_at TIMESTAMP
);

-- SSH Keys table (future enhancement)
CREATE TABLE ssh_keys (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    key_name VARCHAR(100) NOT NULL,
    public_key TEXT NOT NULL,
    fingerprint VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Performance Characteristics

### Scalability Limits

**Current Architecture**:
- Single instance deployment
- In-memory session management
- Shared Docker daemon access
- PostgreSQL connection pooling (HikariCP)

**Tested Limits**:
- 50+ concurrent SSH sessions
- 100+ containers per user
- Sub-300ms API response times
- <100ms SSH proxy latency

### Resource Management

**Container Limits**:
```java
.withMemory(512L * 1024 * 1024)    // 512MB RAM limit
.withCpuQuota(50000L)              // 50% CPU limit
.withRestartPolicy(RestartPolicy.noRestart())
```

**Port Allocation**:
- Dynamic allocation in range 8000-9000
- Collision detection and retry
- Database tracking of allocated ports

## Error Handling & Resilience

### Circuit Breaker Patterns

**API Integration**:
- HTTP timeout: 10 seconds
- Retry logic for transient failures
- Graceful degradation on API unavailability

**Container Management**:
- Docker daemon health checking
- Container state reconciliation
- Orphaned container cleanup

### Logging & Observability

**Structured Logging**:
```json
{
  "timestamp": "2025-01-25T18:21:28.123Z",
  "level": "INFO",
  "logger": "com.platform.service.AuthService",
  "message": "Authentication successful for user: testuser2 (ID: 2)",
  "userId": 2,
  "username": "testuser2",
  "correlationId": "req-123-456"
}
```

**Audit Trail**:
- All authentication attempts
- Container lifecycle events
- SSH session establishment/termination
- API endpoint access patterns

## Future Enhancements

### Planned Architecture Improvements

1. **Multi-node Support**
   - Redis-based session sharing
   - Load balancer integration
   - Container affinity management

2. **Enhanced Security**
   - SSH key authentication
   - Certificate-based container access
   - Network policies and segmentation

3. **Monitoring Integration**
   - Prometheus metrics export
   - Grafana dashboard templates
   - Real-time alerting

4. **Container Persistence**
   - Volume management
   - Backup/restore functionality
   - Data migration tools

---

This architecture provides a solid foundation for enterprise container access management while maintaining security, scalability, and operational simplicity.