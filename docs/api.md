# API documentation

The REST API is served by the backend at:

```text
http://localhost:8080/api
```

Implemented endpoints:

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Run a real `SELECT 1` database connectivity probe (public) |
| POST | `/api/auth/register` | Create a BCrypt-hashed user account |
| POST | `/api/auth/login` | Authenticate and create a session |
| GET/POST/PUT/DELETE | `/api/vessels` | Manage vessels |
| GET/POST/PUT/DELETE | `/api/shipments` | Manage shipments and tracking numbers |
| GET/POST/PUT/DELETE | `/api/containers` | Manage containers |
| GET/POST/PUT/DELETE | `/api/ports` | Manage ports |
| GET/POST/PUT/DELETE | `/api/terminals` | Manage terminals |
| GET/POST/PUT/DELETE | `/api/berths` | Manage berths |
| GET/POST/PUT/DELETE | `/api/cargo` | Manage cargo records |
| GET/POST/PUT/DELETE | `/api/schedules` | Manage shipping schedules |
| GET/POST/PUT/DELETE | `/api/customs` | Manage customs clearances |
| GET/POST/PUT/DELETE | `/api/documents` | Manage documents |
| GET/POST/PUT/DELETE | `/api/invoices` | Manage invoices |
| GET/POST/PUT/DELETE | `/api/invoice-items` | Manage invoice line items |
| GET/POST/PUT/DELETE | `/api/payments` | Manage payments |
| GET | `/api/dashboard` | Return dashboard counters |
| GET/POST | `/api/berth-allocations` | List or create conflict-checked allocations |

Collection endpoints accept `page` (zero-based) and `size` (1-100) query
parameters and return Spring's page shape (`content`, `number`, `size`,
`totalElements`, and `totalPages`). All endpoints use JSON and return
structured error responses. Protected endpoints require authentication;
write access is restricted by role in `SecurityConfig`.

Example:

```powershell
$body = @{
  name = "North Sea Port"
  code = "NSP"
  country = "Fictionland"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/ports `
  -ContentType "application/json" -Body $body -WebSession $session
```

Validation failures return `400`, unknown IDs return `404`, duplicate or
foreign-key conflicts return `409`, unauthenticated requests return `401`,
and unavailable database services return `503`. Financial and audit records
should be retained for compliance; use application-level business workflows
for reversals rather than deleting posted records.
