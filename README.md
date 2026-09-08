# Medicore Backend (Spring Boot)

Patient-first REST API — Java 21 + Spring Boot 3.4 + JPA + JWT + Postgres/Neon.

Port **4110** (same as the previous Node API so web/mobile clients keep working).

## Requirements

- JDK 21+
- Maven 3.9+ (or use the wrapper once generated)
- Postgres (local Docker on `5433` or Neon)

## Configure

```bash
cd backend
cp .env.example .env   # or export env vars
```

### Local Docker Postgres

```env
DATABASE_URL=jdbc:postgresql://127.0.0.1:5433/medicore
DATABASE_USERNAME=medicore
DATABASE_PASSWORD=medicore
```

### Neon

Use the **JDBC** connection string from the Neon console:

```env
DATABASE_URL=jdbc:postgresql://ep-xxxx-pooler.region.aws.neon.tech/neondb?sslmode=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=your-password
```

Spring reads standard env vars / `application.yml`. You can also pass:

```bash
export DATABASE_URL='jdbc:postgresql://...'
export DATABASE_USERNAME='...'
export DATABASE_PASSWORD='...'
./mvnw spring-boot:run
```

## Run with Docker

From the repo root:

```bash
docker compose up --build -d
```

- API: http://127.0.0.1:4110/health  
- Postgres: localhost:5433  

Logs: `docker compose logs -f backend`  
Stop: `docker compose down`

## Run locally (without Docker for the app)

```bash
cd backend
./mvnw spring-boot:run
# or: mvn spring-boot:run
```

Health: `GET http://127.0.0.1:4110/health` → `{ "ok": true, "service": "medicore-backend" }`

On first boot the seeder creates demo users if the DB is empty.

### Seed accounts

| Role | Email | Password |
|------|-------|----------|
| Patient | thiri.supyae@gmail.com | patient123 |
| Staff | aye.myatthu@medicore.mm, khin.sandar@medicore.mm, … | staff123 |

## API routes

- `POST /auth/login` · `GET /auth/me` · `POST /auth/logout`
- `GET/PATCH /patients/me`
- `GET/POST /appointments` · `GET /appointments/slots` · `PATCH /appointments/{id}` · `POST /appointments/{id}/check-in`
- `GET/POST /messages` · `POST /messages/{id}/reply` · `POST /messages/{id}/read`
- `GET /billing/invoices` · `POST /billing/invoices/{id}/pay` · `POST .../checkout`
- `POST /refills` · `GET /refills/pharmacy/queue`
- `GET/POST /vitals`
- `GET /fhir/Patient/{id}` · `POST /fhir/Bundle`
