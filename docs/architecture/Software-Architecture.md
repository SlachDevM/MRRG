# Software Architecture

## Overview

MRRG is composed of a single backend shared by two client applications.

The backend owns the business rules and is responsible for data consistency, authentication, authorization and notification management.

Each client focuses on a specific audience while relying on the same REST API.

```text
                         ┌────────────────────────────┐
                         │        PostgreSQL          │
                         │      Persistent Data       │
                         └────────────▲───────────────┘
                                      │
                                      ▼
                         ┌────────────────────────────┐
                         │      Spring Boot API       │
                         │ Business Rules / Security  │
                         └──────────┬───────┬─────────┘
                                    │       │
                             REST API│       │REST API
                                    │       │
                    ┌───────────────▼──┐   ┌▼─────────────────┐
                    │   MRRG-Mobile    │   │ React Web Client │
                    │  Field Workers   │   │ Managers/Admins  │
                    └─────────┬────────┘   └──────────────────┘
                              │
                              ▼
                    ┌──────────────────────┐
                    │ Firebase Cloud       │
                    │ Messaging            │
                    └─────────▲────────────┘
                              │
                    Notification Delivery
```

---

## System Components

### Spring Boot Backend

The backend is responsible for every business decision within the system.

It validates requests, enforces permissions, manages business workflows, coordinates data persistence and creates notifications.

Neither client contains business logic that could lead to inconsistent behaviour.

---

### React Web Application

The React application is used by managers and administrators.

It provides tools to manage users, schedule work, validate completed jobs and manage day-to-day operations.

---

### Android Application

MRRG-Mobile is used by field workers.

It provides access to assigned jobs, allows photo uploads, supports offline work and receives push notifications.

The application remains lightweight by delegating business decisions to the backend.

---

### PostgreSQL

PostgreSQL is the system of record for all persistent business data.

Both clients access the same information through the backend, ensuring a single and consistent source of data.

---

### Firebase Cloud Messaging

Firebase Cloud Messaging is used exclusively to deliver push notifications.

Notifications are first persisted by the backend before being sent to Android devices.

If delivery fails, the notification remains available inside the application.

---

## System Responsibilities

| Component | Responsibility |
|-----------|----------------|
| Backend | Business rules, authentication, persistence, notifications |
| React | Administration and business management |
| Android | Field operations and offline access |
| PostgreSQL | Persistent storage |
| Firebase | Push notification delivery |

---

## Business Data Flow

All business operations follow the same principle.

```text
User Action

   ↓

REST API

   ↓

Authentication & Authorization

   ↓

Business Validation

   ↓

Database

   ↓

Response

   ↓

Client Update
```

Because every request passes through the backend, both clients always follow the same business rules.

---

## Notification Flow

Notifications originate from business events.

```text
Business Event

↓

Backend Business Logic

↓

Persist Notification

↓

Send FCM

↓

Android Device
```

The backend remains the owner of the notification lifecycle.

Firebase only delivers notifications and does not store application data.

---

## Architectural Principles

The architecture is guided by a small number of principles.

- A single backend owns every business rule.
- Both clients share the same REST API.
- PostgreSQL is the only persistent data store.
- Android may cache data locally, but the backend always remains authoritative.
- Push notifications are persisted before delivery.
- Business behaviour remains identical regardless of the client being used.
- Client applications never become sources of business truth.
- Stable identifiers are used internally for relationships and permissions rather than mutable display values.
