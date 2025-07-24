# File Transfer Application

This is a web-based file transfer application built with Spring Boot and Thymeleaf. It allows users to register, log in, upload, and manage their files securely.

## Features

*   User authentication (registration and login)
*   Secure file uploads and storage
*   File and folder management within a user-specific dashboard
*   User profile management, including avatar uploads
*   A responsive and modern UI built with Tailwind CSS

## Technologies Used

### Backend

*   **Java 21**
*   **Spring Boot 3.3.3**
    *   Spring Web
    *   Spring Security
    *   Spring Data JPA
*   **MySQL**: Database for storing user and file metadata.
*   **Maven**: Dependency management and build tool.

### Frontend

*   **Thymeleaf**: Java template engine for rendering server-side HTML.
*   **Tailwind CSS**: A utility-first CSS framework for styling.
*   **JavaScript**: For client-side interactions.

## Getting Started

Follow these instructions to get a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

*   **JDK 21** or later
*   **Maven 3.8** or later
*   **Node.js and npm**: For managing frontend dependencies and building CSS.
*   **MySQL**: An accessible database instance.

### Installation & Configuration

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/your-username/file-transfer.git
    cd file-transfer
    ```

2.  **Configure the database:**

    *   Open `src/main/resources/application.properties`.
    *   Update the `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` properties to match your MySQL setup.

3.  **Install frontend dependencies:**

    ```bash
    npm install
    ```

## Build and Run

1.  **Build the CSS:**

    Before running the application, you need to compile the Tailwind CSS.

    ```bash
    npm run build:css
    ```

2.  **Run the Spring Boot application:**

    You can run the application using the Maven wrapper:

    ```bash
    ./mvnw spring-boot:run
    ```

    The application will be accessible at `http://localhost:9090`.

## Project Structure

```
file-transfer/
├── src/
│   ├── main/
│   │   ├── java/com/app/file_transfer/
│   │   │   ├── config/         # Spring Security and Web configurations
│   │   │   ├── controller/     # Web request handlers
│   │   │   ├── model/          # JPA entity classes
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   └── services/       # Business logic and services
│   │   └── resources/
│   │       ├── static/         # CSS, JavaScript, and images
│   │       ├── templates/      # Thymeleaf HTML templates
│   │       └── application.properties # Application configuration
│   └── test/                   # Test sources
├── uploads/                    # Directory for storing uploaded files
├── pom.xml                     # Maven project configuration
└── package.json                # Frontend dependencies
```
