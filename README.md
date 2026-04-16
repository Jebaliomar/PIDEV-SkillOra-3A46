# Skillora - JavaFX Desktop Application

Skillora is a JavaFX desktop application for managing Events, Salles (rooms), and Reservations, built with a clean MVC structure and JDBC/MySQL persistence.

## Features

- JavaFX UI (FXML + CSS)
- Event module (front/user display + admin-style management views)
- Salle module (list, details, and related operations)
- Reservation module (submission, listing, and management)
- Dark/modern UI styling across multiple views
- JDBC-based database access (MySQL)

## Tech Stack

- Java 21
- JavaFX 21
- Maven
- MySQL
- JDBC (`DriverManager`, `Connection`, `PreparedStatement`)

## Project Structure

```text
Skillora/
├─ pom.xml
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ tn/esprit/
│  │  │     ├─ entities/        # Domain models
│  │  │     ├─ services/        # Business/database services
│  │  │     ├─ controlles/      # JavaFX controllers
│  │  │     ├─ mains/           # JavaFX entry point
│  │  │     └─ tools/           # Utilities (DB connection, etc.)
│  │  └─ resources/
│  │     ├─ db.properties       # DB config
│  │     ├─ styles/             # JavaFX CSS
│  │     ├─ fxml/               # Common FXML screens
│  │     ├─ viewsadmin/         # Admin interfaces
│  │     └─ viewsuser/          # User/front interfaces
│  └─ test/
└─ target/
