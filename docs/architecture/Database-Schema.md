# MRRG Database Schema

## Purpose

This document describes the current MRRG PostgreSQL database schema.

It is based on the schema exported from DBeaver during the pre-v1.0 release audit.

PostgreSQL is the system of record for MRRG. React and Android clients must never be treated as authoritative sources of business truth.

---

# Overview

The MRRG database currently contains the following core tables:

* `users`
* `jobs`
* `job_assignments`
* `notifications`
* `account_activation_tokens`

The database supports:

* user lifecycle management;
* account activation;
* job creation and workflow;
* worker assignments;
* notifications;
* Android push token storage;
* job history and traceability.

---

# Entity Relationship Summary

```text
users
  ├── account_activation_tokens
  ├── job_assignments
  └── notifications

jobs
  ├── job_assignments
  └── notifications
```

Main relationships:

```text
users.id 1 ─── * account_activation_tokens.user_id

users.id 1 ─── * job_assignments.user_id
jobs.id  1 ─── * job_assignments.job_id

users.id 1 ─── * notifications.user_id
jobs.id  1 ─── * notifications.job_id
```

---

# Tables

## users

Stores MRRG user accounts.

Used by:

* administrators;
* managers;
* employees;
* authentication;
* role-based authorization;
* Android FCM token storage.

### Columns

| Column       |           Type | Nullable | Description                            |
| ------------ | -------------: | -------: | -------------------------------------- |
| `id`         |    `bigserial` |       No | Primary key                            |
| `created_at` |       `bigint` |       No | Creation timestamp                     |
| `updated_at` |       `bigint` |      Yes | Last update timestamp                  |
| `email`      | `varchar(255)` |       No | User email address                     |
| `password`   | `varchar(255)` |       No | Hashed password                        |
| `name`       | `varchar(255)` |       No | Display name                           |
| `role`       | `varchar(255)` |       No | User role                              |
| `enabled`    |      `boolean` |       No | Whether the user is enabled            |
| `fcm_token`  |         `text` |      Yes | Android Firebase Cloud Messaging token |

### Constraints

```text
PRIMARY KEY (id)
UNIQUE (email)
CHECK role IN ('EMPLOYEE', 'MANAGER', 'ADMIN')
```

### Notes

User status is computed from:

* `enabled`;
* account activation state;
* activation token state.

Business statuses are:

```text
PENDING_ACTIVATION
ACTIVE
DISABLED
```

Disabled users cannot log in and must not appear in assignable worker lists.

---

## jobs

Stores jobs created and managed by managers/admins.

Used by:

* scheduling;
* job workflow;
* field reporting;
* photos;
* notes;
* callback/archive workflow.

### Columns

| Column             |           Type | Nullable | Description                                 |
| ------------------ | -------------: | -------: | ------------------------------------------- |
| `id`               |    `bigserial` |       No | Primary key                                 |
| `created_at`       |       `bigint` |       No | Creation timestamp                          |
| `updated_at`       |       `bigint` |      Yes | Last update timestamp                       |
| `created_by`       |       `bigint` |      Yes | Creator user id, currently stored as raw id |
| `client_name`      | `varchar(255)` |       No | Customer name                               |
| `client_address`   | `varchar(255)` |       No | Customer address                            |
| `client_phone`     | `varchar(255)` |       No | Customer phone                              |
| `details`          |         `text` |      Yes | Job details                                 |
| `notes`            |         `text` |      Yes | Job notes                                   |
| `job_types`        |         `text` |       No | Job type information                        |
| `priority_level`   |      `integer` |       No | Job priority                                |
| `status`           | `varchar(255)` |       No | Current job status                          |
| `job_date`         |         `date` |      Yes | Business job date                           |
| `job_start_hour`   | `varchar(255)` |      Yes | Scheduled start time                        |
| `before_photos`    |         `text` |      Yes | Serialized before-photo data                |
| `after_photos`     |         `text` |      Yes | Serialized after-photo data                 |
| `assigned_workers` |         `text` |      Yes | Compatibility field for assigned worker ids |

### Constraints

```text
PRIMARY KEY (id)

CHECK status IN (
  'PENDING',
  'SCHEDULED',
  'IN_PROGRESS',
  'READY_FOR_CONFIRMATION',
  'DONE',
  'TO_BE_FIXED',
  'ARCHIVED'
)
```

### Notes

`job_date` is a PostgreSQL `date`, not a timestamp. This is important because business job dates must not shift because of timezone conversion.

`assigned_workers` is retained for API compatibility, but the authoritative assignment model is `job_assignments`.

---

## job_assignments

Stores the relationship between jobs and assigned workers.

This table is the authoritative source of worker assignments.

### Columns

| Column    |        Type | Nullable | Description     |
| --------- | ----------: | -------: | --------------- |
| `id`      | `bigserial` |       No | Primary key     |
| `job_id`  |    `bigint` |       No | Assigned job    |
| `user_id` |    `bigint` |       No | Assigned worker |

### Constraints

