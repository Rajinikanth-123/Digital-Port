# Architecture

The system is organized as a small monorepo:

```text
frontend/   Static HTML, CSS, and browser JavaScript
backend/    Spring Boot REST API and business services
database/   MySQL schema and development seed data
docs/       Architecture, API, and deployment documentation
```

The backend follows a layered design:

```text
controller -> service -> repository -> database
     |           |
    DTO       domain entities
```

Authentication and authorization are isolated under the `security` package.
Cross-cutting configuration and exception handling live under `config` and
`exception`.
