# SDD Delta Specs — admin-panel

## Domain: security

### Requirement: Role-Based Endpoint Access

The system MUST enforce role-based access control on all business write endpoints. Non-ADMIN users MUST receive 403 Forbidden on Swagger documentation endpoints.

| Role | Products CUD | Categories CUD | Orders CU/D | Customers CU/D | Shipments CU/D |
|------|-------------|----------------|-------------|----------------|----------------|
| ADMIN | Yes | Yes | Yes | Yes | Yes |
| PRODUCCION | Yes | No | No | No | No |
| VENTAS | Yes | No | CU only | CU only | No |
| LOGISTICA | No | No | No | No | CU only |

#### Scenario: ADMIN accesses Swagger

- GIVEN a user with role ADMIN
- WHEN they request any Swagger endpoint
- THEN the response is 200 with documentation

#### Scenario: Non-ADMIN accesses Swagger

- GIVEN a user with role VENTAS
- WHEN they request any Swagger endpoint
- THEN the response is 403 Forbidden

#### Scenario: PRODUCCION creates product

- GIVEN a user with role PRODUCCION
- WHEN they POST to /api/products
- THEN the request succeeds with 201

#### Scenario: VENTAS deletes category

- GIVEN a user with role VENTAS
- WHEN they DELETE to /api/categories/{id}
- THEN the response is 403 Forbidden

#### Scenario: LOGISTICA updates shipment

- GIVEN a user with role LOGISTICA
- WHEN they PATCH to /api/shipments/{id}
- THEN the request succeeds with 200

#### Scenario: Unauthenticated write request

- GIVEN no valid JWT token
- WHEN any write endpoint is called
- THEN the response is 401 Unauthorized

### Requirement: User Profile and Status Management

The system MUST expose the authenticated user's profile via GET /api/auth/me with fields { id, username, email, fullName, role, enabled }. ADMIN users MUST be able to toggle user enabled status via PATCH /api/admin/users/{id}/status.

#### Scenario: Get current user profile

- GIVEN an authenticated user
- WHEN they GET /api/auth/me
- THEN the response contains id, username, email, fullName, role, enabled

#### Scenario: ADMIN toggles user status

- GIVEN an ADMIN user and a target user ID
- WHEN they PATCH /api/admin/users/{id}/status
- THEN the target user's enabled field is toggled

#### Scenario: Non-ADMIN toggles user status

- GIVEN a user with role VENTAS
- WHEN they PATCH /api/admin/users/{id}/status
- THEN the response is 403 Forbidden

### Requirement: Security Headers

The system MUST include security headers on all responses: Strict-Transport-Security, X-Frame-Options: DENY, X-Content-Type-Options: nosniff, Content-Security-Policy: default-src 'self'. CORS MUST allow http://localhost:5173 in development mode.

#### Scenario: Security headers present

- GIVEN any HTTP response
- WHEN headers are inspected
- THEN HSTS, X-Frame-Options, X-Content-Type-Options, and CSP headers are present

#### Scenario: CORS allows dev origin

- GIVEN the application runs in dev mode
- WHEN a request originates from http://localhost:5173
- THEN the CORS Access-Control-Allow-Origin header includes that origin

---

## Domain: audit-logging

### Requirement: Audit Log Data Model

The system MUST persist audit entries with fields: id (UUID), username, action, entity_type, entity_id, old_values (JSONB), new_values (JSONB), ip_address, timestamp.

#### Scenario: Audit entry schema

- GIVEN a new audit entry is created
- WHEN persisted to the database
- THEN it contains all required fields with correct types

### Requirement: @Audited Annotation and AOP Capture

The system MUST provide an @Audited annotation that marks methods for audit capture. An AOP aspect MUST capture before/after state on annotated methods. Audit logging MUST be asynchronous and MUST NOT block the main request.

#### Scenario: Annotated method triggers audit

- GIVEN a method annotated with @Audited
- WHEN the method executes
- THEN an audit entry is created asynchronously

#### Scenario: Audit does not block request

- GIVEN an @Audited method with slow audit persistence
- WHEN the method is called
- THEN the HTTP response returns before audit write completes

### Requirement: Audit Coverage