```text
PRIMARY KEY (id)
UNIQUE (job_id, user_id)

FOREIGN KEY (job_id) REFERENCES jobs(id)
FOREIGN KEY (user_id) REFERENCES users(id)
```

### Notes

Assignments use stable user ids, not display names.

This avoids historical issues where worker names changing could corrupt assignment history.

A job may be scheduled with zero workers. This is valid business behavior.

---

## notifications

Stores backend-created notification history.

Firebase Cloud Messaging is only a delivery mechanism. Notifications remain stored in the backend even if FCM delivery fails.

### Columns

| Column       |           Type | Nullable | Description                     |
| ------------ | -------------: | -------: | ------------------------------- |
| `id`         |    `bigserial` |       No | Primary key                     |
| `created_at` |       `bigint` |       No | Notification creation timestamp |
| `is_read`    |      `boolean` |       No | Read/unread state               |
| `user_id`    |       `bigint` |       No | Notification owner              |
| `job_id`     |       `bigint` |       No | Related job                     |
| `type`       | `varchar(255)` |       No | Notification type               |
| `message`    |         `text` |      Yes | Notification message            |

### Constraints

```text
PRIMARY KEY (id)

FOREIGN KEY (user_id) REFERENCES users(id)
FOREIGN KEY (job_id) REFERENCES jobs(id)

CHECK type IN (
  'JOB_ASSIGNED',
  'JOB_RESCHEDULED',
  'JOB_READY_FOR_CONFIRMATION',
  'JOB_CONFIRMED'
)
```

### Notes

Notification ownership is security-sensitive.

A user may only read or mark as read their own notifications.

---

## account_activation_tokens

Stores account activation tokens.

A user may have multiple activation tokens over time.

### Columns

| Column       |        Type | Nullable | Description                       |
| ------------ | ----------: | -------: | --------------------------------- |
| `id`         | `bigserial` |       No | Primary key                       |
| `created_at` |    `bigint` |       No | Token creation timestamp          |
| `expires_at` |    `bigint` |       No | Expiration timestamp              |
| `used_at`    |    `bigint` |      Yes | Timestamp when token was consumed |
| `token`      |      `text` |       No | Activation token                  |
| `user_id`    |    `bigint` |       No | Related user                      |

### Constraints

```text
PRIMARY KEY (id)
UNIQUE (token)

FOREIGN KEY (user_id) REFERENCES users(id)
```

### Notes

Activation tokens must remain linked to users through a many-to-one relationship.

Do not model activation tokens as one-to-one with users.

---

# Sequences

The schema uses PostgreSQL sequences for primary keys:

```text
users_id_seq
jobs_id_seq
job_assignments_id_seq
notifications_id_seq
account_activation_tokens_id_seq
```

These sequences back the `bigserial` primary keys.

---

# Important Business Rules Reflected in the Schema

## Stable Worker Assignment

Worker assignment is represented by:

```text
jobs
  └── job_assignments
        └── users
```

The system must not rely on mutable names as assignment source of truth.

---

## Job Status Workflow

Valid job statuses are enforced by a database check constraint:

```text
PENDING
SCHEDULED
IN_PROGRESS
READY_FOR_CONFIRMATION
DONE
TO_BE_FIXED
ARCHIVED
```

Workflow transitions are enforced by backend business logic, not by the database.

---

## User Roles

Valid roles are enforced by a database check constraint:

```text
EMPLOYEE
MANAGER
ADMIN
```

Authorization is enforced by the backend.

---

## Notification Ownership

Notifications are tied to a single user through `notifications.user_id`.

The backend must always enforce ownership before allowing notification updates.

---

## Activation Token History

Activation tokens are stored separately from users.

This allows:

* new activation links;
* expired token tracking;
* used token tracking;
* reactivation flows for never-activated users.

---

# Production Notes

For production:

* PostgreSQL is the system of record;
* automatic schema modification must not run at startup;
* Hibernate should use `ddl-auto=validate`;
* schema changes should be handled through explicit migrations;
* future production evolution should use Flyway or Liquibase.

---

# Test Database Reset

For manual test campaigns, the database may be reset while keeping the initial admin user.

Recommended reset order:

```sql
DELETE FROM public.notifications;
DELETE FROM public.job_assignments;
DELETE FROM public.account_activation_tokens;
DELETE FROM public.jobs;
DELETE FROM public.users WHERE id <> 1;

UPDATE public.users
SET fcm_token = NULL
WHERE id = 1;

SELECT setval('public.jobs_id_seq', 1, false);
SELECT setval('public.job_assignments_id_seq', 1, false);
SELECT setval('public.notifications_id_seq', 1, false);
SELECT setval('public.account_activation_tokens_id_seq', 1, false);
SELECT setval('public.users_id_seq', 1, true);
```

This preserves:

```text
users.id = 1
```

and removes all test data.

---

# Known Compatibility Fields

## jobs.assigned_workers

`jobs.assigned_workers` is retained as a compatibility field.

The authoritative assignment model is:

```text
job_assignments.job_id
job_assignments.user_id
```

Do not introduce new logic that treats `assigned_workers` as the source of truth.