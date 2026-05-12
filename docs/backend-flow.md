# Backend Flow

This document explains the SynapseCore backend as it exists in the repository today: profiles, environment expectations, module structure, API responsibilities, and operational dependencies.

## Backend Stack

- Spring Boot `3.5.0`
- Java `21`
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Data Redis
- Spring Session Redis
- WebSocket / STOMP / SockJS
- Spring Actuator
- Micrometer Prometheus

Main app entry point:

- `backend/src/main/java/com/synapsecore/SynapseCoreApplication.java`

## Module Shape

Top-level backend module lanes:

- `access`
- `alert`
- `api`
- `audit`
- `auth`
- `bootstrap`
- `config`
- `decision`
- `domain`
- `event`
- `fulfillment`
- `integration`
- `intelligence`
- `observability`
- `prediction`
- `realtime`
- `scenario`
- `security`
- `simulation`
- `tenant`

## Profiles

### Default

`application.yml` sets the default profile to:

- `dev`

### `dev`

Purpose:

- local PostgreSQL-backed development
- local Redis-backed development
- starter seeding allowed

Key truths:

- datasource is PostgreSQL-backed
- Redis uses host/port values
- header fallback is allowed by default

### `local`

Purpose:

- fast local demo or smoke flow without a real PostgreSQL dependency

Key truths:

- uses in-memory H2
- `ddl-auto=create-drop`
- Redis health is disabled
- dashboard cache is disabled

### `prod`

Purpose:

- production-like deployment
- Render/live deployment

Key truths:

- datasource is environment-driven
- Redis session store is enabled
- Flyway is enabled
- `ddl-auto=validate`
- header fallback is disabled by default
- starter seeding is disabled by default

## Core Environment Variables

### Core runtime

- `SPRING_PROFILES_ACTIVE`
- `SERVER_ADDRESS`
- `SERVER_PORT`
- `PORT`

