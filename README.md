# LogiVault Backend

REST API for a shop's warehouse inventory: items, variants, prices, stock and sales orders. It refuses to sell stock the shop does not have, even under concurrent sales, and keeps a full history of every stock change.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Spring Data JPA · Flyway · Spring Security + JWT · MapStruct · Lombok · springdoc (OpenAPI / Swagger UI) · JUnit 5 + Testcontainers · JaCoCo

---

## In plain words / Penjelasan sederhana
*No technical background needed for this section. / Bagian ini bisa dibaca tanpa latar belakang IT.*

<details>
<summary><b>🇬🇧 English</b></summary>

**The problem.** Many shops track stock in notebooks or spreadsheets. The numbers drift from what is really on the shelf, items get sold that are already gone, and when stock goes missing nobody can tell who changed it or why.

**What LogiVault does.** It is the "brain" behind a shop's inventory. Other apps, such as a cashier (POS) app or an admin panel, connect to it to:
- **Record products** and their variants, for example a T-shirt in size M black and size L white, each with its own price and stock.
- **Receive stock** when a supplier delivers goods.
- **Sell.** An order reduces stock right away. If even one item is short, the whole order is refused, so the shop never sells what it does not have, even when two cashiers sell the last unit at the same second.
- **Cancel a sale** and put the items back into stock, with a reason.
- **Show the full history** of every stock change: who did it, when, why, and how much stock there was before and after.
- **Warn about low stock** with a list of products that need restocking.
- **Control access.** The owner (ADMIN) manages products, stock corrections and staff accounts; staff (STAFF) sell and receive stock.

**What it is not.** LogiVault has no screens of its own. It is the engine; a cashier app or admin panel would be the dashboard built on top of it.

**About the name.** *LogiVault* = **Logistics** + **Vault** (a safe place to keep valuables): a logistics system that is secure, accurate and keeps reliable records of every stock movement.

</details>

<details>
<summary><b>🇮🇩 Bahasa Indonesia</b></summary>

**Masalahnya.** Banyak toko mencatat stok di buku atau spreadsheet. Angkanya sering tidak cocok dengan barang di rak, barang yang sudah habis tetap terjual, dan kalau stok hilang tidak ada yang tahu siapa yang mengubahnya atau kenapa.

**Apa yang dilakukan LogiVault.** LogiVault adalah "otak" pengelolaan stok toko. Aplikasi lain, misalnya aplikasi kasir (POS) atau panel admin, terhubung ke LogiVault untuk:
- **Mencatat produk** beserta variannya, misalnya kaos ukuran M hitam dan ukuran L putih, masing-masing dengan harga dan stok sendiri.
- **Menerima stok** saat barang dari supplier datang.
- **Menjual.** Pesanan langsung mengurangi stok. Kalau ada satu barang saja yang kurang, seluruh pesanan ditolak. Toko tidak akan pernah menjual barang yang tidak ada, bahkan saat dua kasir menjual barang terakhir di detik yang sama.
- **Membatalkan penjualan** dan mengembalikan barang ke stok, dengan alasan yang wajib diisi.
- **Menampilkan riwayat lengkap** setiap perubahan stok: siapa, kapan, kenapa, serta jumlah stok sebelum dan sesudahnya.
- **Memberi peringatan stok menipis** berupa daftar produk yang perlu diisi ulang.
- **Mengatur hak akses.** Pemilik (ADMIN) mengelola produk, koreksi stok, dan akun karyawan; karyawan (STAFF) menjual dan menerima stok.

**Yang bukan LogiVault.** LogiVault tidak punya tampilan layar sendiri. Ia adalah mesinnya; aplikasi kasir atau panel admin adalah tampilan yang dibangun di atasnya.

**Arti nama.** *LogiVault* = **Logistics** (logistik) + **Vault** (brankas, tempat penyimpanan aman): sistem logistik yang aman, akurat, dan menjaga keakuratan data dalam setiap pergerakan stok.

</details>

