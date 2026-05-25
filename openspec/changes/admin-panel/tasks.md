# Tasks: Admin Panel

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

~1,650 lines | PR 1ÔåÆ2ÔåÆ3ÔåÆ4ÔåÆ5a-5k | ask-on-risk

## Phase 1: Security Hardening (~200L)

- [x] 1.1 SwaggerÔåÆ`hasRole('ADMIN')`; HSTS, X-Frame-Options, CSP; CORS `localhost:5173` ÔÇö mod `SecurityConfig.kt`
- [x] 1.2 ProductController: `@Authenticated` GET, `@ProduccionOnly` CUD
- [x] 1.3 CategoryController: `@Authenticated` GET, `@AdminOnly` CUD
- [x] 1.4 OrderController: `@Authenticated` GET, `@VentasOnly` CU, `@AdminOnly` D
- [x] 1.5 CustomerController: `@Authenticated` GET, `@VentasOnly` CU, `@AdminOnly` D
- [x] 1.6 ShipmentController: `@Authenticated` GET, `@LogisticaOnly` CUD, `@AdminOnly` D
- [x] 1.7 GET `/api/auth/me` ÔåÆ profile ÔÇö `UserMeResponse.kt`, mod `AuthController.kt`
- [x] 1.8 PATCH `/api/admin/users/{id}/status` ÔÇö mod `AdminController.kt`

**Deps**: 1.1 ÔåÆ {1.2-1.8 autonomous}

## Phase 2: Audit Logging (~250L)

- [x] 2.1 Flyway V2: `audit_log` table + indexes ÔÇö `V2__audit_log_table.sql`
- [x] 2.2 `AuditLogEntity` + `AuditLogRepository` (paginated)
- [x] 2.3 `@Audited(action, entityType)` ÔÇö `Audited.kt`
- [x] 2.4 `AuditAspect`: @Before captures args+old state, @AfterReturning publishes, @Async ÔÇö `AuditAspect.kt`, `AuditEvent.kt`
- [x] 2.5 `AuditAsyncConfig`: @EnableAsync, ThreadPoolTaskExecutor (2/4/100)
- [x] 2.6 Apply `@Audited` to admin write endpoints ÔÇö mod Phase 1 controllers
- [x] 2.7 GET `/api/admin/audit` ADMIN-only ÔÇö controller + DTOs

**Deps**: 2.1 ÔåÆ {2.2,2.3} ÔåÆ 2.4 ÔåÆ {2.5,2.6}; 2.2 ÔåÆ 2.7

## Phase 3: Web Auth Flow (~300L)

- [x] 3.1 `WebAuthController`: login (httpOnly cookies), logout, refresh ÔÇö controller + DTOs
- [x] 3.2 `CookieJwtFilter`: JWT from `AUTH_TOKEN` cookie on `/api/web/**`
- [x] 3.3 `WebSecurityConfig` (@Order 1): `/api/web/**`, stateful, CSRF
- [x] 3.4 `ApiSecurityConfig` (@Order 2): `/api/**` excl web, stateless Bearer ÔÇö mod `SecurityConfig.kt`
- [x] 3.5 `CsrfCookieController`: GET `/api/web/csrf-token`
- [x] 3.6 `SpaConfig`: ResourceHandler `/admin/**` ÔåÆ static
- [x] 3.7 `SpaForwardController`: forward `/admin/**` non-file to index.html
- [x] 3.8 CORS `localhost:5173` with credentials ÔÇö mod configs

**Deps**: 3.1 ÔåÆ 3.2 ÔåÆ 3.3 ÔåÆ {3.4,3.5,3.8}; 3.6 ÔåÆ 3.7

## Phase 4: Frontend Scaffold (~200L)

- [x] 4.1 Init Vite+React+TS in `frontend/`: package.json, vite.config.ts, tsconfig, index.html
- [x] 4.2 Tailwind+shadcn: tailwind.config.js, postcss.config.js, src/index.css, components.json, ui/ base components
- [x] 4.3 Axios: withCredentials, base `/api/web`, auto CSRF — `src/lib/api.ts`
- [x] 4.4 AuthContext + ProtectedRoute + RoleGuard — login/logout, role-check, idle timeout
- [x] 4.5 Gradle: `npmBuild` task, `bootJar` dependsOn; .gitignore

**Deps**: 4.1 ÔåÆ {4.2,4.5}; 4.2 ÔåÆ 4.3 ÔåÆ 4.4

## Phase 5: Frontend Pages (chained PRs)

- [x] 5.1 Layout: Sidebar (collapsible), Header (user+logout), AdminLayout
- [x] 5.2 LoginPage: form, errors, rate-limit feedback
- [x] 5.3 DashboardPage: KPI cards, recent activity
- [x] 5.4 UsersPage: table, toggle, role change, CRUD
- [x] 5.5 ProductsPage: paginated, CRUD, image upload
- [x] 5.6 CategoriesPage: list, CRUD
- [x] 5.7 CustomersPage: list, CRUD, search
- [x] 5.8 OrdersPage + OrderDetailPage: paginated, detail with line items
- [x] 5.9 ShipmentsPage: list, CRUD, guide assignment
- [x] 5.10 StatesPage: list, CRUD
- [x] 5.11 AuditLogPage: paginated table, filters
- [x] 5.12 Wire App.tsx + main.tsx: routes, protected routes, role guards

**Deps**: 5.1 ÔåÆ {5.2-5.11 autonomous} ÔåÆ 5.12