Audit entries MUST be created for: user CRUD, role changes, status changes, product CRUD, category CRUD, order CRUD, customer CRUD, shipment CRUD.

#### Scenario: User creation audited

- GIVEN an ADMIN creates a new user
- WHEN the operation completes
- THEN an audit entry records action=CREATE, entity_type=USER, new_values contain user data

#### Scenario: Role change audited

- GIVEN an ADMIN changes a user's role
- WHEN the operation completes
- THEN an audit entry records action=UPDATE, old_values and new_values contain the role change

#### Scenario: Product deletion audited

- GIVEN an ADMIN deletes a product
- WHEN the operation completes
- THEN an audit entry records action=DELETE, entity_type=PRODUCT, old_values contain product data

### Requirement: Audit Query API

The system MUST expose GET /api/admin/audit returning paginated audit entries. This endpoint MUST be restricted to ADMIN users only.

#### Scenario: ADMIN queries audit log

- GIVEN an ADMIN user
- WHEN they GET /api/admin/audit?page=0&size=20
- THEN paginated audit entries are returned

#### Scenario: Non-ADMIN queries audit log

- GIVEN a user with role VENTAS
- WHEN they GET /api/admin/audit
- THEN the response is 403 Forbidden

---

## Domain: web-auth

### Requirement: Cookie-Based Authentication

The system MUST provide POST /api/web/login accepting { username, password } and returning httpOnly cookies: access_token (15min TTL), refresh_token (7d TTL), csrf_token. POST /api/web/logout MUST revoke refresh tokens and clear cookies. POST /api/web/refresh MUST rotate both tokens via httpOnly cookie.

#### Scenario: Successful login

- GIVEN valid credentials
- WHEN POST /api/web/login is called
- THEN httpOnly cookies for access_token, refresh_token, and csrf_token are set

#### Scenario: Invalid login

- GIVEN invalid credentials
- WHEN POST /api/web/login is called
- THEN the response is 401 and no cookies are set

#### Scenario: Logout clears tokens

- GIVEN an authenticated session
- WHEN POST /api/web/logout is called
- THEN refresh tokens are revoked and cookies are cleared

#### Scenario: Token rotation

- GIVEN a valid refresh_token cookie
- WHEN POST /api/web/refresh is called
- THEN new access_token and refresh_token cookies are set and old refresh_token is revoked

### Requirement: Cookie JWT Filter

