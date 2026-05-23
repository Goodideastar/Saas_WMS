# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WMS (Warehouse Management System) — full-stack SaaS application with a Spring Boot 3.2 / Java 21 backend and Vue 3 / Vite frontend. Manages products, inbound/outbound orders, stock alerts, and analytics dashboards.

不要说废话，直接执行
保证应用的安全运行
不需要看思考过程

## Commands

### Backend (Java / Maven)

```bash
# Build and run
mvn clean package -DskipTests
mvn spring-boot:run

# Run tests
mvn test
mvn test -pl . -Dtest=ProductServiceTest
```

### Frontend (Vue 3)

```bash
cd frontend
npm run dev       # Dev server on port 3000, proxies /api → localhost:8080
npm run build     # Production build
```

### Docker (full stack)

```bash
docker-compose up -d              # Start MySQL + Redis + backend + frontend
docker-compose up -d mysql redis  # Infrastructure only (then run backend/frontend locally)
```

## Architecture

### Backend (`src/main/java/com/wms/`)

Standard layered architecture:

- **`controller/`** — REST endpoints. Each controller corresponds to a business module (Auth, Product, InboundOrder, OutboundOrder, StockAlert, Dashboard).
- **`service/` + `service/impl/`** — Business logic. Service impls extend `ServiceImpl<Mapper, Entity>` from MyBatis Plus. Inbound/outbound order auditing uses `@Transactional` + `selectForUpdate` row-level locking for stock mutations.
- **`mapper/`** — MyBatis Plus mappers. Extend `BaseMapper<T>` for built-in CRUD. Custom queries use `LambdaQueryWrapper` chains.
- **`entity/`** — JPA entities mapped to DB tables. `BaseEntity` provides `id`, `createTime`, `updateTime`, `isDeleted`, `version` with auto-fill via `MetaObjectHandler`.
- **`dto/` + `vo/`** — DTOs for request bodies, VOs for responses. MapStruct is available (but manual conversion is currently used in some places).
- **`config/`** — Spring configuration: `SecurityConfig` (JWT filter chain), `MybatisPlusConfig` (pagination interceptor + auto-fill handler), `RedisConfig`, `RedissonConfig`, `CorsConfig`.
- **`security/`** — `JwtAuthenticationFilter` (extracts Bearer token, loads user permissions from Redis, sets `SecurityContext`), `UserDetailsServiceImpl`, `UserDetailsImpl`.
- **`common/`** — `Result<T>` unified response (code/message/data/timestamp), shared constants.
- **`exception/`** — `BusinessException` + `GlobalExceptionHandler` (@RestControllerAdvice).
- **`aspect/`** — `OperationLogAspect` for AOP-based operation logging.
- **`utils/`** — `JwtUtils` (token generation/validation), `RedisUtil` (wraps StringRedisTemplate operations).

**Key patterns:**
- Order numbers: `IN`/`OUT` + yyyyMMdd + 4-digit sequence (generated in-memory with `LIMIT 1` query, not thread-safe under concurrency).
- Stock mutations happen inside `@Transactional` audit methods. Products use optimistic locking (`version` field) to prevent overselling.
- Stock alert checks (`StockAlertService.checkAndCreateAlerts`) fire after each inbound/outbound audit.
- Scheduled task (`@Scheduled cron="0 0 2 * * ?"`) scans all products at 2 AM for threshold violations.

### Frontend (`frontend/src/`)

- **`api/`** — Axios API modules organized by business domain (auth, product, inbound, outbound, alert, dashboard). All use the shared `request.js` instance.
- **`router/`** — Vue Router with split constant routes (login, 403, 404) and async routes (main layout with children). Route guard checks token, fetches user info/permissions, then allows navigation. Dynamic route registration via `hasAddedRoutes` flag.
- **`store/user.js`** — Pinia store for user state (token, userInfo, permissions, menus). `getUserInfoAction()` fetches user info and permissions from `/api/auth/info`.
- **`utils/request.js`** — Axios instance with request interceptor (adds `Bearer` token) and response interceptor (checks `res.code !== 200`, handles 401/403 by clearing auth and redirecting).
- **`utils/auth.js`** — Token storage helpers (localStorage get/set/remove).
- **`directives/permission.js`** — `v-permission` directive for button-level access control.
- **`components/Layout/MainLayout.vue`** — Classic admin layout (sidebar menu, top nav, tags-view).
- **`views/`** — Page components: Dashboard (ECharts), ProductList, InboundList, OutboundList, AlertList, Login.

### Database

MySQL 8.0 with MyBatis Plus handling ORM. Key tables: `user`, `role`, `permission`, `user_role`, `role_permission`, `product`, `warehouse`, `inbound_order`, `inbound_order_item`, `outbound_order`, `outbound_order_item`, `stock_log`, `stock_alert`, `operation_log`. Logical delete via `is_deleted` column.

### Auth Flow

1. `POST /api/auth/login` → validates credentials → returns JWT `access_token`
2. Frontend stores token in localStorage, attaches via `Authorization: Bearer <token>` header
3. `JwtAuthenticationFilter` extracts token → loads user permissions from Redis (`user:perm:{userId}`) → sets `SecurityContext`
4. Controller methods use `@PreAuthorize("hasAuthority('product:add')")` for fine-grained access control
5. Frontend uses `v-permission="'product:add'"` to hide unauthorized buttons

### Caching

- Spring Cache + Redis for product details, user menus
- Dashboard statistics cached for 1 hour, evicted on stock change
- User permissions cached in Redis (`user:perm:{userId}`) to reduce DB queries on each request

### Infrastructure

`docker-compose.yml` defines: MySQL 8.0 (port 3306), Redis 7 (port 6379), backend (port 8080), frontend with Nginx (port 80). Backend Dockerfile is multi-stage (Maven build → JRE runtime).
