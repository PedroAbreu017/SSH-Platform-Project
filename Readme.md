# SSH Platform - Container Management System

Full-stack platform for managing Docker containers with SSH access, real-time logs streaming, and comprehensive REST API.

---

## ✨ Features

### ✅ Authentication & Security
- JWT authentication with access/refresh tokens
- User registration and login
- BCrypt password hashing
- Role-based access control (USER, ADMIN)
- CORS configuration

### ✅ Container Management
- Create, start, stop, delete containers
- Support for multiple images (Ubuntu, Alpine, Debian, Node, Python, etc.)
- Dynamic port allocation (8000-9000)
- Automatic SSH server setup in containers
- Container status monitoring
- Image whitelist for security
- User isolation (5 containers per user limit)

### ✅ SSH Key Management (TDD Implementation)
- Add/delete SSH public keys
- Key fingerprint generation (SHA256)
- Key type detection (RSA, ECDSA, ED25519)
- Usage tracking
- Statistics dashboard
- 9 unit tests covering all edge cases

### ✅ Container Logs
- **REST API** - Historical logs with configurable line count
- **WebSocket** - Real-time log streaming
- Download logs as `.txt` files
- Pause/resume live streaming
- Clear logs without disconnecting
- Line counter and connection status

### ✅ Dashboard & UI
- Responsive React interface
- Real-time metrics (containers, SSH keys)
- Container cards with status indicators
- Simulated terminal interface
- Modern dark theme with Tailwind CSS

---

## 🛠 Tech Stack

### Backend
- **Java 17** with Spring Boot 3.2.0
- **PostgreSQL 15** for persistence
- **Redis 7** for caching
- **Docker Java API** for container management
- **WebSocket** for real-time features
- **Spring Security** with JWT
- **Maven** for build management

### Frontend
- **React 18** with TypeScript
- **Vite** for fast development
- **Tailwind CSS** for styling
- **Axios** for API calls
- **React Router** for navigation
- **lucide-react** for icons

### Infrastructure
- **Docker & Docker Compose**
- **PostgreSQL** database
- **Redis** cache (optional)

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Maven 3.6+