The system MUST provide a CookieJwtFilter that reads JWT from the access_token cookie for paths matching /api/web/**. A separate SecurityFilterChain MUST exist for web paths vs API paths.

#### Scenario: Cookie JWT authenticates web request

- GIVEN a valid access_token cookie
- WHEN a request is made to /api/web/dashboard
- THEN the user is authenticated via the cookie JWT

#### Scenario: API path not affected by cookie filter

- GIVEN a Bearer token in Authorization header
- WHEN a request is made to /api/products
- THEN authentication uses the Bearer token, not cookies

### Requirement: CSRF Protection

The system MUST enforce CSRF double-submit pattern on /api/web/** mutation endpoints: csrf_token cookie value MUST match X-CSRF-Token request header.

#### Scenario: Valid CSRF token

- GIVEN a valid csrf_token cookie
- WHEN a mutation request includes matching X-CSRF-Token header
- THEN the request proceeds

#### Scenario: Missing CSRF token

- GIVEN a valid csrf_token cookie
- WHEN a mutation request omits X-CSRF-Token header
- THEN the response is 403 Forbidden

#### Scenario: Mismatched CSRF token

- GIVEN a valid csrf_token cookie
- WHEN X-CSRF-Token header contains a different value
- THEN the response is 403 Forbidden

### Requirement: SPA Forward Controller

The system MUST serve index.html from static/admin/ for all /admin/** paths that are not API endpoints.

#### Scenario: SPA route served

- GIVEN a request to /admin/dashboard
- WHEN no API route matches
- THEN index.html from static/admin/ is served

---

## Domain: frontend-scaffold

### Requirement: Project Structure and Build

The system MUST provide a Vite + React + TypeScript project in the frontend/ directory. The build output MUST be written to src/main/resources/static/admin/.

#### Scenario: Frontend builds to static directory

- GIVEN the frontend/ project
- WHEN npm run build executes
- THEN output files appear in src/main/resources/static/admin/

### Requirement: Styling and Component Library

The system MUST use Tailwind CSS for styling and shadcn/ui for component primitives.

#### Scenario: Tailwind classes compile

- GIVEN a component using Tailwind utility classes
- WHEN the project builds
- THEN classes are compiled into the output CSS

### Requirement: HTTP Client Configuration

The system MUST provide an Axios instance configured with withCredentials: true for cookie transmission. The instance MUST automatically inject the CSRF token from cookie into the X-CSRF-Token header on mutation requests.

#### Scenario: Credentials sent with requests

- GIVEN the configured Axios instance
- WHEN any request is made
- THEN cookies are included via withCredentials: true

#### Scenario: CSRF header auto-injected

- GIVEN a valid csrf_token cookie
- WHEN a POST/PUT/PATCH/DELETE request is made
- THEN the X-CSRF-Token header is set from the cookie value

### Requirement: Authentication Context

The system MUST provide an Auth context with login/logout/refresh functions and role-checking capability. A ProtectedRoute component MUST redirect unauthenticated users to /admin/login. A RoleGuard component MUST restrict route access by user role.

#### Scenario: Unauthenticated user redirected

- GIVEN no valid session
- WHEN accessing a ProtectedRoute
- THEN the user is redirected to /admin/login

#### Scenario: Insufficient role blocked

- GIVEN a user with role VENTAS
- WHEN accessing a route guarded for ADMIN
- THEN access is denied

### Requirement: Session Timeout

The system MUST redirect to /admin/login after 15 minutes of inactivity.

#### Scenario: Inactivity timeout

- GIVEN an authenticated user idle for 15 minutes
- WHEN they attempt any action
- THEN they are redirected to /admin/login

---

## Domain: frontend-pages

### Requirement: Login Page

The system MUST provide a login page with username/password form, error display, and rate-limit feedback.

#### Scenario: Successful login redirects

- GIVEN valid credentials entered
- WHEN the form is submitted
- THEN the user is redirected to /admin/dashboard

#### Scenario: Rate limit feedback

- GIVEN too many failed login attempts
- WHEN the login form is submitted
- THEN a rate-limit error message is displayed

### Requirement: Dashboard Page

The system MUST display total orders, revenue, pending shipments, and recent activity.

#### Scenario: Dashboard loads metrics

- GIVEN an authenticated ADMIN
- WHEN they visit /admin/dashboard
- THEN KPI cards and recent activity are displayed

### Requirement: Entity Management Pages

The system MUST provide CRUD pages for: Users (list, create, edit, delete, enable/disable, role change), Products (list with pagination, create, edit, delete, image upload), Categories (list, create, edit, delete), Customers (list, create, edit, delete), Orders (list with pagination, view detail, create, edit), Shipments (list, create, edit, assign guide), States (list, create, edit, delete).

#### Scenario: User list with actions

- GIVEN an ADMIN on /admin/users
- WHEN the page loads
- THEN a table of users with enable/disable and role-change actions is displayed

#### Scenario: Product image upload

- GIVEN an ADMIN creating a product
- WHEN they attach an image file
- THEN the image is uploaded and previewed

#### Scenario: Order detail view

- GIVEN an ADMIN on /admin/orders
- WHEN they click an order
- THEN the order detail page with line items is displayed

### Requirement: Audit Log Page

The system MUST provide a paginated audit log table with filters for user, action, entity type, and date range.

#### Scenario: Filtered audit log

- GIVEN an ADMIN on /admin/audit
- WHEN they filter by entity_type=USER and date range
- THEN only matching audit entries are displayed

### Requirement: Application Layout

The system MUST provide a layout with sidebar navigation, header showing user info and logout button, and responsive design.

#### Scenario: Responsive sidebar

- GIVEN a viewport width below 768px
- WHEN the admin panel loads
- THEN the sidebar collapses to a hamburger menu

#### Scenario: Header shows user info

- GIVEN an authenticated user
- WHEN any admin page loads
- THEN the header displays username and a logout button
