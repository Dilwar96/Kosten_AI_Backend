# KostenAI Backend

Eine Spring Boot Anwendung zur intelligenten Verwaltung von Rechnungen und Projekten mit KI-gestützter Rechnungsanalyse.

## 🚀 Technologie-Stack

- **Java 21**
- **Spring Boot 3.x**
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - Spring Validation
- **PostgreSQL** - Relationale Datenbank
- **JWT** - JSON Web Token für Authentifizierung
- **Gemini AI** - KI-gestützte Rechnungsanalyse
- **Lombok** - Java Code-Generierung
- **Maven** - Build-Management

## 📋 Voraussetzungen

- Java 17 oder höher
- Maven 3.6+
- PostgreSQL 13+
- Gemini AI API-Schlüssel

## ⚙️ Installation

### 1. Repository klonen
```bash
git clone <repository-url>
cd kosten-ai
```

### 2. Datenbank einrichten

PostgreSQL-Datenbank erstellen:
```sql
CREATE DATABASE kostenai;
CREATE USER kostenai_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE kostenai TO kostenai_user;
```

### 3. Konfiguration


```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/kostenai
spring.datasource.username=kostenai_user
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=your-secret-key-min-256-bits-for-HS256
jwt.expiration=86400000

# Gemini AI Configuration
gemini.api.key=your-gemini-api-key
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent

# File Upload Configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.com.kosten.ai=DEBUG
```

### 4. Dependencies installieren
```bash
mvn clean install
```

## 🏃 Ausführung

### Entwicklungsmodus
```bash
mvn spring-boot:run
```

### Production Build
```bash
mvn clean package
java -jar target/kosten-ai-0.0.1-SNAPSHOT.jar
```

Die Anwendung läuft auf `http://localhost:8080`

## 📡 API-Endpunkte

### Authentication
| Methode | Endpunkt             | Beschreibung          |
|---------|----------------------|-----------------------|
| POST    | `/api/auth/register` | Benutzer registrieren |
| POST    | `/api/auth/login`    | Benutzer anmelden     |

### Projects
| Methode | Endpunkt            | Beschreibung            |
|---------|---------------------|-------------------------|
| GET | `/api/projects`         | Alle Projekte abrufen   |
| GET | `/api/projects/{id}`    | Projekt nach ID         |
| POST | `/api/projects`        | Neues Projekt erstellen |
| PUT | `/api/projects/{id}`    | Projekt aktualisieren   |
| DELETE | `/api/projects/{id}` | Projekt löschen         |

### Invoices
| Methode | Endpunkt                             | Beschreibung                     |
|---------|--------------------------------------|----------------------------------|
| GET     | `/api/invoices`                      | Alle Rechnungen abrufen          |
| GET     | `/api/invoices/{id}`                 | Rechnung nach ID                 |
| GET     | `/api/invoices/project/{projectId}`  | Rechnungen nach Projekt          |
| POST    | `/api/invoices/upload/{projectId}`   | Rechnung hochladen & analysieren |
| PUT     | `/api/invoices/{id}`                 | Rechnung aktualisieren           |
| DELETE  | `/api/invoices/{id}`                 | Rechnung löschen                 |
| GET     | `/api/invoices/{id}/download`        | Rechnungsbild herunterladen      |

### Request/Response Beispiele

#### Register
```json
POST /api/auth/register
{
  "username": "max.mustermann",
  "email": "max@example.com",
  "password": "SecurePass123!"
}
```

#### Login
```json
POST /api/auth/login
{
  "username": "max.mustermann",
  "password": "SecurePass123!"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "username": "max.mustermann",
  "email": "max@example.com"
}
```

#### Create Project
```json
POST /api/projects
Authorization: Bearer <token>

{
  "name": "Website Redesign",
  "description": "Komplette Überarbeitung der Firmenwebsite"
}
```

#### Upload Invoice
```http
POST /api/invoices/upload/{projectId}
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <image-file>
```

## 🏗️ Projektstruktur

```
kosten-ai/
├── src/
│   ├── main/
│   │   ├── java/com/kosten/ai/
│   │   │   ├── config/           # Konfigurationsklassen
│   │   │   ├── controller/       # REST Controller
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA Entities
│   │   │   ├── exception/        # Custom Exceptions & Handler
│   │   │   ├── repository/       # JPA Repositories
│   │   │   ├── security/         # Security & JWT
│   │   │   └── service/          # Business Logic
│   │   └── resources/
│   │       └── application.properties
│   └── test/                     # Unit & Integration Tests
├── pom.xml
└── README.md
```

## ✨ Hauptfunktionen

### 🔐 Authentifizierung & Autorisierung
- JWT-basierte Authentifizierung
- Passwort-Verschlüsselung mit BCrypt
- Benutzer-spezifische Datenisolation

### 🤖 KI-gestützte Rechnungsanalyse
- Automatische Extraktion von Rechnungsdaten mit Gemini AI
- Erkennung von:
  - Rechnungsnummer
  - Lieferant/Firma
  - Betrag
  - Rechnungsdatum
  - Beschreibung

### 📊 Projekt-Management
- Projekte erstellen und verwalten
- Rechnungen zu Projekten zuordnen
- Projekt-Statistiken und Übersichten

### 🧾 Rechnungsverwaltung
- Rechnungen hochladen (Bild-Format)
- Automatische KI-Analyse
- CRUD-Operationen
- Rechnungsbilder herunterladen

## 🔧 Entwicklung

### Code-Style
- Verwenden Sie Lombok für Boilerplate-Code
- Folgen Sie Spring Boot Best Practices
- Schreiben Sie aussagekräftige Commit-Messages

### Datenbank-Migrationen
Das Projekt verwendet `spring.jpa.hibernate.ddl-auto=update` für automatische Schema-Updates in der Entwicklung.

Für Production empfohlen: Liquibase oder Flyway für kontrollierte Migrationen.

## 🐛 Fehlerbehandlung

Das Backend implementiert globale Exception-Handler:
- `ResourceNotFoundException` - 404 Not Found
- `InvalidRequestException` - 400 Bad Request
- `UnauthorizedException` - 401 Unauthorized
- `AiServiceException` - KI-Service Fehler
- `FileProcessingException` - Datei-Verarbeitungsfehler

## 🔒 Sicherheit

- JWT-Token mit 24h Ablaufzeit
- Passwörter werden mit BCrypt gehasht
- CORS-Konfiguration für Frontend-Integration
- Request-Validierung mit Bean Validation

## 📝 Logging

Logging-Level können in `application.properties` konfiguriert werden:
```properties
logging.level.com.kosten.ai=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate=INFO
```

## 🚀 Deployment

### Docker (Optional)
```dockerfile
FROM openjdk:17-slim
COPY target/kosten-ai-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```