### 1. Start Infrastructure
```bash
cd backend
docker-compose up -d postgres redis
2. Start Backend
bash
Copy code
# Linux/WSL
mvn spring-boot:run -Dspring-boot.run.profiles=unix

# Windows (if Docker Desktop exposed on tcp://localhost:2375)
mvn spring-boot:run
3. Start Frontend
bash
Copy code
cd frontend
npm install
npm run dev
4. Access Application
Frontend: http://localhost:5173

Backend API: http://localhost:8080

Health Check: http://localhost:8080/actuator/health

📡 API Endpoints
Authentication
POST /api/auth/register → Register new user

POST /api/auth/login → User login

POST /api/auth/refresh → Refresh access token

Containers
GET /api/containers → List user containers

POST /api/containers → Create container

POST /api/containers/{id}/start → Start container

POST /api/containers/{id}/stop → Stop container

DELETE /api/containers/{id} → Delete container

GET /api/containers/{id}/logs → Get historical logs

WS /ws/logs/{id} → Stream live logs (WebSocket)

SSH Keys
GET /api/ssh-keys → List user SSH keys

POST /api/ssh-keys → Add SSH key

DELETE /api/ssh-keys/{id} → Delete SSH key

GET /api/ssh-keys/stats → Get statistics

🧪 Testing
Run Tests
bash
Copy code
cd backend
mvn test
Test Coverage
14 unit tests passing

SSH Key Service: 9 tests (TDD implementation)

Container Logs Service: 3 tests

Context Tests: 2 tests

Integration tests with TestContainers require Docker Desktop configured properly.

📂 Project Structure
bash
Copy code
ssh-platform-project/
├── backend/
│   ├── src/main/java/com/platform/
│   │   ├── config/           # Security, Docker, WebSocket
│   │   ├── controller/       # REST endpoints
│   │   ├── service/          # Business logic
│   │   ├── model/entity/     # JPA entities
│   │   ├── repository/       # Data access
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── security/jwt/     # JWT utilities
│   │   ├── websocket/        # WebSocket handlers
│   │   └── exception/        # Custom exceptions
│   └── src/test/java/        # Unit & integration tests
├── frontend/
│   ├── src/
│   │   ├── components/       # React components
│   │   ├── pages/            # Page components
│   │   ├── services/         # API client
│   │   └── context/          # React context
│   └── public/
└── README.md
🔒 Security Features
Password Hashing: BCrypt with salt

JWT Tokens: Access (1h) + Refresh (24h)

Image Whitelist: Only approved Docker images

User Isolation: Containers isolated by user

Resource Limits: 5 containers per user

CORS Protection: Configured allowed origins

SQL Injection Protection: JPA with parameterized queries

Allowed Docker Images
Ubuntu, Alpine, Debian, Node.js, Python, OpenJDK, Nginx
(see ContainerService.java for complete list)

⚠️ Known Limitations
Terminal: Simulated only (real SSH terminal in roadmap v2.0)

WebSocket Logs: Disconnects after Docker API timeout (~30s idle)

Windows Compatibility: Best on Linux/WSL for Docker socket access

Single Instance: No clustering support yet

🛤 Roadmap
v2.0 (Future)
Real SSH terminal with xterm.js

Resource monitoring (CPU/Memory)

Container health checks

Docker Compose support

Multi-node deployment

🧑‍💻 Development
Adding New Features
Backend: Add service → controller → tests

Frontend: Add component → integrate API

Run tests: mvn test

Commit with descriptive message

Environment Profiles
default → Windows (tcp://localhost:2375)

unix → Linux/WSL (unix:///var/run/docker.sock)

🤝 Contributing
This is a portfolio/educational project. Feel free to fork and extend.

📜 License
Educational/Portfolio project - See LICENSE

Author: Pedro Abreu
Stack: Spring Boot • React • TypeScript • Docker • PostgreSQL • WebSocket
Architecture: REST API • WebSocket • Container Orchestration • JWT Auth

markdown
Copy code

Quer que eu já adapte esse `README.md` para ficar **mais voltado a portfólio profissional** (com ênfase em impacto e diferenciais técnicos), ou prefere manter esse estilo mais **técnico e detalhado**?






You said:
pode ser mais voltado a portfólio profissional


ChatGPT said:
Boa! 👌 Então a ideia é deixar o README.md não só técnico, mas também atrativo para recrutadores, destacando impacto, diferenciais técnicos e propósito.
Segue a versão adaptada para portfólio profissional:

markdown
Copy code
# 🚀 SSH Platform - Container Management System  

**Full-stack platform** for secure Docker container management with **SSH access**, **real-time log streaming**, and a **comprehensive REST API**.  
Designed as a **portfolio project** to demonstrate **enterprise-grade architecture**, **clean code practices**, and **production-ready features**.  

---

## 🎯 Project Goals
- Demonstrate **full-stack expertise**: Java (Spring Boot) + React (TypeScript).  
- Apply **real-world DevOps practices**: container orchestration, monitoring, caching, and security.  
- Showcase **scalable design patterns**: layered architecture, DTOs, WebSockets, and role-based access.  
- Build a platform with **practical use cases** for developers and teams.  

---

## ✨ Key Features

### 🔐 Authentication & Security
- JWT with **access/refresh tokens**
- BCrypt **password hashing**
- **Role-based access control** (USER / ADMIN)
- **CORS protection** and SQL injection prevention
- **User isolation** (up to 5 containers per user)

### 🐳 Container Management
- Create, start, stop, delete containers  
- Automatic **SSH server setup** inside containers  
- Dynamic port allocation (8000–9000)  
- Image whitelist for security (Ubuntu, Debian, Alpine, Node, Python, etc.)  
- Container status monitoring  

### 🔑 SSH Key Management (TDD Approach)
- Add/Delete SSH public keys  
- Key type detection (**RSA, ECDSA, ED25519**)  
- Fingerprint generation (SHA256)  
- Usage statistics + dashboard  
- **9 unit tests** (100% coverage on this module)  

### 📡 Container Logs
- **REST API** → retrieve historical logs  
- **WebSocket** → real-time streaming with pause/resume  
- Download logs as `.txt`  
- Clear logs without disconnecting  
- Connection + line counter indicators  

### 🎨 Dashboard & UI
- **Responsive React + Tailwind CSS** interface  
- Real-time metrics (containers, SSH keys)  
- Container status cards with visual indicators  
- **Simulated terminal** (real SSH terminal planned in roadmap)  
- Modern dark theme  

---

## 🛠 Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2**  
- **PostgreSQL 15** for persistence  
- **Redis 7** for caching  
- **Docker Java API** for container management  
- **WebSocket** for real-time communication  
- **Spring Security (JWT)**  

### Frontend
- **React 18 + TypeScript**  
- **Vite** (fast dev environment)  
- **Tailwind CSS** (modern UI styling)  
- **Axios** (API client)  
- **React Router** (routing)  
- **lucide-react** (icons)  

### Infrastructure
- **Docker & Docker Compose**  
- PostgreSQL + Redis containers  

---

## 🚀 Quick Start

### Prerequisites
- Java 17+, Node.js 18+, Docker, Maven 3.6+

### Run Project
```bash
# Start infrastructure
cd backend
docker-compose up -d postgres redis