### Database

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DATABASE_URL`
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

### Redis / session

- `SPRING_DATA_REDIS_URL`
- `SPRING_SESSION_REDIS_NAMESPACE`
- `REDIS_HOST`
- `REDIS_PORT`

### Frontend/browser trust

- `CORS_ALLOWED_ORIGINS`
- `SESSION_COOKIE_SECURE`
- `SESSION_COOKIE_SAME_SITE`
- `ALLOW_HEADER_FALLBACK`

### Realtime

- `SYNAPSECORE_REALTIME_BROKER_MODE`
- `SYNAPSECORE_REALTIME_REDIS_CHANNEL`
- relay variables for STOMP relay mode

### Build / public fingerprint

- `SYNAPSECORE_BUILD_VERSION`
- `SYNAPSECORE_BUILD_COMMIT`
- `SYNAPSECORE_BUILD_TIME`
- `PUBLIC_APP_URL`
- `PUBLIC_API_URL`

### Operational policy

- `DASHBOARD_CACHE_ENABLED`
- `DISPATCH_INTERVAL_MS`
- `DISPATCH_BATCH_SIZE`
- integration pull worker variables
- inventory retry variables
- security rate-limit variables

## Auth / Session Flow

Primary auth controller:

- `AuthController`

Sign-in path:

- `POST /api/auth/session/login`

Session model:

- validates tenant + user credentials
- resolves the mapped operator identity
- stores session identity
- uses Redis-backed sessions in production

Related trust path:

- `GET /api/auth/session`

Production truth:

- browser sessions should be Redis-backed
- session cookie posture matters for deployed environments
- header fallback should not be relied on in production

## Tenant Enforcement

Tenant separation is central to the backend design:

- requests resolve a tenant/workspace context
- data queries and mutations are tenant-scoped
- realtime topics are tenant-scoped
- operator and role resolution are tenant-aware

This is why hosted proof and runtime issues must always be evaluated with tenant context in mind.

## Controllers / API Surface

Primary controller set under `api/controller`:

- `AccessController`
- `AlertController`
- `AuditController`
- `AuthController`
- `DashboardController`
- `DevToolsController`
- `EventController`
- `FulfillmentController`
- `InventoryController`
- `OperationalPolicyController`
- `OrderController`
- `ProductController`
- `RecommendationController`
- `ScenarioController`
- `ServiceStatusController`
- `SystemController`
- `WarehouseController`

Additional integration endpoints are handled in:

- `integration/ExternalOrderWebhookController`

## Repository / Service Structure

The backend follows a controller -> service -> repository flow:

- controllers own HTTP contracts and response shape
- services own business logic
- repositories own persistence

This pattern powers:

- catalog creation
- inventory mutation
- order and fulfillment updates
- integration replay
- scenario planning and execution
- runtime and incident views

## Product / Catalog APIs

Catalog ownership lives in product and warehouse surfaces.

Representative API areas:

- product create/list/update
- warehouse create/list/update
- catalog CSV import or onboarding flows used by the frontend

The catalog is tenant-owned and feeds order, inventory, and scenario logic.

## Inventory APIs

Representative inventory surfaces:

- `GET /api/inventory`
- `POST /api/inventory/update`

Inventory responsibilities:

- record stock posture
- apply thresholds
- trigger low-stock and depletion logic
- refresh downstream alerts/recommendations/runtime signals

## Orders APIs

Representative order surfaces:

- `POST /api/orders`
- `GET /api/orders/recent`

Order responsibilities:

- validate order shape
- persist order and line items
- open fulfillment task
- reduce inventory
- refresh alerts/recommendations
- record business events and audit signals

## Integration Connector APIs

Integration surfaces include:

- connector management
- webhook ingestion
- CSV import
- recent import visibility
- replay queue inspection
- manual replay

Connector truths:

- connectors can be enabled/disabled
- connector health and limitations are operator-visible
- connector scope is intentionally narrow today

## CSV Import

CSV ingestion is handled through integration services that:

- read tenant-scoped CSV data
- group rows into real orders
- validate and normalize
- either ingest successfully or record replayable failures

## Replay Queue

Replay queue purpose:

- keep failed inbound work visible
- allow safe operational recovery

Representative surface:

- `POST /api/integrations/orders/replay/{replayRecordId}`

Important proof truth:

- manual replay must remain intentional and observable

## Scenario APIs

Scenario system responsibilities include:

- what-if analysis
- comparison
- save
- approval / rejection
- execution
- review queues and escalations

Representative scenario surfaces:

- `/api/scenarios/order-impact`
- `/api/scenarios/order-impact/compare`
- `/api/scenarios/save`
- `/api/scenarios/history`
- `/api/scenarios/{scenarioRunId}/approve`
- `/api/scenarios/{scenarioRunId}/reject`
- `/api/scenarios/{scenarioRunId}/execute`

## Runtime / Health APIs

Runtime and health surfaces include:

- `/`
- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/api/system/runtime`
- `/api/system/incidents`
- `/actuator/prometheus`
- `/ws/info`

Interpretation:

- liveness answers whether the app process is up enough to stay running
- readiness answers whether the app is ready for real traffic, including DB and Redis
- runtime/incidents give more operator-friendly trust posture

## CORS / Security / Rate Limit

The backend has explicit CORS posture through configuration and security filters. Production expectations:

- only allowed browser origins should receive cross-origin access
- session cookies should be compatible with the deployed frontend origin
- sensitive mutation lanes are rate-limited

Rate-limit lanes currently include:

- auth login
- password auth
- tenant onboarding
- access admin mutation
- integration mutation

## Flyway / Migrations

Flyway is part of the production startup contract:

- enabled by default
- baseline on migrate
- validates on migrate
- production schema posture is validate-oriented

If DB connectivity or schema posture is broken, readiness should not be trusted.

## Database Startup Expectations

For a real PostgreSQL-backed startup, the backend expects:

- database reachable
- credentials valid
- schema available
- Flyway migration path valid

If PostgreSQL is down or unreachable:

- startup may hang or fail depending on deployment environment
- readiness should not pass
- other endpoints may time out in hosted environments

## Redis Expectations

For a real production-like startup, the backend expects:

- Redis reachable
- session store available
- realtime pub/sub available when `REDIS_PUBSUB` is used

If Redis is unavailable:

- readiness may fail
- session or realtime posture may degrade or fail

## Backend Bottom Line

The backend is not just a CRUD API layer. It is the operational state, trust, recovery, approval, and realtime engine behind SynapseCore.