### Glossary / Glosarium
| Term / Istilah | English | Bahasa Indonesia |
|---|---|---|
| API | A way for apps to talk to each other. A cashier app sends a request ("sell 2 shirts") and LogiVault answers. | Cara aplikasi saling berkomunikasi. Aplikasi kasir mengirim permintaan ("jual 2 kaos") dan LogiVault menjawab. |
| Endpoint | One specific "door" of the API, e.g. the door for creating an order. LogiVault has 32. | Satu "pintu" tertentu di API, misalnya pintu untuk membuat pesanan. LogiVault punya 32. |
| Backend | The part of a system that runs behind the scenes and stores the data. | Bagian sistem yang bekerja di balik layar dan menyimpan data. |
| Database | Where all data is stored permanently (products, stock, orders, users). LogiVault uses PostgreSQL. | Tempat semua data disimpan permanen (produk, stok, pesanan, pengguna). LogiVault memakai PostgreSQL. |
| Docker | A tool that runs software (here, the database) in a ready-made box, so nobody has to install it by hand. | Alat untuk menjalankan software (di sini, database) dalam "kotak" siap pakai, tanpa perlu instal manual. |
| Terminal | The text window where commands like `docker compose up` are typed. | Jendela teks tempat mengetik perintah seperti `docker compose up`. |
| Token (JWT) | A digital pass received after logging in, shown with every request so the system knows who you are. | Tiket digital yang didapat setelah login, ditunjukkan di setiap permintaan supaya sistem tahu siapa Anda. |
| `.env` file | A small settings file holding passwords and secrets for your own computer. Never shared. | File pengaturan kecil berisi password dan rahasia untuk komputer Anda sendiri. Tidak boleh dibagikan. |
| SKU | A unique product code, e.g. `KAOS-M-HITAM`. | Kode unik produk, misalnya `KAOS-M-HITAM`. |
| Variant | One version of a product (size, color) with its own price and stock. | Satu versi produk (ukuran, warna) dengan harga dan stok sendiri. |
| Stock movement | One recorded change in stock: stock in, sale, cancel or correction. | Satu catatan perubahan stok: barang masuk, penjualan, pembatalan, atau koreksi. |
| Swagger UI | A web page listing every endpoint where you can try the API by clicking. | Halaman web berisi daftar semua endpoint, tempat mencoba API dengan klik. |
| ADMIN / STAFF | The two user roles: owner (full access) and employee (sell and receive stock). | Dua peran pengguna: pemilik (akses penuh) dan karyawan (menjual dan menerima stok). |
| Test | An automatic check that the system behaves correctly. LogiVault has 240. | Pemeriksaan otomatis bahwa sistem bekerja dengan benar. LogiVault punya 240. |

*The rest of this document is technical and written for developers. / Bagian selanjutnya bersifat teknis dan ditujukan untuk developer.*

---

