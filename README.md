![CI](https://github.com/mariamfathallah/job-application-tracker/actions/workflows/ci.yml/badge.svg)
# Job Application Tracker – Backend API

A secure, production-ready RESTful backend application to manage and track job applications throughout the recruitment process.

The project demonstrates backend engineering best practices including JWT-based authentication, role-based access control, environment-driven configuration, containerized deployment, CI integration, and managed cloud database usage.

### 🔗 Live API:
https://job-application-tracker-rat3.onrender.com

> ⚠️ Deployed on Render free tier — the service may take a few seconds to wake up after inactivity.

## 🧪 Quick Test (Live Deployment)

1. Register:
   POST https://job-application-tracker-rat3.onrender.com/api/auth/register

2. Login to receive JWT token:
   POST https://job-application-tracker-rat3.onrender.com/api/auth/login

3. Use the token to access protected endpoints:
   Authorization: Bearer <token>

---

## 🚀 Features

### Core functionality
- Create, read, update and delete job applications
- Pagination & sorting
- Filtering support
- Proper HTTP status codes and structured error handling
- Input validation with meaningful error responses

### Security
- Authentication (JWT-based)
- User registration & login
- Ownership rules (users can only access their own applications)
- Role-based structure ready for admin extension

### Data & persistence
- PostgreSQL (production profile)
- H2 (test profile)
- Profile-based configuration (dev / prod / test)
- Environment-variable driven production config
- Docker support

### Quality & Testing
- Integration tests (MockMvc)
- Profile-based testing configuration
- Clean service/repository architecture
- Global exception handling

### DevOps & Deployment
- Dockerized multi-stage build
- GitHub Actions CI (tests on push)
- Managed PostgreSQL (Render)
- Public HTTPS deployment
- Actuator health endpoint

---

## 🛠 Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Web**
- **Spring Data JPA**
- **Spring Security (JWT)**
- **PostgreSQL**
- **H2 (tests)**
- **Docker**
- **Springdoc OpenAPI (Swagger UI)**
- **Spring Boot Actuator**
- **JUnit 5 & MockMvc**
- **Maven**

---

## 🏗 Architecture Overview

The application follows a layered architecture with clear separation of concerns:

Controller → Service → Repository → Database

- **Controllers** handle HTTP communication and request validation.
- **Services** encapsulate business logic and ownership rules.
- **Repositories** manage database persistence via Spring Data JPA.
- **Security layer** integrates JWT authentication and enforces per-user data isolation.
- **Global exception handling** ensures consistent and structured API error responses.
- **Profile-based configuration** separates development and production environments.
- **Actuator** provides operational monitoring.

---

## 📌 Domain Model

### JobApplication
- Company
- Position
- Status (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`)
- Date applied
- Notes
- Owner (User relationship)

### User
- Email
- Encrypted password
- Role

---

## 🔐 Authentication
### Register
```bash
POST /api/auth/register
```
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "displayName": "User Name"
}
```
### Login
```bash
POST /api/auth/login
```
Returns a JWT token to be used in:
```makefile
Authorization: Bearer <token>
```
All `/api/applications/**` endpoints require authentication.
Users can only access their own data.

---

## 📡 API Endpoints

Base path: `/api/applications`

| Method | Endpoint                        | Description                   |
|--------|---------------------------------|-------------------------------|
| POST   | `/api/applications`             | Create a new job application  |
| GET    | `/api/applications`             | List applications (Paginated) |
| GET    | `/api/applications/{id}`        | Get application by ID         |
| PUT    | `/api/applications/{id}`        | Full update                   |
| PATCH  | `/api/applications/{id}/status` | Update application status     |
| DELETE | `/api/applications/{id}`        | Delete an application         |


Pagination example:
```bash
GET /api/applications?page=0&size=10&sort=dateApplied,desc
```
---

## ❤️ Health Check
```bash
GET /actuator/health
```
Used for production monitoring.

---

## 📘 API Documentation 
After running the application, access:

* Swagger UI:
http://localhost:8080/swagger-ui/index.html

* OpenAPI JSON:
http://localhost:8080/v3/api-docs

---
## ⚙️ Profiles
- `dev` → Swagger enabled
- `test` → H2 in-memory DB
- `prod` → PostgreSQL + environment variables
Required production environment variables:
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`

---

## 🐳 Docker
Build image:
```bash
docker build -t job-tracker .
```

Run container:
```bash
docker run -p 8080:8080 job-tracker
```

---

## 🧪 Tests

Integration tests validate:
- Authentication flows
- Ownership enforcement
- CRUD operations
- Validation errors
- Pagination behavior

Run tests with:
```bash
mvn test
```
---

## ▶️ Run the Application
### Prerequisites
* Java 21
* Maven

### Start the app
```bash
mvn spring-boot:run
```
The application runs on:
```arduino
http://localhost:8080
```

---

## ☁ Deployment

- Currently deployed on **Render** with managed PostgreSQL.
- CI/CD pipeline runs on GitHub Actions and automatically validates builds on push.
- Architecture supports migration to other cloud providers (e.g., Google Cloud Run).

---

## 🎯 Project Goals

This project demonstrates:
- Secure REST API design
- Clean layered architecture
- Authentication & authorization implementation
- Profile-based configuration management
- Integration testing strategy
- Production deployment readiness
- Docker containerization

---
## 📚 Purpose

This project was developed as part of a **gap year personal initiative** after a MIAGE Bachelor degree to:
* Strengthen backend development skills
* Apply clean architecture principles
* Practice REST API design
* Gain hands-on experience with testing and QA-oriented development

## 🔜 Future Improvements
* Admin role
* Search & filtering enhancements
* Statistics endpoint (dashboard metrics)
* Frontend 
* Monitoring & logging improvements

## 👩‍💻 Author
**Mariam Fathallah**
MIAGE Graduate - Software Engineering & QA oriented