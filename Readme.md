# VibeCheck AI

### Intelligent Brand-Governance Platform

**Team:** Karasuno  
**Event:** CodeNexus India 2026  
**Project:** VibeCheck AI

VibeCheck AI is an AI-powered brand governance platform designed to help organizations maintain consistent brand communication, review content, and manage brand compliance through role-based workflows.

## Features

- **Authentication:** JWT-based login and registration.
- **Role-based access:** Separate permissions for Content Creators, Managers, and Admins.
- **Content Editor:** Workspace for creating and editing content.
- **Review Dashboard:** Manager and Admin review workflows.
- **Document Manager:** Admin-only document management.
- **Compliance:** Brand compliance workflow.
- **Audit Logging:** Records important platform actions.
- **Demo Accounts:** Three predefined accounts for testing the platform.

## Tech Stack

### Frontend
- Angular 21
- TypeScript
- HTML and CSS
- Angular Router
- Angular Forms

### Backend
- Java 17
- Spring Boot 3.3.5
- Spring Security
- JWT Authentication
- Spring Data JPA
- MySQL
- BCrypt Password Encoding

## Project Structure

```text
VibeCheck-AI/
├── frontend/
│   └── src/
│       └── app/
│           ├── components/
│           ├── services/
│           └── guards/
│
├── backend/
│   └── src/
│       └── main/
│           ├── java/com/karasuno/backend/
│           │   ├── config/
│           │   ├── controller/
│           │   ├── entity/
│           │   ├── repository/
│           │   ├── security/
│           │   └── service/
│           └── resources/
│
└── README.md
```

*Adjust the folder names above if your actual repository structure differs.*

## User Roles

| Role | Permissions |
|---|---|
| Content Creator | Content Editor and compliance workflow |
| Manager | Content Editor and Review Dashboard |
| Admin | Content Editor, Review Dashboard, and Document Manager |

Backend API authorization is enforced using Spring Security.

## Demo Accounts

| Role | Username | Password |
|---|---|---|
| Content Creator | `creator` | `Creator@123` |
| Manager | `manager` | `Manager@123` |
| Admin | `admin` | `Admin@123` |

These accounts are intended for local development and demonstration only.

Demo account creation is controlled through the `DEMO_ACCOUNTS_ENABLED` environment setting.

## Getting Started

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd <YOUR_PROJECT_FOLDER>
```

### 2. Backend Setup

- Open the backend project.
- Configure the MySQL database.
- Set the required environment variables.
- Start the Spring Boot application.

Example configuration values:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vibecheck
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

app.cors.allowed-origins=http://localhost:4200
```

Use your own local database credentials. Do not commit passwords, JWT secrets, or API keys.

### 3. Frontend Setup

```bash
cd frontend
npm install
ng serve
```

Open:

```text
http://localhost:4200
```

### 4. Backend

Run the Spring Boot application from your IDE or using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

## API Security

The backend uses stateless JWT authentication.

| Endpoint | Access |
|---|---|
| `/api/auth/**` | Public |
| `/api/documents/**` | Admin |
| `/api/review/**` | Manager, Admin |
| `/api/compliance/**` | Content Creator, Manager, Admin |
| `/api/audit/**` | Admin |

## Team Collaboration

Before starting work:

```bash
git pull origin main
```

After making changes:

```bash
git add .
git commit -m "Describe your changes"
git push origin main
```

For larger features, create a separate branch and open a pull request.

## Security Notes

- Never commit `.env` files or application secrets.
- Never commit JWT signing keys or third-party API keys.
- Use environment variables for sensitive configuration.
- Demo credentials are for local demonstration only.
- Change demo credentials before any production deployment.

## Project Status

VibeCheck AI is under active development for CodeNexus India 2026.

More features, integrations, testing, and documentation will be added as development progresses.

---

**Built with ❤️ by Team Karasuno**