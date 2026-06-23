# Installation and Deployment Guide

## Local Development

### Prerequisites
- Docker and Docker Compose
- Git

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SlachDevM/MRRG.git
   cd MRRG
   ```

2. **Start local development environment:**
   ```bash
     docker compose up --build -d
   ```

   This brings up:
   - **Backend**: http://localhost:4000 (Spring Boot API)
   - **Frontend**: http://localhost:3000 (React app)
   - **Database**: PostgreSQL on localhost:5432
   - **Email**: Mailpit (local email testing) on http://localhost:8025
   - **Swagger UI**: http://localhost:4000/swagger-ui/index.html

3. **Login to the application:**
   - Navigate to http://localhost:3000
   - Use the bootstrap administrator credentials:
     - Email: `admin@mrrg.local`
     - Password: `test`

   On a fresh database, the backend creates this administrator automatically at startup when no `ADMIN` user exists yet. The credentials come from the `INITIAL_ADMIN_*` environment variables configured in `docker-compose.yml`. Passwords are hashed with the application's `PasswordEncoder` before persistence. The bootstrap is idempotent and does not recreate or reset an existing administrator.

   To disable the bootstrap:

   ```properties
   app.bootstrap.initial-admin.enabled=false
   ```

### Firebase Configuration (Optional)

For push notifications, place the Firebase service account JSON file:
```
backend/
└── firebase-service-account.json
```

The Docker Compose configuration mounts this file automatically.

### Local Configuration

Local development uses:
- **Docker Compose**: `docker-compose.yml`
- **Database**: Local PostgreSQL with auto-schema update (`ddl-auto=update`)
- **Email**: Mailpit (development only, no real SMTP)
- **Firebase**: Optional (configure `firebase-service-account.json` if needed)
- **Profiles**: `dev` profile active by default, Automatic database initialization with development seed data.
- **Initial admin bootstrap**: enabled by default in local Docker Compose via `INITIAL_ADMIN_*` variables

### Android Development

When using the Android emulator, use this API base URL:
```
http://10.0.2.2:4000
```

---

## Production Readiness

### Configuration Files

Production deployment uses a separate configuration:
- **Docker Compose**: `docker-compose.prod.yml`
- **Environment Variables**: `.env` (based on `.env.example`)
- **Backend Profile**: `application-prod.properties`

### Before Production Deployment

1. **Prepare environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Fill `.env` with real production values:**
   - Database credentials (strong, secure passwords)
   - JWT secret (generate a strong random value)
   - SMTP provider configuration (SendGrid, AWS SES, etc.)
   - Firebase service account path
   - Frontend origin and activation link URL
   - API base URLs

3. **Ensure Firebase service account is secured:**
   - Place `firebase-service-account.json` outside Git
   - File is in `.gitignore` and will not be committed
   - Mount securely in production Docker Compose

4. **Set up HTTPS/TLS:**
   - Use a reverse proxy (nginx, Caddy, Traefik)
   - Terminate TLS at the proxy
   - Proxy to backend:4000 and frontend:80 internally

5. **Configure database:**
   - Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` in `.env`
   - Introduce a migration strategy such as Flyway or Liquibase before deploying to production.
   - Run migrations separately during deployment, not on startup
   - Set up regular backups of PostgreSQL volumes

6. **Configure SMTP:**
   - Use a real SMTP provider (not Mailpit)
   - Set `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, credentials in `.env`
   - Test email delivery before production

7. **Secure secrets:**
   - All sensitive values must come from `.env`
   - Use Docker secrets or secure vault for credentials
   - Rotate secrets regularly

### Production Start

```bash
docker compose -f docker-compose.prod.yml up -d
```

### Important Production Notes

- **Database is not exposed publicly** (no port mapping)
- **Mailpit is not included** in production (local dev only)
- **All secrets come from environment variables** (no hardcoding)
- **Hibernate DDL is set to validate** (requires migration strategy)
- **CORS origin is configurable** via `FRONTEND_ORIGIN` variable
- **Firebase, SMTP, and all integrations are environment-driven**

---

## Configuration Reference

See `.env.example` for all available environment variables and their descriptions.

---

## Troubleshooting

### Local Development Issues

**Containers won't start:**
```bash
docker compose down -v
docker compose up --build
```

**Database connection error:**
- Ensure PostgreSQL container is running: `docker ps`
- Check database credentials in `docker-compose.yml`

**API not responding:**
- Verify backend container logs: `docker logs mrrg-backend-1`
- Check database is healthy: `docker logs mrrg-postgres-1`

**Firebase issues:**
- Verify Firebase service account file is present at `backend/firebase-service-account.json`
- Check file permissions are readable

**Email testing:**
- Access Mailpit UI at http://localhost:8025
- Check docker logs for SMTP errors

### Production Issues

**Environment variables not loading:**
- Ensure `.env` file is in the same directory as `docker-compose.prod.yml`
- All required variables must be set
- Use `docker compose -f docker-compose.prod.yml config` to validate

**Database connection fails:**
- Verify `SPRING_DATASOURCE_*` variables are set correctly in `.env`
- Ensure PostgreSQL credentials match in all three places:
  - `POSTGRES_*` variables
  - `SPRING_DATASOURCE_*` variables
  - Database connection string

**Email not sending:**
- Verify SMTP configuration in `.env`
- Check mail server is accessible from production network
- Review backend logs for SMTP errors

**CORS errors:**
- Verify `FRONTEND_ORIGIN` is set correctly in `.env`
- Ensure it matches your production frontend domain
- Backend logs will show CORS rejection details

---

## Additional Resources

- Backend: [Software Architecture](../architecture/Software-Architecture.md)
- Security Model: [Security Model](../architecture/Security.md)
- Administrator Manual: [Administrator Manual](../user/Administrator-Manual.md)
