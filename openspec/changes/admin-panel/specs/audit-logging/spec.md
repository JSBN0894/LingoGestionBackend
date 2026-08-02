# Audit Logging Specification

## Purpose

Defines the audit logging capability for tracking all administrative actions across the system with asynchronous, non-blocking persistence.

## Requirements

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
