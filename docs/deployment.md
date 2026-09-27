# Deployment guide

Deployment will consist of:

1. A MySQL 8 database initialized with `database/schema.sql`.
2. The Spring Boot backend packaged with Maven.
3. The static frontend served by a web server such as Nginx.

Runtime secrets must be supplied through environment variables. Do not commit
`.env` files, database passwords, JWT secrets, or uploaded documents.