# Start backend
mvn spring-boot:run -Dspring-boot.run.profiles=unix   # Linux/WSL
mvn spring-boot:run                                   # Windows

# Start frontend
cd frontend
npm install
npm run dev
Access
Frontend → http://localhost:5173

Backend API → http://localhost:8080

Health Check → http://localhost:8080/actuator/health

📊 Testing & Quality
14 Unit Tests (JUnit + Mockito)

9 tests → SSH Key Service (TDD approach)

3 tests → Container Logs Service

2 tests → Context & configuration

✅ TDD applied on critical modules (SSH Key Service)
✅ Integration tests with TestContainers (Docker-based)

📂 Project Structure
bash
Copy code
ssh-platform-project/
├── backend/ (Spring Boot API)
│   ├── config/         # Security, Docker, WebSocket
│   ├── controller/     # REST endpoints
│   ├── service/        # Business logic
│   ├── model/entity/   # JPA entities
│   ├── repository/     # Data access
│   ├── dto/            # Request/Response objects
│   ├── security/jwt/   # JWT utilities
│   ├── websocket/      # Real-time logs
│   └── exception/      # Custom exception handling
│
├── frontend/ (React + TypeScript)
│   ├── components/     # UI components
│   ├── pages/          # Page-level components
│   ├── services/       # API client
│   └── context/        # Global state
│
└── docker-compose.yml
🔒 Security Highlights
BCrypt password hashing

JWT Auth (Access = 1h, Refresh = 24h)

Whitelisted Docker images

Per-user container limits

CORS + SQL Injection protection

⚠️ Known Limitations
Terminal is simulated (real SSH terminal in roadmap v2.0)

WebSocket logs may disconnect after ~30s idle (Docker API timeout)

Best experience on Linux/WSL (Windows socket limitations)

Single instance only (no clustering yet)

🛤 Roadmap (v2.0)
Real SSH terminal with xterm.js

Resource monitoring (CPU/Memory usage)

Container health checks

Native Docker Compose support

Multi-node / cluster deployment

📌 About the Author
👨‍💻 Pedro Marschhausen
Full Stack Developer • Spring Boot • React • Docker • PostgreSQL • WebSocket

GitHub

LinkedIn

📧 pedroabreu6497@gmail.com

📜 License
Educational / Portfolio Project – MIT License.