# Frontend Pages Specification

## Purpose

Defines all admin panel pages, their functionality, and the shared application layout.

## Requirements

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
