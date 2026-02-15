# Job Application Tracker – Backend API

A secure RESTful backend application to manage and track job applications throughout the recruitment process.

This project was built as a personal initiative to demonstrate **backend engineering skills, production readiness, testing strategy, and deployment practices**.

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
- H2 (development & test profile)
- Environment-based configuration
- Docker support

### Quality & Testing
- Integration tests (MockMvc)
- Profile-based testing configuration
- Clean service/repository architecture
- Global exception handling

### DevOps
- Dockerized application
- Production & development profiles
- Designed for deployment on Google Cloud Run
- CI/CD ready (GitHub Actions)

---

## 🛠 Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Web**
- **Spring Data JPA**
- **Spring Security (JWT)**
- **PostgreSQL**
- **H2 (dev & test)**
- **Docker**
- **Springdoc OpenAPI (Swagger UI)**
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

## 📘 API Documentation 
After running the application, access:

* Swagger UI:
http://localhost:8080/swagger-ui/index.html

* OpenAPI JSON:
http://localhost:8080/v3/api-docs

---
## ⚙️ Profiles
### Development
- H2 database
- SQL visible in logs
- Swagger enabled

### Production
- PostgreSQL
- Environment variables required
- Secure configuration

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
- 404 handling
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
Designed for:
- GitHub Actions CI/CD
- Google Cloud Run
- Managed PostgreSQL

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
* Frontend (React)
* Monitoring & logging improvements

## 👩‍💻 Author
**Mariam Fathallah**
MIAGE Graduate - Software Engineering & QA oriented