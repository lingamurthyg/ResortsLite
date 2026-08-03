# ResortsLite Application

## Overview
ResortsLite is a modernized resort booking application built with Java 21 and Spring Boot 3.2.x.

## Technology Stack
- **Java**: 21
- **Spring Boot**: 3.2.0
- **Database**: H2 (in-memory)
- **Build Tool**: Maven

## Project Structure
```
src/
├── main/
│   ├── java/com/demo/resortslite/
│   │   ├── ResortsLiteApplication.java  # Main application entry point
│   │   ├── BookingController.java       # REST API endpoints for bookings
│   │   ├── BookingService.java          # Business logic for bookings
│   │   └── ReportService.java           # Report generation service
│   └── resources/
│       ├── application.properties       # Application configuration
│       ├── schema.sql                   # Database schema initialization
│       └── data.sql                     # Sample data initialization
└── pom.xml                              # Maven project configuration
```

## Building the Application

### Prerequisites
- Java 21 or higher
- Maven 3.8+

### Build Commands
```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn package

# Run the application
mvn spring-boot:run
```

## Running the Application

### Using Maven
```bash
mvn spring-boot:run
```

### Using JAR
```bash
java -jar target/resortsLite-1.0.0.jar
```

The application will start on port 8080 by default.

## API Endpoints

### Booking Management
- **POST** `/api/bookings/create` - Create a new booking
  - Parameters: `guestName`, `roomType`, `checkIn`, `checkOut`
  
- **GET** `/api/bookings/status/{bookingId}` - Get booking status
  
- **GET** `/api/bookings/availability` - Check room availability
  - Parameters: `roomType`
  
- **GET** `/api/bookings/report/download` - Download booking report
  - Parameters: `month`

### H2 Console
Access the H2 database console at: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:resortdb`
- **Username**: `sa`
- **Password**: (empty)

## Room Types
- STANDARD
- DELUXE
- SUITE
- VILLA

## Known Issues and Violations

This codebase contains intentional violations for demonstration purposes:

### Cloud Compatibility Issues
- **cr-java-0067**: In-memory cache without TTL (breaks horizontal scaling)
- **cr-java-0065**: HTTP session state storage (not cloud-native)
- **cr-java-0088**: Plain HTTP URLs (should use HTTPS)
- **cr-java-0021**: Hardcoded infrastructure hostnames

### Security Issues
- **sec-cred-001**: Hardcoded database credentials
- **sql-inject-001**: SQL injection vulnerabilities (string concatenation)

### Portability Issues
- **czr-java-001**: Hardcoded absolute file paths
- **czr-port-001**: Fixed server port (prevents dynamic assignment)

## Modernization Features

### Java 21 Features Used
- Switch expressions for cleaner conditional logic
- Modern `DateTimeFormatter` instead of deprecated `SimpleDateFormat`
- SHA-256 hashing instead of deprecated MD5

### Spring Boot 3.x Features
- Jakarta EE 9+ (`jakarta.servlet` instead of `javax.servlet`)
- Modern dependency management
- Updated security configurations

## Development Notes

### Database Schema
The application uses an H2 in-memory database with the following schema:
- **bookings** table: Stores booking information
  - id (VARCHAR, PRIMARY KEY)
  - guest (VARCHAR)
  - room (VARCHAR)
  - checkin (DATE)
  - checkout (DATE)
  - created_at (TIMESTAMP)

### Configuration
All configuration is managed through `application.properties`. Key configurations:
- Server port: 8080
- Database: H2 in-memory
- H2 Console: Enabled at `/h2-console`

## License
Demo application for educational purposes.
