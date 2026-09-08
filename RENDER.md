# Deploy Medicore API on Render

## Why Docker?

[Render has no native Java runtime](https://render.com/docs/docker).  
JVM apps (Spring Boot) **must** use Docker. Local/dev can still run with `./mvnw` + Neon (no Docker on your Mac).

## Render settings

| Setting | Value |
|---------|--------|
| Repository | `healthcare_system_backend` |
| Branch | `development_v1` |
| Runtime | **Docker** |
| Dockerfile path | `./Dockerfile` |
| Docker context | `.` |
| Health check | `/health` |

Leave Build/Start commands **empty** (Dockerfile handles them).

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

## Push & deploy

```bash
cd backend
git add Dockerfile .dockerignore render.yaml RENDER.md
git commit -m "Restore Dockerfile — required for Render Java"
git push
```

Then Render → **Manual Deploy** (clear build cache once).

## Vercel

`VITE_API_URL=https://<service>.onrender.com` (Config type) → Redeploy web.

## Local (no Docker)

```bash
./mvnw spring-boot:run   # uses .env → Neon
```
