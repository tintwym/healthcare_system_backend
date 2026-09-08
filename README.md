# Medicore Backend (Spring Boot)

Patient-first REST API — Java 21 + Spring Boot 3.4 + JPA + JWT + **Neon** Postgres.

Port **4110**. Render deploy uses **Docker** (Render has no native Java) — see [`RENDER.md`](RENDER.md). Local run needs only Maven + Neon.

## Requirements

- JDK 21+
- Maven wrapper (`./mvnw`) or Maven 3.9+
- Neon (or any Postgres) — configure in `.env`

## Configure

```bash
cd backend
cp .env.example .env
# set DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD (Neon JDBC)
```

### Neon

```env
DATABASE_URL=jdbc:postgresql://ep-xxxx-pooler.region.aws.neon.tech/neondb?sslmode=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=your-password
```

## Run locally

```bash
./mvnw spring-boot:run
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
