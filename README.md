# MRRG

Business Management Platform for Margaret River Re-Gutter

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![React](https://img.shields.io/badge/React-Frontend-61DAFB?logo=react)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-Backend-6DB33F?logo=springboot)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql)
[![Backend CI](https://github.com/SlachDevM/MRRG/actions/workflows/backend-ci.yaml/badge.svg?branch=main)](https://github.com/SlachDevM/MRRG/actions)

### Login:  

<img width="1012" height="1140" alt="image" src="https://github.com/user-attachments/assets/3ec89d35-c6ec-40b1-a15e-ba9955a12ab9" />

### Dashboard:  

<img width="2838" height="1508" alt="image" src="https://github.com/user-attachments/assets/47af2e86-5bb0-4d9c-9fab-714d30fc2230" />

---

## Overview

MRRG is a roofing business management platform developed for Margaret River Re-Gutter, a roofing company based in Western Australia.

The application centralizes scheduling, employee assignment, field reporting, manager validation, notifications and job archiving. While built for a real business, it also serves as my portfolio, demonstrating how I design and develop maintainable business software with Java, Spring Boot and React.

The backend is shared by two clients:

- React Web application for managers and administrators
- Android application (Jetpack Compose) for field workers

---

## Design Philosophy

MRRG follows a backend-centric architecture.

Business rules, authorization, workflow validation and data consistency are enforced exclusively by the Spring Boot backend.

The React and Android applications act as specialized clients sharing the same business logic through a common REST API.

---

## Related Projects

MRRG-Mobile – Android companion application for field workers built with Kotlin and Jetpack Compose.

[MRRG-Mobile](https://github.com/SlachDevM/MRRG-Mobile)

---

## Documentation

Additional project documentation is available in the `docs/` directory.

| Document | Description |
|----------|-------------|
| Project Philosophy | Design principles and development philosophy |
| Software Architecture | High-level architecture of the MRRG ecosystem |
| Database Schema | Detailed description of MRRG database |
| Security Model | Security model and authentication strategy |
| Installation Guide | Local development environment setup |
| Maintenance Guide | Guidance for maintaining and extending the project |
| Administrator Manual | Business administration workflows |
| User Manual | Android application guide for field workers |

---

## Business Problem

Before MRRG, job scheduling, communication and progress tracking relied primarily on manual coordination between managers and field workers.

This platform centralizes those processes into a single application.

---

## Key Features

- User invitation and account activation
- Employee scheduling  
- Job assignment management  
- Before/after photo uploads
- Download photos  
- Job lifecycle and validation workflow  
- Archived jobs management  
- Re-open and priority escalation system  
- Real-time notifications  
- Role-based access control  
- Dynamic job status workflow
- Secure account activation workflow
- Offline-first Android support
- Callback management with job restoration

---

## Technology

| Backend | Java 21, Spring Boot, Spring Security |
|----------|---------------------------------------|
| Frontend | React |
| Frontend (Mobile) | Kotlin + Jetpack Compose |
| Database | PostgreSQL |
| Authentication | JWT |
| Containerization | Docker Compose |
| Persistence | Spring Data JPA / Hibernate |
| Architecture | MVC + Service Layer + Repository Pattern |
| Testing | JUnit 5, Mockito |

---

## Deployment Architecture

```text
                         ┌────────────────────────────┐
                         │        PostgreSQL          │
                         │      Persistent Data       │
                         └────────────▲───────────────┘
                                      │
                                      ▼
                         ┌────────────────────────────┐
                         │      Spring Boot API       │
                         │ Business Logic / JWT / FCM │
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

## Technical Decisions

- Layered architecture (Controller → Service → Repository) to keep business logic isolated from REST controllers.
- JWT authentication for stateless API security.
- PostgreSQL to ensure transactional consistency.
- Docker Compose for reproducible development and deployment environments.
- Role-based authorization to enforce business rules.
- User account lifecycle is modeled explicitly with `PENDING_ACTIVATION`, `ACTIVE` and `DISABLED` statuses.
- Stable entity relationships are modeled using JPA associations rather than mutable display values.
- Client applications guide user interactions but never enforce business rules.

---

## Account Activation Workflow

Users are not created through public registration.

All users are created by an administrator from the React web application.

```text
Admin
   │
   ▼
Create User
   │
   ▼
PENDING_ACTIVATION
   │
   ▼
Activation Email
   │
   ▼
Android Deep Link / React Web
   │
   ▼
Choose Password
   │
   ▼
ACTIVE
   │
   ▼
Login
```

---
  
## Business Workflow

```text
Pending
   │
   ▼
Scheduled
   │
   ▼
In Progress
   │
   ▼
Waiting Manager Validation
   │
   ▼
Completed
   │
   ▼
Archived
   ▲
   │
Callback
```

- Managers create roofing jobs.
- Jobs are scheduled and assigned to employees.
- Employees upload at least one before photo, automatically moving the job to In Progress.
- Employees upload at least one after photo before submitting the job for manager validation.
- Managers validate completed jobs.
- Validated jobs are archived.
- Customer callbacks restore archived jobs to the workflow with elevated priority and without assigned workers, allowing managers to choose the most appropriate team for the follow-up work.

---

## Roles

### Admin  
- Full user management
- Create users
- Update users
- Deactivate users
- Reactivate users
- Resend activation emails
- All manager capabilities

### Manager   
- Create and update jobs
- Schedule work
- Assign employees
- Validate completed jobs
- Archive and restore jobs

### Employee 
- View active work schedule
- View details of assigned jobs
- Upload before/after photos
- Add job notes
- Mark assigned jobs as completed

---

## Notifications

Managers are notified when employees complete assigned jobs.

Employees are notified when:
- jobs are assigned;
- schedules change;
- important job updates occur.

---

## Installation

```bash
git clone https://github.com/SlachDevM/MRRG.git

cd MRRG

docker compose up --build -d
```

After starting the application:

- Frontend: `http://localhost:3000`
- Swagger UI: `http://localhost:4000/swagger-ui/index.html`

---

## API Documentation

<img width="2844" height="1504" alt="image" src="https://github.com/user-attachments/assets/c97309c7-3d80-4515-a9c0-842f98f80fe2" />

---

## Testing

Backend services are extensively covered with unit tests using JUnit 5 and Mockito.

More than 160 automated tests validate authentication, user lifecycle, business workflows, notifications and security rules.

Tested areas include:
- Authentication
- Job workflow
- Notifications
- Users
- User management
- Account activation

---

## Additional screenshots 
### Job Management 

<img width="1244" height="1498" alt="image" src="https://github.com/user-attachments/assets/3ffd18d2-9cee-4aa4-8eff-304891b4b015" />
<img width="1252" height="748" alt="image" src="https://github.com/user-attachments/assets/597f8859-3009-486e-ba8a-f8314f3d5fda" />
<img width="1254" height="1512" alt="image" src="https://github.com/user-attachments/assets/5e168306-3399-44d0-91d7-2e204a1d5795" />

### Scheduling

<img width="2786" height="914" alt="image" src="https://github.com/user-attachments/assets/76aea401-1154-41c7-9039-da9eff38b95e" />

### Notifications 

<img width="2802" height="1518" alt="image" src="https://github.com/user-attachments/assets/2cd77d84-d5ea-46ba-bffc-45cf38de9652" />
<img width="2834" height="1520" alt="image" src="https://github.com/user-attachments/assets/2e0cde92-b1bd-48ba-9bcc-469d3b2dad62" />

### Validation 

<img width="1242" height="1496" alt="image" src="https://github.com/user-attachments/assets/bdabb9ee-f46b-4ee6-9d16-2b3dd8708a37" />
<img width="480" height="526" alt="image" src="https://github.com/user-attachments/assets/ef45badd-fec5-443f-ae7c-cbec30dd3cf1" />
<img width="2852" height="858" alt="image" src="https://github.com/user-attachments/assets/d6e601fd-8cfb-4fde-a07d-12372702b3d7" />
<img width="1238" height="1504" alt="image" src="https://github.com/user-attachments/assets/47bfbb5d-ffea-401b-a0e6-08fa03a28f95" />

### Administration 

<img width="2838" height="1152" alt="image" src="https://github.com/user-attachments/assets/f31df653-4294-4a9c-8b62-226504c00c70" />
<img width="2844" height="1024" alt="image" src="https://github.com/user-attachments/assets/67ead378-f46d-4cdb-9ab8-f4d45d9a4d6d" />

### User Management

<img width="1124" height="958" alt="image" src="https://github.com/user-attachments/assets/faa8a2fb-56d6-467a-9efe-ab18c0b0bbae" />
<img width="2840" height="768" alt="image" src="https://github.com/user-attachments/assets/0dd2bfa1-b138-4ce0-a371-9c4846b92a6e" />
<img width="1478" height="994" alt="image" src="https://github.com/user-attachments/assets/c5d47bfb-aea1-47aa-9269-9b86e766da00" />
<img width="972" height="1092" alt="image" src="https://github.com/user-attachments/assets/ac250514-5ea3-4abc-930b-7f07146f44ea" />
<img width="974" height="816" alt="image" src="https://github.com/user-attachments/assets/38130dc0-a196-4a9c-858a-7eb6a7b1407f" />
<img width="2844" height="786" alt="image" src="https://github.com/user-attachments/assets/2a596428-b6de-40be-a7c3-b1ed679dde78" />

### Archive / Callback

<img width="1250" height="1494" alt="image" src="https://github.com/user-attachments/assets/5a6c07cb-66c9-4b38-a5d6-6b7425c775b0" />
<img width="2702" height="874" alt="image" src="https://github.com/user-attachments/assets/1625b3a1-5683-46ad-871a-a9aa1ac5eec7" />
<img width="2822" height="1454" alt="image" src="https://github.com/user-attachments/assets/cc7d770a-cc77-4904-8007-b5ec89e1c0db" />

---

## License

This repository is publicly available for demonstration and portfolio purposes.

The MRRG application and its business logic were developed for the Margaret River Re-Gutter business and remain the intellectual property of Margaret River Re-Gutter.

Source code is published to showcase software engineering practices and architecture, but redistribution or commercial use of the application is not permitted without permission.