## Contents
1. [How to run the application](#1-how-to-run-the-application)
2. [Design decisions](#2-design-decisions)
3. [Assumptions](#3-assumptions)
4. [API endpoints](#4-api-endpoints)
5. [API examples](#5-api-examples)
6. [Tests and coverage](#6-tests-and-coverage)
7. [Project structure](#7-project-structure)

---

## 1. How to run the application

### Prerequisites
| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| Docker Desktop | any recent, running | `docker ps` |

Maven is not required: the project ships the Maven wrapper (`./mvnw`).

### Steps
```bash
# 1. Get the code
git clone git@github.com:septalonika/LogiVault-backend.git
cd LogiVault-backend

# 2. Create and fill in .env (see "Setting up .env" below)
cp .env.example .env

# 3. Start PostgreSQL 16 (host port 5433)
docker compose up -d db

# 4. Run the API (dev profile by default, reads .env automatically)
./mvnw spring-boot:run
```

### Setting up `.env`
`.env` holds the local configuration and secrets. It is read by **both** Docker Compose (to create the database) and the app (via `spring.config.import`), so one file configures everything. It is git-ignored; never commit it.

**1. Copy the template**
```bash
cp .env.example .env
```

**2. Generate a JWT secret**

The app signs login tokens with this key. It must be Base64 and at least 32 bytes, so generate it instead of typing one:
```bash
openssl rand -base64 48
# e.g. 3q2+7w9kZ0x1...   (64 characters)
```
Open `.env` and paste the output after `JWT_SECRET=`. Or let a command do it:
```bash
# macOS
sed -i '' "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|" .env
# Linux / WSL / Git Bash
sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48)|" .env
```

> **Replace `JWT_SECRET` before the first run.** The placeholder in `.env.example` is not valid Base64, so the app will not start until it is replaced.

**3. Choose a database password**

Set `DB_PASSWORD` to any value. Docker Compose uses it to create the database user, and the app uses it to connect.
The password is applied only when the database volume is created for the first time. To change it later, reset the database with `docker compose down -v` (deletes all data).

**4. Choose the first admin account**

Set `LOGIVAULT_ADMIN_EMAIL` and `LOGIVAULT_ADMIN_PASSWORD` (use at least 8 characters). This account is created once, on the very first start, and is the one you log in with. Other users are created later through `POST /api/v1/users`.

**5. Leave the rest as is**

`DB_HOST`, `DB_PORT`, `DB_NAME` and `DB_USERNAME` already match `docker-compose.yml`. Change `DB_PORT` only if port 5433 is taken. `LOGIVAULT_CORS_ALLOWED_ORIGINS` can stay empty unless a browser app on another origin calls the API.

**Result:** a finished `.env` looks like this (your secret and passwords will differ):
```properties
DB_HOST=localhost
DB_PORT=5433
DB_NAME=logivault
DB_USERNAME=logivault
DB_PASSWORD=my-local-db-pass

JWT_SECRET=3q2+7w9kZ0x1bWFrZS15b3VyLW93bi1zZWNyZXQtd2l0aC1vcGVuc3NsLXJhbmQtNDg=

LOGIVAULT_ADMIN_EMAIL=admin@logivault.local
LOGIVAULT_ADMIN_PASSWORD=Admin12345

LOGIVAULT_CORS_ALLOWED_ORIGINS=
```

Quick check that no placeholder is left (should print `0`):
```bash
grep -c "change-me" .env
```

### Setting up Docker (database)
Docker runs **only PostgreSQL**. The API itself runs on your machine with `./mvnw`. The integration tests also need Docker, because Testcontainers starts its own throwaway PostgreSQL.

**1. Install and start Docker**

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) (macOS / Windows) or Docker Engine with the Compose plugin (Linux), then start it. Check:
```bash
docker version            # shows both Client and Server; "Cannot connect" means Docker is not running
docker compose version    # Compose v2 (the "docker compose" command, with a space)
```

**2. Start the database**

Run from the project folder, after `.env` is filled in (Compose reads `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` and `DB_PORT` from it):
```bash
docker compose up -d db
```
The first run downloads the `postgres:16-alpine` image, creates the `logivault-db` container and a named volume for the data.

**3. Wait until it is healthy**
```bash
docker compose ps
# NAME           IMAGE                STATUS
# logivault-db   postgres:16-alpine   Up 10 seconds (healthy)
```
Start the app only after the status shows `(healthy)`; that usually takes a few seconds.

**4. (Optional) Look inside the database**
```bash
docker compose exec db psql -U logivault -d logivault
# \dt                         list tables
# select email, role from users;
# \q                          quit
```
Any SQL client works too: host `localhost`, port `5433`, database `logivault`, user `logivault`, password from `DB_PASSWORD`.

**Everyday commands**
| Task | Command | Data |
|---|---|---|
| Start the database | `docker compose up -d db` | kept |
| Stop it (e.g. end of the day) | `docker compose stop` | kept |
| Show logs | `docker compose logs -f db` | kept |
| Remove the container | `docker compose down` | kept (volume stays) |
| **Reset everything** (fresh schema and admin seed on next app start) | `docker compose down -v` | **deleted** |

The app does not create the database container for you: if the app fails with `Connection to localhost:5433 refused`, the database is not running. Run `docker compose up -d db` and wait for `(healthy)`.

On first start, Flyway creates the schema (`V1__init_schema.sql`) and seeds the first ADMIN account from `LOGIVAULT_ADMIN_EMAIL` / `LOGIVAULT_ADMIN_PASSWORD` (`V2__SeedAdmin`).

Check that it is up:
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

| URL | What |
|---|---|
| http://localhost:8080/api/v1 | API base |
| http://localhost:8080/swagger-ui.html | Swagger UI (dev profile only) |
| http://localhost:8080/v3/api-docs | OpenAPI 3.1 spec (dev profile only) |
| http://localhost:8080/actuator/health | Health check |

### Environment variables
| Name | Default | Purpose |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5433` / `logivault` | Database location |
| `DB_USERNAME` / `DB_PASSWORD` | `logivault` / – | Database credentials (required) |
| `JWT_SECRET` | – | Base64 HMAC key, at least 32 bytes (required) |
| `LOGIVAULT_ADMIN_EMAIL` / `LOGIVAULT_ADMIN_PASSWORD` | – | First ADMIN account, created on first start |
| `LOGIVAULT_CORS_ALLOWED_ORIGINS` | empty | Comma-separated allowed origins; empty disables CORS |

### Profiles
| Profile | Use | Differences |
|---|---|---|
| `dev` (default) | Local development | Swagger UI and `/v3/api-docs` enabled, SQL logging |
| `prod` | Deployment | API docs disabled, only `/actuator/health` exposed, no health details |
| `test` | Automated tests | Used by Testcontainers-based integration tests |

### Using Swagger UI
1. Open http://localhost:8080/swagger-ui.html.
2. Expand **Auth → POST /api/v1/auth/login**, click **Try it out**, send the admin email and password.
3. Copy `data.accessToken` from the response, click **Authorize** (top right) and paste it.
4. Every endpoint with a lock icon now sends `Authorization: Bearer <token>`.

![Swagger UI overview](docs/screenshots/swagger-overview.png)

### Troubleshooting
| Symptom | Fix |
|---|---|
| App fails on start with `Illegal base64 character` or `must decode to at least 32 bytes` | `JWT_SECRET` still has the placeholder. Replace it with the output of `openssl rand -base64 48`. |
| `docker compose up` fails with `port is already allocated` on 5433 | Another PostgreSQL uses that port. Set `DB_PORT` in `.env` to a free port (e.g. `5434`); both Docker Compose and the app read it. |
| `docker compose up` fails with `container name "/logivault-db" is already in use` | An older LogiVault database container exists. Start it with `docker start logivault-db`, or remove it with `docker rm -f logivault-db` (this deletes the container; the data stays in its volume, listed by `docker volume ls`). |
| App fails with `Port 8080 was already in use` | Run on another port: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. |
| Login with the admin from `.env` returns `401 INVALID_CREDENTIALS` | The admin is seeded only on the very first start. Changing `LOGIVAULT_ADMIN_*` later has no effect. Use the original password, or reset the database with `docker compose down -v` (deletes all data). |
| Tests fail with `Could not find a valid Docker environment` | Start Docker Desktop; integration tests use Testcontainers. |

---

## 2. Design decisions

### Stock correctness
| Decision | Why |
|---|---|
| **One write path for stock.** `StockService.applyMovement` is the only code that changes `Variant.stock`. It requires an existing transaction (`Propagation.MANDATORY`). | Stock-in, adjustments, sales and cancellations all follow the same rules, so the ledger and the stock column can never drift apart. |
| **Pessimistic row locks** (`SELECT ... FOR UPDATE`) on the variants of an order, always taken in a fixed order (sorted by id). | Two cashiers selling the last unit at the same moment must not both succeed. Locking in a fixed order prevents deadlocks when two orders touch the same variants in opposite order (`OrderConcurrencyIT` checks both cases). |
| **All-or-nothing orders.** If any line is short, the whole order fails with `409 INSUFFICIENT_STOCK` and nothing is deducted. The response lists every short line with `requested` and `available`. | A half-fulfilled sale is worse than a failed one at a till. The client can show exactly what is missing. |
| **Immediate deduction, no reservation.** Creating an order is the sale. | Matches an in-store POS flow; reservation and pending orders are out of scope for V1. |
| **Append-only ledger** (`stock_movements`) with type, signed qty, stock before and after, reason, actor and order reference. | Answers "who changed this stock, when, why, and what was it after?". Current stock always equals the replayed sum of the ledger (`LedgerInvariantIT`). |
| **Database `CHECK (stock >= 0)`** as a last line of defense. | Even a bug in the service layer cannot persist negative stock; the violation maps to `INSUFFICIENT_STOCK`. |
| **Cancel restores stock** with a `SALE_CANCEL` movement per line and requires a reason. Orders are never edited. | Keeps the history honest. To fix an order, cancel it and create a new one. |

### Catalog
| Decision | Why |
|---|---|
| Item → Variant model. Price and stock live on the variant; `variant.price` is optional and falls back to `item.basePrice` (`effectivePrice`). | Most variants of a product share a price; only exceptions need their own. |
| An item created without variants gets a `Default` variant with an auto-generated SKU. | Every sellable thing is a variant, so orders and stock only deal with one concept. |
| SKU is unique (case-insensitive) and becomes immutable once stock movements exist (`SKU_IMMUTABLE`). | SKUs printed on labels and referenced in history must not change underneath them. |
| Soft delete (`DELETE` sets `active = false`, `PATCH .../activate` restores). Inactive variants cannot be sold or stocked (`422 VARIANT_INACTIVE`). | Orders and movements keep referencing the variant, so rows are never physically removed. |
| Variant attributes stored as PostgreSQL `JSONB` (`{"size":"M","color":"Hitam"}`). | Flexible per product without extra tables. |

### API and security
| Decision | Why |
|---|---|
| Stateless JWT access tokens (15 min) plus refresh tokens (7 days). Refresh tokens are stored only as SHA-256 hashes and rotated on every refresh; logout revokes them. | Short-lived access tokens limit damage if leaked; a stolen DB dump does not expose usable refresh tokens. |
| Two roles: `ADMIN` (catalog, adjustments, users) and `STAFF` (sell, cancel, stock-in, read). ADMIN rules are enforced as URL rules **and** `@PreAuthorize`. | URL rules run before request validation, so STAFF always gets `403`, never a `400` that leaks the request shape. `@PreAuthorize` is a second layer. `RoleMatrixIT` checks every endpoint for every role (117 cases). |
| Every response uses one envelope: `{ status, message, data }`. Lists add `meta` (paging); errors add a stable `code` (`INSUFFICIENT_STOCK`, `SKU_ALREADY_EXISTS`, ...), plus `errors[]` for validation and `details` for extra context. Security 401/403 return the same shape. Actions without a result return `200` with `data: null` instead of `204`. | Clients parse one shape for every endpoint and branch on `code`, not on English messages. |
| UUID primary keys. | Ids are not guessable and can be generated without a DB round trip. |
| Order codes `ORD-YYYYMMDD-NNNN`, numbered per business day in `Asia/Jakarta`, from a counter table. | Human-readable for receipts; per-day counters avoid gaps from sequences. |
| All timestamps stored in UTC (`Instant`); business dates use `Asia/Jakarta`. Time comes from an injected `Clock`. | Correct "today" boundaries for a shop in Indonesia, and deterministic tests. |
| Lists put the rows in `data` and paging in `meta` (`page`, `size`, `totalElements`, `totalPages`); max page size 100. | Same envelope as single results, and does not leak Spring's `Page` JSON, which changes between versions. |
| Package-by-layer (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, ...) with a feature prefix on class names. | Familiar Spring layout; one feature is still found by name (`Order*`). |
| Schema managed only by Flyway (`ddl-auto: validate`). MapStruct with `unmappedTargetPolicy=ERROR`. | The schema is reviewed SQL, not guessed by Hibernate. A new field that is not mapped fails the build instead of silently returning `null`. |
| OpenAPI generated from code (springdoc) with summaries, error responses and examples; Swagger UI only in `dev`. | Docs cannot go stale, and production does not expose the API surface. |

---

## 3. Assumptions
- One shop, one warehouse, one currency (IDR). Business timezone `Asia/Jakarta`.
- Clients are trusted internal apps (admin panel, POS) on a private or HTTPS network. There is no public sign-up; an ADMIN creates users and resets passwords.
- Stock is counted in whole units. No fractional quantities, batches or expiry dates.
- Payment happens outside LogiVault. A created order means the sale already happened.
- Cancelling an order voids a mistaken sale. Real customer returns and partial refunds are out of scope.
- `minStock` defaults to 5 per variant. A variant is "low stock" when `stock <= minStock`.
- Expected volume for V1: up to 5,000 variants, 1,000 orders per day, 50 concurrent users.
- Both ADMIN and STAFF can see all orders and cancel any order.
- A deactivated user can no longer log in or refresh, but an access token already issued stays valid until it expires (max 15 minutes).
- Login lockout after repeated failures is designed (`429 TOO_MANY_ATTEMPTS`) but deferred; there is no brute-force protection yet.
- Out of scope for V1: multiple warehouses, suppliers and purchase orders, discounts and taxes, notifications, reports and exports, and a frontend.

---

## 4. API endpoints

Base path `/api/v1`. All endpoints except login and refresh require `Authorization: Bearer <accessToken>`.
"Any" means any logged-in user (ADMIN or STAFF).

### Auth
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/auth/login` | public | Log in with email and password |
| POST | `/auth/refresh` | public | Exchange a refresh token for a new token pair |
| POST | `/auth/logout` | Any | Revoke a refresh token |

### Items
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/items` | ADMIN | Create an item (with variants, or a Default variant) |
| GET | `/items?q=&active=&page=&size=&sort=` | Any | List items |
| GET | `/items/{id}` | Any | Item with its variants |
| PUT | `/items/{id}` | ADMIN | Update name, description, base price |
| DELETE | `/items/{id}` | ADMIN | Deactivate an item |
| PATCH | `/items/{id}/activate` | ADMIN | Reactivate an item |
| GET | `/items/{id}/variants` | Any | List the variants of an item |
| POST | `/items/{id}/variants` | ADMIN | Add a variant |

### Variants
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/variants/{id}` | Any | Variant detail |
| GET | `/variants/sku/{sku}` | Any | Lookup by SKU (case-insensitive) |
| PUT | `/variants/{id}` | ADMIN | Update a variant |
| DELETE | `/variants/{id}` | ADMIN | Deactivate a variant |
| PATCH | `/variants/{id}/activate` | ADMIN | Reactivate a variant |

### Stock
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/variants/{id}/stock-in` | Any | Receive stock |
| POST | `/variants/{id}/adjust` | ADMIN | Manual adjustment (signed `delta`, reason required) |
| GET | `/variants/{id}/movements?type=&from=&to=` | Any | Stock movement history |
| GET | `/stock/low` | Any | Variants at or below their minimum stock |

### Orders
| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/orders` | Any | Create an order and deduct stock (all or nothing) |
| POST | `/orders/{id}/cancel` | Any | Cancel an order and restore stock |
| GET | `/orders?status=&from=&to=&createdBy=` | Any | List orders |
| GET | `/orders/{id}` | Any | Order with its lines |

### Users
| Method | Path | Role | Description |
|---|---|---|---|
| GET | `/users/me` | Any | Own profile |
| PUT | `/users/me/password` | Any | Change own password |
| GET | `/users?q=&role=&active=` | ADMIN | List users |
| POST | `/users` | ADMIN | Create a user |
| GET | `/users/{id}` | ADMIN | Get a user |
| PUT | `/users/{id}` | ADMIN | Update name and role |
| PATCH | `/users/{id}/status` | ADMIN | Activate or deactivate (not yourself) |
| POST | `/users/{id}/reset-password` | ADMIN | Reset a user's password |

### Error codes
| HTTP | `code` |
|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_OLD_PASSWORD` |
| 401 | `UNAUTHORIZED`, `INVALID_CREDENTIALS`, `INVALID_REFRESH_TOKEN` |
| 403 | `FORBIDDEN` |
| 404 | `USER_NOT_FOUND`, `ITEM_NOT_FOUND`, `VARIANT_NOT_FOUND`, `ORDER_NOT_FOUND` |
| 409 | `EMAIL_ALREADY_EXISTS`, `SKU_ALREADY_EXISTS`, `SKU_IMMUTABLE`, `INSUFFICIENT_STOCK`, `INVALID_ORDER_STATUS`, `SELF_MODIFICATION_NOT_ALLOWED`, `CONCURRENT_MODIFICATION` |
| 422 | `VARIANT_INACTIVE` |
| 429 | `TOO_MANY_ATTEMPTS` (reserved for login lockout) |
| 500 | `INTERNAL_ERROR` |

---

## 5. API examples

### Response format
Every response, success or error, uses the same envelope:

| Field | When | Meaning |
|---|---|---|
| `status` | always | HTTP status code, repeated in the body |
| `message` | always | Short human-readable result, e.g. `Item created`, `Not enough stock` |
| `data` | always | The result; an object, an array for lists, or `null` for errors and actions without a result |
| `meta` | lists only | Paging: `page`, `size`, `totalElements`, `totalPages` |
| `code` | errors only | Stable machine-readable error code, e.g. `INSUFFICIENT_STOCK` |
| `errors` | validation errors only | Field errors: `[{ "field", "message" }]` |
| `details` | some errors | Extra context, e.g. the short lines of `INSUFFICIENT_STOCK` |

The examples below are real responses captured from a local run, following the main flow: **login → create item → stock-in → order → insufficient stock → cancel → history**. Ids will differ on your machine.

```bash
BASE=http://localhost:8080/api/v1
```

### 5.1 Login
```bash
curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@logivault.local","password":"<LOGIVAULT_ADMIN_PASSWORD>"}'
```
`200 OK`
```json
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "<43-char random token>",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": { "id": "a23cc587-dfa5-4ef3-b62a-86de33ca5469", "name": "Administrator", "email": "admin@logivault.local", "role": "ADMIN" }
  }
}
```
```bash
TOKEN=<data.accessToken from the response>
```

### 5.2 Create an item with two variants (ADMIN)
The first variant has no own price, so it uses the item's `basePrice`.
```bash
curl -s -X POST $BASE/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "name": "Kaos Polos",
    "description": "Cotton combed 30s",
    "basePrice": 75000,
    "variants": [
      { "sku": "KAOS-M-HITAM", "name": "M / Hitam", "attributes": {"size":"M","color":"Hitam"}, "minStock": 5 },
      { "sku": "KAOS-L-PUTIH", "name": "L / Putih", "attributes": {"size":"L","color":"Putih"}, "price": 80000, "minStock": 5 }
    ]
  }'
```
`201 Created`, `Location: /api/v1/items/{id}`
```json
{
  "status": 201,
  "message": "Item created",
  "data": {
    "id": "15ac477f-401f-4985-a81e-0101b432bea0",
    "name": "Kaos Polos",
    "description": "Cotton combed 30s",
    "basePrice": 75000,
    "active": true,
    "variants": [
      {
        "id": "3a60ab72-d210-406e-bd44-a4557fe5d155",
        "sku": "KAOS-M-HITAM",
        "name": "M / Hitam",
        "attributes": { "size": "M", "color": "Hitam" },
        "price": null,
        "effectivePrice": 75000,
        "stock": 0,
        "minStock": 5,
        "lowStock": true,
        "active": true
      },
      {
        "id": "86017dec-e97f-444f-8473-8b41a0bf9c32",
        "sku": "KAOS-L-PUTIH",
        "name": "L / Putih",
        "attributes": { "size": "L", "color": "Putih" },
        "price": 80000,
        "effectivePrice": 80000,
        "stock": 0,
        "minStock": 5,
        "lowStock": true,
        "active": true
      }
    ],
    "createdAt": "2026-10-01T16:40:30.497263Z",
    "updatedAt": "2026-10-01T16:40:30.497263Z"
  }
}
```

### 5.3 Stock-in
```bash
VARIANT=3a60ab72-d210-406e-bd44-a4557fe5d155
curl -s -X POST $BASE/variants/$VARIANT/stock-in \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"qty":50,"note":"Supplier delivery"}'
```
`200 OK`
```json
{
  "status": 200,
  "message": "Stock received",
  "data": {
    "variantId": "3a60ab72-d210-406e-bd44-a4557fe5d155",
    "sku": "KAOS-M-HITAM",
    "stock": 50,
    "movement": {
      "id": "1949b824-c311-453e-a259-abfffb7bd582",
      "type": "STOCK_IN",
      "qty": 50,
      "stockBefore": 0,
      "stockAfter": 50,
      "reason": "Supplier delivery",
      "orderCode": null,
      "actor": { "id": "a23cc587-dfa5-4ef3-b62a-86de33ca5469", "name": "Administrator" },
      "createdAt": "2026-10-01T16:40:30.544890Z"
    }
  }
}
```

### 5.4 Create an order
```bash
curl -s -X POST $BASE/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"lines":[{"variantId":"'$VARIANT'","qty":2}],"note":"Pickup at 3pm"}'
```
`201 Created`, `Location: /api/v1/orders/{id}`
```json
{
  "status": 201,
  "message": "Order created",
  "data": {
    "id": "1bd78192-4e23-49e9-824b-228048f32e70",
    "code": "ORD-20261001-0002",
    "status": "COMPLETED",
    "total": 150000.0,
    "note": "Pickup at 3pm",
    "lines": [
      {
        "variantId": "3a60ab72-d210-406e-bd44-a4557fe5d155",
        "sku": "KAOS-M-HITAM",
        "itemName": "Kaos Polos",
        "variantName": "M / Hitam",
        "qty": 2,
        "unitPrice": 75000.0,
        "subtotal": 150000.0
      }
    ],
    "createdBy": { "id": "a23cc587-dfa5-4ef3-b62a-86de33ca5469", "name": "Administrator" },
    "createdAt": "2026-10-01T16:40:30.594331Z",
    "cancelledBy": null,
    "cancelledAt": null,
    "cancelReason": null
  }
}
```
Note that `status` at the top is the HTTP status (`201`), while `data.status` is the order status (`COMPLETED`).

### 5.5 Order with insufficient stock
Stock is now 48. Asking for 999 fails, and nothing is deducted.
```bash
curl -s -X POST $BASE/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"lines":[{"variantId":"'$VARIANT'","qty":999}]}'
```
`409 Conflict`
```json
{
  "status": 409,
  "message": "Not enough stock",
  "code": "INSUFFICIENT_STOCK",
  "data": null,
  "details": {
    "lines": [
      { "variantId": "3a60ab72-d210-406e-bd44-a4557fe5d155", "sku": "KAOS-M-HITAM", "requested": 999, "available": 48 }
    ]
  }
}
```

### 5.6 Cancel the order
```bash
ORDER=1bd78192-4e23-49e9-824b-228048f32e70
curl -s -X POST $BASE/orders/$ORDER/cancel \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"reason":"Customer changed their mind"}'
```
`200 OK` (abridged)
```json
{
  "status": 200,
  "message": "Order cancelled",
  "data": {
    "id": "1bd78192-4e23-49e9-824b-228048f32e70",
    "code": "ORD-20261001-0002",
    "status": "CANCELLED",
    "total": 150000.0,
    "cancelledBy": { "id": "a23cc587-dfa5-4ef3-b62a-86de33ca5469", "name": "Administrator" },
    "cancelledAt": "2026-10-01T16:40:30.657208Z",
    "cancelReason": "Customer changed their mind"
  }
}
```

### 5.7 Stock movement history
Newest first. Each row shows who, why, and the stock before and after.
```bash
curl -s "$BASE/variants/$VARIANT/movements?size=5" -H "Authorization: Bearer $TOKEN"
```
`200 OK` (actor ids abridged)
```json
{
  "status": 200,
  "message": "Stock movements retrieved",
  "data": [
    { "type": "SALE_CANCEL", "qty": 2,  "stockBefore": 48, "stockAfter": 50, "reason": "Customer changed their mind", "orderCode": "ORD-20261001-0002", "actor": { "name": "Administrator" }, "createdAt": "2026-10-01T16:40:30.663947Z" },
    { "type": "SALE",        "qty": -2, "stockBefore": 50, "stockAfter": 48, "reason": null,                          "orderCode": "ORD-20261001-0002", "actor": { "name": "Administrator" }, "createdAt": "2026-10-01T16:40:30.596116Z" },
    { "type": "STOCK_IN",    "qty": 50, "stockBefore": 0,  "stockAfter": 50, "reason": "Supplier delivery",           "orderCode": null,                "actor": { "name": "Administrator" }, "createdAt": "2026-10-01T16:40:30.544890Z" }
  ],
  "meta": { "page": 0, "size": 5, "totalElements": 3, "totalPages": 1 }
}
```

### 5.8 Low-stock list
```bash
curl -s "$BASE/stock/low" -H "Authorization: Bearer $TOKEN"
```
`200 OK`
```json
{
  "status": 200,
  "message": "Low-stock variants retrieved",
  "data": [
    { "variantId": "86017dec-e97f-444f-8473-8b41a0bf9c32", "sku": "KAOS-L-PUTIH", "itemName": "Kaos Polos", "variantName": "L / Putih", "stock": 0, "minStock": 5 }
  ],
  "meta": { "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
}
```

### 5.9 Action without a result
Deactivate, logout, change password and reset password return `200` with `data: null`:
```json
{ "status": 200, "message": "Variant deactivated", "data": null }
```

### 5.10 Error shapes
No token, `401 Unauthorized`:
```json
{ "status": 401, "message": "Authentication is required", "code": "UNAUTHORIZED", "data": null }
```

Invalid body, `400 Bad Request`:
```bash
curl -s -X POST $BASE/items -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"","basePrice":-1}'
```
```json
{
  "status": 400,
  "message": "Request validation failed",
  "code": "VALIDATION_ERROR",
  "data": null,
  "errors": [
    { "field": "basePrice", "message": "must be greater than or equal to 0" },
    { "field": "name", "message": "must not be blank" }
  ]
}
```

---

## 6. Tests and coverage

```bash
./mvnw test                       # unit + integration tests (Docker must be running)
./mvnw clean verify               # same, plus the JaCoCo coverage gate
./mvnw test -Dtest=OrderConcurrencyIT
```

- `*Test` classes are plain unit tests (no Spring context).
- `*IT` classes start the full app against a real PostgreSQL 16 in Testcontainers and call the API through MockMvc. Both run in `./mvnw test`.
- `./mvnw verify` fails the build if any `*Service` class drops below 70% line coverage. The HTML report is written to `target/site/jacoco/index.html`.

### Latest result
`./mvnw clean verify` on 2026-10-01: **240 tests, 0 failures, 0 errors, 0 skipped. BUILD SUCCESS. All coverage checks met.**

| Test class | Tests | What it proves |
|---|---:|---|
| `security.RoleMatrixIT` | 117 | Every endpoint × (no token, STAFF, STAFF with invalid body, ADMIN) returns the expected 401 / 403 / allowed status |
| `controller.CreateOrderIT` | 10 | Stock deduction with one SALE movement, price snapshot, insufficient stock changes nothing, STAFF can sell |
| `controller.UserManagementIT` | 9 | Create, update, deactivate, reset password, self-modification guard |
| `controller.ItemReadUpdateIT` | 8 | Item list, search, detail, update, deactivate and reactivate |
| `exception.GlobalExceptionHandlerTest` | 8 | Exception → error envelope mapping and error codes |
| `controller.CancelOrderIT` | 6 | Cancel restores stock with a SALE_CANCEL movement, also for inactive variants; STAFF can cancel |
| `controller.CreateItemIT` | 6 | Item with variants, Default variant, duplicate SKU |
| `controller.OrderQueryIT` | 6 | Order list filters and detail |
| `controller.StockHistoryIT` | 6 | History filters, ordering and paging |
| `controller.StockInAdjustIT` | 6 | Stock-in and adjustment rules, negative result rejected |
| `controller.VariantIT` | 6 | Lowercase SKU lookup, update, deactivate/reactivate, STAFF forbidden |
| `controller.AuthIT` | 5 | Login, refresh rotation, logout, invalid credentials |
| `entity.VariantTest` | 5 | Effective price, low-stock and orderable rules |
| `security.JwtServiceTest` | 5 | Token round trip; rejects expired, wrong-secret and garbage tokens |
| `controller.UserProfileIT` | 4 | Own profile and password change |
| `security.SecurityConfigIT` | 4 | Public vs protected endpoints, bad tokens |
| `config.OpenApiIT` | 4 | All 32 endpoints documented, bearer scheme, public endpoints, envelope schemas |
| `controller.LowStockIT` | 3 | Includes stock at `minStock`, most critical first, skips inactive variants and items |
| `repository.UserRepositoryIT` | 3 | Admin seed, case-insensitive email lookup and uniqueness |
| `repository.VariantRepositoryIT` | 3 | JSONB attributes round trip, SKU lookup, variant ordering |
| `service.OrderCodeGeneratorIT` | 3 | Per-day order code sequence |
| `service.OrderConcurrencyIT` | 3 | **20 parallel orders for the last unit → exactly 1 succeeds**; all-or-nothing rollback; no deadlock on opposite line order |
| `service.StockServiceTest` | 3 | `applyMovement` rules (unit) |
| `security.ProdProfileIT` | 2 | API docs disabled in `prod` |
| `service.StockLockingIT` | 2 | Row locks serialize concurrent stock changes |
| `SmokeIT` | 1 | Application context starts, migrations apply |
| `dto.PageResponseTest` | 1 | Paging shape |
| `service.LedgerInvariantIT` | 1 | Current stock always equals the sum of the ledger |
| **Total (28 classes)** | **240** | |

### Coverage (JaCoCo)
Service layer: 96% instructions, 85% branches, 364 of 372 lines covered.

![JaCoCo service coverage](docs/screenshots/jacoco-service.png)

Whole project:

![JaCoCo report](docs/screenshots/jacoco-report.png)

---

## 7. Project structure
```
src/main/java/com/logivault/
├── config/        Security, OpenAPI, JPA auditing, Clock, typed properties
├── controller/    REST controllers (Auth, Item, Variant, Stock, Order, User)
├── service/       Business logic; StockService.applyMovement is the only stock writer
├── repository/    Spring Data repositories, row-lock queries, specifications
├── entity/        JPA entities and enums (BaseEntity, Item, Variant, StockMovement, Order, ...)
├── dto/           Request/response records per feature (dto.order, dto.stock, ...)
├── mapper/        MapStruct mappers
├── security/      JWT filter and service, refresh token hashing, 401/403 handlers
├── exception/     ErrorCode, BusinessException, GlobalExceptionHandler
├── db/migration/  Java Flyway migrations (admin seed)
└── util/          SKU normalizer
src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
└── db/migration/  SQL Flyway migrations
src/test/java/com/logivault/
├── support/       AbstractIntegrationTest (shared Testcontainer), TestDataFactory
└── ...            Tests mirror the main packages
```
