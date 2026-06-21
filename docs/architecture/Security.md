# Security Model

## Overview

MRRG follows a centralized security model where every business operation is validated by the backend.

Neither the React web application nor the Android application is trusted to enforce permissions or business rules. Client applications authenticate users, submit requests and display responses, while the backend remains responsible for every security decision.

This approach guarantees consistent behaviour across the entire MRRG ecosystem.

---

## Authentication

MRRG uses JWT authentication for both client applications.

After a successful login, the backend issues a JSON Web Token that is included in subsequent requests.

The same authentication mechanism is used by both the React web application and the Android application, providing a consistent security model regardless of the client being used.

---

## Authorization

Every protected request is authorized by the backend.

Permissions are determined from the authenticated user's role and current account status before any business operation is executed.

Because authorization is enforced centrally, business behaviour remains identical across every client application.

---

## User Lifecycle

User accounts are managed exclusively by administrators.

There is no public registration process. Every account is created as part of an administrative business process.

New users follow the activation workflow below.

```text
Administrator

↓

Create User

↓

PENDING_ACTIVATION

↓

Activation Email

↓

Android Deep Link

↓

Choose Password

↓

ACTIVE

↓

Login
```

Accounts can be disabled at any time without deleting business data, ensuring that historical records remain intact while preventing future authentication.

---

## Business Validation

Every business operation is validated by the backend before application data is modified.

Examples include:

* validating user permissions;
* enforcing job workflow transitions;
* controlling account activation;
* protecting administrative operations.

Client applications are responsible only for presenting information and collecting user input.

---

## Notifications

Notifications are created and stored by the backend before being delivered to Android devices.

Firebase Cloud Messaging is used exclusively as a delivery mechanism.

If a notification cannot be delivered immediately, it remains available through the application's notification history.

---

## Security by Design

Security has been considered as part of the system architecture rather than as a separate feature.

The following design decisions contribute to the overall security of the application.

* Passwords are never stored in plain text.
* User authentication is performed exclusively by the backend.
* Authorization decisions are never delegated to client applications.
* Administrative operations require an authenticated user with the appropriate role.
* User accounts must be activated before authentication is permitted.
* Business rules are enforced server-side for every request.
* Notifications are persisted before being delivered, preventing data loss if push delivery fails.

These principles reduce the risk of inconsistent behaviour between clients while ensuring that sensitive business operations remain under backend control.

---

## Security Principles

The security model of MRRG is based on a small number of consistent principles.

* Authentication is centralized.
* Authorization is enforced by the backend.
* Business rules are never trusted to client applications.
* User accounts are administrator-managed.
* Business data remains protected regardless of the client platform.
