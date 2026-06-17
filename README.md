# MRRG

Business Management Platform for Margaret River Re-Gutter

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![React](https://img.shields.io/badge/React-Frontend-61DAFB?logo=react)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-Backend-6DB33F?logo=springboot)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-4169E1?logo=postgresql)
[![Backend CI](https://github.com/SlachDevM/MRRG/actions/workflows/backend-ci.yaml/badge.svg?branch=main)](https://github.com/SlachDevM/MRRG/actions)

Login :  
<img width="964" height="1256" alt="image" src="https://github.com/user-attachments/assets/f92cc585-5ecc-45de-891a-3712e4793f1d" />

Dashboard :  
<img width="2812" height="1508" alt="image" src="https://github.com/user-attachments/assets/fc0c4112-7c68-4e2f-a8a6-fcdba8dd9fb0" />

MRRG is a roofing business management platform developed for Margaret River Re-Gutter, a roofing company based in Western Australia.

The application centralizes scheduling, employee assignment, field reporting, manager validation, notifications and job archiving. While built for a real business, it also serves as my portfolio, demonstrating how I design and develop maintainable business software with Java, Spring Boot and React.

The backend is shared by two clients:

React Web application for managers and administrators
Android application (Jetpack Compose) for field workers

MRRG-Mobile : https://github.com/SlachDevM/MRRG-Mobile

# Business Problem

Before MRRG, job scheduling, communication and progress tracking relied primarily on manual coordination between managers and field workers.

This platform centralizes those processes into a single application.

# Key Features

- Employee scheduling  
- Job assignment management  
- Task management linked to jobs  
- Before/after photo uploads
- Download photos  
- Job lifecycle and validation workflow  
- Archived jobs management  
- Re-open and priority escalation system  
- Real-time notifications  
- Role-based access control  
- Dynamic job status workflow  

# Technology

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

# Deployment Architecture

```text
                 PostgreSQL
                      ▲
                      │
              Spring Boot API
                JWT / REST API
               ▲             ▲
               │             │
          React Web     Android App
```

# Technical Decisions

- Layered architecture (Controller → Service → Repository) to keep business logic isolated from REST controllers.
- JWT authentication for stateless API security.
- PostgreSQL to ensure transactional consistency.
- Docker Compose for reproducible development and deployment environments.
- Role-based authorization to enforce business rules.
  
# Business Workflow

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
- Employees upload before photos directly from the field.
- Employees complete the work and upload after photos.
- Managers validate completed jobs.
- Validated jobs are archived.
- Customer callbacks automatically restore archived jobs to the scheduling queue with elevated priority.

# Roles

### Admin  
Currently under development.

### Manager   
- Create and update jobs
- Schedule work
- Assign employees
- Validate completed jobs
- Archive and restore jobs

### Employee 
- View assigned work
- Upload before/after photos
- Add job notes
- Mark assigned jobs as completed

# Notifications
- Managers are notified when employees complete assigned jobs.
- Employees are notified when jobs are assigned or rescheduled.

# Installation

```bash
git clone https://github.com/SlachDevM/MRRG.git

cd MRRG

docker compose up --build
```

After starting the application:

- Frontend: `http://localhost:3000`
- Swagger UI: `http://localhost:4000/swagger-ui/index.html`

# API Documentation

<img width="2844" height="1504" alt="image" src="https://github.com/user-attachments/assets/c97309c7-3d80-4515-a9c0-842f98f80fe2" />

# Testing

Backend services are covered with unit tests using JUnit 5 and Mockito.

Tested areas include:
- Authentication
- Job workflow
- Notifications
- Users
- Tasks

# Additional screenshots 
## Job Management 
<img width="1244" height="1498" alt="image" src="https://github.com/user-attachments/assets/3ffd18d2-9cee-4aa4-8eff-304891b4b015" />
<img width="1252" height="748" alt="image" src="https://github.com/user-attachments/assets/597f8859-3009-486e-ba8a-f8314f3d5fda" />
<img width="1254" height="1512" alt="image" src="https://github.com/user-attachments/assets/5e168306-3399-44d0-91d7-2e204a1d5795" />

## Scheduling
<img width="2786" height="914" alt="image" src="https://github.com/user-attachments/assets/76aea401-1154-41c7-9039-da9eff38b95e" />

## Notifications 
<img width="2802" height="1518" alt="image" src="https://github.com/user-attachments/assets/2cd77d84-d5ea-46ba-bffc-45cf38de9652" />
<img width="2834" height="1520" alt="image" src="https://github.com/user-attachments/assets/2e0cde92-b1bd-48ba-9bcc-469d3b2dad62" />

## Validation 
<img width="1242" height="1496" alt="image" src="https://github.com/user-attachments/assets/bdabb9ee-f46b-4ee6-9d16-2b3dd8708a37" />
<img width="480" height="526" alt="image" src="https://github.com/user-attachments/assets/ef45badd-fec5-443f-ae7c-cbec30dd3cf1" />
<img width="2852" height="858" alt="image" src="https://github.com/user-attachments/assets/d6e601fd-8cfb-4fde-a07d-12372702b3d7" />
<img width="1238" height="1504" alt="image" src="https://github.com/user-attachments/assets/47bfbb5d-ffea-401b-a0e6-08fa03a28f95" />

## Administration 
<img width="2820" height="1440" alt="image" src="https://github.com/user-attachments/assets/38dcd062-3b27-42b7-a55a-cb248e71785f" />

## Archive / Callback
<img width="1250" height="1494" alt="image" src="https://github.com/user-attachments/assets/5a6c07cb-66c9-4b38-a5d6-6b7425c775b0" />
<img width="2702" height="874" alt="image" src="https://github.com/user-attachments/assets/1625b3a1-5683-46ad-871a-a9aa1ac5eec7" />
<img width="2822" height="1454" alt="image" src="https://github.com/user-attachments/assets/cc7d770a-cc77-4904-8007-b5ec89e1c0db" />

# License

MIT
