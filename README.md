# E-Commerce Backend

REST API for a full-stack e-commerce platform with role-based access control. Built with Spring Boot, PostgreSQL, and JWT authentication.

**Live API:** https://ecommerce-backend-f4f3.onrender.com  
**Frontend:** https://ecommerce-cav.netlify.app  
**Frontend repo:** https://github.com/cristian-ves/ecommerce-frontend

> **Note:** The backend is hosted on Render's free tier and may take up to 50 seconds to respond on the first request after a period of inactivity.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.6 |
| Language | Java 17 |
| Database | PostgreSQL (Neon) |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT (jjwt 0.11.5) |
| Security | Spring Security |
| Build | Maven |
| Utilities | Lombok |

---

## Roles

The system has four roles with different access levels:

| Role ID | Name | Access |
|---|---|---|
| 1 | Admin | Employee management, reports |
| 2 | Moderator | Item request approval, user bans |
| 3 | Logistics | Purchase delivery management |
| 4 | User | Buy, sell, cart, purchases |

---

## Getting Started

### Prerequisites

- Java 17
- Maven 3.8+
- PostgreSQL database (local or cloud — Neon recommended)

### Local setup

**1. Clone the repo**

```bash
git clone https://github.com/cristian-ves/ecommerce-backend
cd ecommerce-backend
```

**2. Create `src/main/resources/application-dev.properties`**

This file is gitignored. Use it for local development:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_db
spring.datasource.username=postgres
spring.datasource.password=your_password
jwt.secret=local-dev-secret-key-minimum-32-chars!!
cors.allowed-origins=http://localhost:5173
```

**3. Run with the `dev` profile**

In IntelliJ: Edit Configurations → Active profiles → `dev`

Or from the terminal:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**4. API is available at** `http://localhost:8080`

### Environment variables (production)

The `application-prod.properties` file references these environment variables, which must be set on the deployment platform:

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 32 chars) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins |

---

## API Reference

All endpoints except `/api/auth/**` require a `Bearer` token in the `Authorization` header.

### Auth

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Login and receive JWT | No |
| POST | `/api/auth/register` | Register a new user | No |
| GET | `/api/auth/validate` | Validate existing JWT | Yes |
| GET | `/api/auth/ping` | Health check / wake backend | No |

**Login request:**
```json
{
  "email": "buyer@demo.com",
  "password": "demo1234"
}
```

### Items
| Method | Endpoint | Description |
|---|---|---|
| GET | `/items?page=0&size=20` | Get paginated items |
| GET | `/items/search?q=keyword&categories=1,2` | Search items by query and/or filter by category IDs (both params optional, combinable) |
| GET | `/items/user/{userId}` | Get items listed by a specific user |
| POST | `/items/add` | Add a new item for sale |
| PUT | `/items/update` | Update an existing item |
| PUT | `/items/review?rate=5` | Submit a rating for an item |

### Cart

| Method | Endpoint | Description |
|---|---|---|
| GET | `/cart/user/{userId}` | Get cart items for a user |
| POST | `/cart/add` | Add an item to the cart |
| DELETE | `/cart/decrement?userId=&itemId=` | Decrement item quantity by 1 |
| DELETE | `/cart/delete?userId=&itemId=` | Remove item from cart |
| DELETE | `/cart/clear?userId=` | Clear entire cart |

### Cards

| Method | Endpoint | Description |
|---|---|---|
| GET | `/cards/{userId}` | Get saved cards for a user |
| POST | `/cards` | Save a new payment card |

### Purchases

| Method | Endpoint | Description |
|---|---|---|
| POST | `/purchases` | Create a new purchase |
| GET | `/purchases/user/{userId}` | Get purchases for a user |
| GET | `/purchases` | Get all purchases (logistics) |
| PUT | `/purchases/{id}/deliver` | Mark purchase as delivered |
| PUT | `/purchases/{id}/delivery-date` | Update expected delivery date |

### Moderator

| Method | Endpoint | Description |
|---|---|---|
| GET | `/mod/requests` | Get pending item requests |
| PUT | `/mod/accept/{id}` | Accept an item request |
| PUT | `/mod/reject/{id}` | Reject an item request |
| GET | `/mod/users` | Get all users with user role |
| PUT | `/mod/ban/{id}` | Toggle ban/unban on a user |

### Admin

| Method | Endpoint | Description |
|---|---|---|
| GET | `/admin/employees` | Get all employees |
| POST | `/admin/employees` | Add a new employee |
| PUT | `/admin/employees/{id}` | Update an employee |

### Reports (Admin)

All report endpoints accept `startDate` and `endDate` query params in `YYYY-MM-DD` format.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/reports/top-products?startDate=&endDate=` | Top 10 best-selling products |
| GET | `/reports/top-clients-revenue?startDate=&endDate=` | Top 5 clients by revenue |
| GET | `/reports/top-clients-sales?startDate=&endDate=` | Top 5 clients by sales volume |
| GET | `/reports/top-clients-orders?startDate=&endDate=` | Top 10 clients by order count |
| GET | `/reports/top-clients-inventory` | Top 10 clients by products listed |

---

## Project Structure

```
src/main/java/com/alejo/
├── config/          — Security and CORS configuration
├── controllers/     — REST controllers and DTOs
├── entities/        — JPA entities
├── persistence/     — DAO interfaces and implementations
├── repository/      — Spring Data JPA repositories
├── security/        — JWT filter and utilities
└── service/         — Business logic interfaces and implementations
```

---

## Known Limitations

- Payment processing is simulated — no real payment gateway integration
- Image URLs are external links (Pexels) and may load slowly or become unavailable