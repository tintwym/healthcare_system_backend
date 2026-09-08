# Deploy Medicore API on Render (no Docker)

Repo: `healthcare_system_backend` (this folder is the git root).

## Render settings (manual or Blueprint)

| Setting | Value |
|---------|--------|
| Language / Runtime | **Java** (not Docker) |
| Branch | `development_v1` |
| Build command | `chmod +x mvnw && ./mvnw -B -DskipTests package` |
| Start command | `java -XX:MaxRAMPercentage=75.0 -jar target/medicore-backend-0.1.0.jar` |
| Health check | `/health` |

## Environment

```
DATABASE_URL=jdbc:postgresql://ep-floral-violet-b3rn442u-pooler.c-4.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=<neon-password>
CORS_ORIGIN=https://medicore-health-mm.vercel.app
APP_URL=https://medicore-health-mm.vercel.app
JWT_SECRET=<long-random>
PORT=4110
```

## Vercel

`VITE_API_URL=https://<your-service>.onrender.com` → redeploy web.

## Local run (also no Docker)

```bash
# uses backend/.env (Neon)
./mvnw spring-boot:run
```
