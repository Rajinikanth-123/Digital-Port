# Digital Port and Shipping Management System

A comprehensive, enterprise-grade digital management platform for port operations, vessel scheduling, berth allocations, container tracking, cargo handling, customs clearances, invoicing, payments, system audit logging, global search, user profiles, and support ticketing.

---

## 1. System Architecture & Project Structure

```text
digital-port/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/portshipping/
│       │   ├── DigitalPortShippingApplication.java
│       │   ├── config/
│       │   │   └── SecurityConfig.java
│       │   ├── controller/
│       │   │   ├── AuditLogController.java
│       │   │   ├── AuthController.java
│       │   │   ├── BerthController.java
│       │   │   ├── CargoController.java
│       │   │   ├── ContainerController.java
│       │   │   ├── CustomsClearanceController.java
│       │   │   ├── DocumentController.java
│       │   │   ├── FeedbackController.java
│       │   │   ├── HelpCenterController.java
│       │   │   ├── HealthController.java
│       │   │   ├── InvoiceController.java
│       │   │   ├── NotificationController.java
│       │   │   ├── PaymentController.java
│       │   │   ├── PortController.java
│       │   │   ├── ReportsController.java
│       │   │   ├── SearchController.java
│       │   │   ├── ShipmentController.java
│       │   │   ├── SupportTicketController.java
│       │   │   ├── TerminalController.java
│       │   │   ├── UserController.java
│       │   │   ├── UserProfileController.java
│       │   │   └── VesselController.java
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── repository/
│       │   └── service/
│       └── main/resources/
│           ├── application.properties
│           └── application-dev.properties
├── frontend/
│   ├── index.html
│   ├── login.html
│   ├── dashboard.html
│   ├── ports.html
│   ├── terminals.html
│   ├── vessels.html
│   ├── berths.html
│   ├── containers.html
│   ├── cargo.html
│   ├── shipments.html
│   ├── schedules.html
│   ├── customs.html
│   ├── documents.html
│   ├── invoices.html
│   ├── payments.html
│   ├── reports.html
│   ├── notifications.html
│   ├── audit-logs.html
│   ├── search.html
│   ├── profile.html
│   ├── help.html
│   ├── users.html
│   ├── css/
│   └── js/
│       ├── config.js
│       ├── navigation.js
│       ├── app.js
│       ├── dashboard.js
│       ├── api/api.js
│       └── modules/
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── migration_*.sql
└── .github/
    └── workflows/
        └── ci.yml
```

---

## 2. Prerequisites & Tools

- **Java**: JDK 17 or JDK 25 LTS
- **Build Tool**: Apache Maven 3.9+ (or included `./mvnw`)
- **Database**: MySQL 8.0+ (Production) / H2 (Development & Testing)
- **Web Browser**: Chrome / Firefox / Edge
- **Operating System**: Windows 11 / Linux / macOS

---

## 3. Database Setup & Initialization

### MySQL Schema Setup
1. Log into your local MySQL server using MySQL Workbench or Command Line Client:
   ```sql
   CREATE DATABASE digital_port_shipping CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Execute the schema initialization script:
   ```sql
   SOURCE database/schema.sql;
   ```
3. (Optional) Populate seed data for development:
   ```sql
   SOURCE database/seed.sql;
   ```

---

## 4. Environment Variables Configuration

Copy `.env.example` to `.env` or set system environment variables:

| Environment Variable | Description | Default Value |
|---|---|---|
| `SERVER_PORT` | HTTP Server Listener Port | `8080` |
| `DB_HOST` | Database Hostname / IP | `localhost` |
| `DB_PORT` | Database Port | `3306` |
| `DB_NAME` | MySQL Database Name | `digital_port_shipping` |
| `DB_USERNAME` | Database User | `root` |
| `DB_PASSWORD` | Database Password | *(Empty / System Default)* |
| `DB_SSL` | Enable SSL DB Connection | `false` |
| `JPA_DDL_AUTO` | Hibernate Schema Strategy | `validate` |
| `JPA_SHOW_SQL` | Print Executed SQL | `false` |
| `LOG_LEVEL` | Application Logging Level | `INFO` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated CORS Origins | `http://localhost:5500,http://127.0.0.1:5500,http://localhost:3000,http://localhost:5173,http://localhost:8080` |

---

## 5. Local Execution Instructions (Windows PowerShell)

### A. Run Backend (Development In-Memory Mode)
```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### B. Run Backend (MySQL Production Configuration)
Set your local environment variables in PowerShell and start the backend:
```powershell
cd backend
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="digital_port_shipping"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YourPasswordHere"

.\mvnw.cmd spring-boot:run
```

### C. Serve Frontend Client
Serve the `frontend/` directory using Python HTTP Server or VS Code Live Server:
```powershell
cd frontend
python -m http.server 5500
```
Open your browser at `http://localhost:5500`.

---

## 6. Automated Testing & Verification

Execute the complete backend test suite:
```powershell
cd backend
.\mvnw.cmd clean test
```

Execute a specific system test suite:
```powershell
cd backend
.\mvnw.cmd test -Dtest=SupportTicketsHelpCenterSystemTests
```

Build production executable JAR package:
```powershell
cd backend
.\mvnw.cmd clean package
```

---

## 7. API Base URL & Health Check Endpoint

- **Base REST API URL**: `http://localhost:8080/api`
- **Health Check Endpoint**: `http://localhost:8080/api/health`
  ```json
  {
    "status": "UP",
    "database": "UP",
    "timestamp": "2026-09-27T04:50:54Z"
  }
  ```

---

## 8. Deployment Architecture & Go-Live Checklist

```text
[ Users / Browser Client ]
           │ (HTTPS)
           ▼
[ Frontend Web Host ]  ──►  [ Java Spring Boot REST API ]  ──►  [ MySQL Private DB ]
 (HTML/CSS/JS Static)             (Port 8080/443)                  (Port 3306)
```

### Go-Live Readiness Checklist
- [x] **Frontend Pages**: All 20 UI module pages load and render cleanly.
- [x] **Backend Build**: Spring Boot compiles without errors and passes all automated tests.
- [x] **Database Schema**: Validated against `schema.sql` and Hibernate mappings (`ddl-auto=validate`).
- [x] **Security**: Passwords hashed via BCrypt, CSRF token cookies active, role-based authorization enforced.
- [x] **Secrets Management**: Credentials externalized via environment variables.
- [x] **CORS Configuration**: Allowed origins externalized via `CORS_ALLOWED_ORIGINS`.
- [x] **CI/CD Integration**: GitHub Actions workflow created at `.github/workflows/ci.yml`.
- [x] **Documentation**: Complete setup, configuration, and troubleshooting guide in `README.md`.

---

## 9. Troubleshooting & Known Limitations

- **Port Conflicts (`Address already in use`)**: If port `8080` is in use, set `$env:SERVER_PORT="8085"` before starting the application.
- **CSRF Token Mismatch**: Ensure your client sends `credentials: 'include'` in HTTP requests so session cookies and `X-XSRF-TOKEN` headers are attached.
- **External Provider Integrations**: Live GPS vessel tracking, SMS gateways, and credit card processing require sandbox API keys from external service providers.
