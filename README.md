# Backend - Sistema de Gestión de Comedor UTEC

## 📋 Descripción

Backend REST API desarrollado con Spring Boot para el sistema de gestión de comedor universitario. Permite la gestión completa de usuarios, vendors (puestos de comida), menús, pedidos, disponibilidad, feedback y pagos, con autenticación JWT y control de acceso basado en roles. Integrado con Mercado Pago para procesamiento de pagos mediante Yape.

## 🏗️ Arquitectura

El proyecto sigue una **arquitectura limpia (Clean Architecture)** con separación de capas:

```
backend_proyecto_pi/
├── src/main/java/com/example/proyecto_pi3_backend/
│   ├── Auth/              # Autenticación y autorización
│   │   ├── application/   # AuthController
│   │   ├── domain/        # AuthService
│   │   └── dto/           # LoginRequest, RegisterRequest, AuthResponse
│   ├── User/              # Gestión de usuarios
│   │   ├── application/   # UserController
│   │   ├── domain/        # Users, UserService, Role
│   │   ├── dto/           # UserResponseDTO, UpdateRoleRequestDTO
│   │   └── infrastructure/# UserRepository
│   ├── Vendors/           # Gestión de vendors (puestos)
│   │   ├── application/   # VendorsController
│   │   ├── domain/        # Vendors, VendorsService
│   │   ├── dto/           # VendorRequestDTO, VendorResponseDTO
│   │   └── infrastructure/# VendorsRepository
│   ├── MenuItems/         # Gestión de items del menú
│   │   ├── application/   # MenuItemsController
│   │   ├── domain/        # MenuItems, MenuItemsService
│   │   ├── dto/           # MenuItemsRequestDTO, MenuItemsResponseDTO
│   │   └── infrastructure/# MenuItemsRepository
│   ├── Availability/       # Disponibilidad y stock
│   │   ├── application/   # AvailabilityController
│   │   ├── domain/        # Availability, AvailabilityService
│   │   ├── dto/           # AvailabilityResponseDTO
│   │   └── infrastructure/# AvailabilityRepository
│   ├── Orders/            # Gestión de pedidos
│   │   ├── application/   # OrdersController
│   │   ├── domain/        # Orders, OrdersService, OrderStatus, OrderSchedulerService
│   │   ├── dto/           # OrderRequestDTO, OrderResponseDTO
│   │   └── infrastructure/# OrdersRepository
│   ├── OrderDetails/      # Detalles de pedidos
│   │   ├── domain/        # OrderDetails
│   │   └── infrastructure/# OrderDetailsRepository
│   ├── Feedback/          # Comentarios y calificaciones
│   │   ├── application/   # FeedBackController
│   │   ├── domain/        # Feedback, FeedbackService
│   │   ├── dto/           # FeedbackRequestDTO, FeedbackResponseDTO
│   │   └── infrastructure/# FeedbackRepository
│   ├── Payment/           # Integración con Mercado Pago
│   │   ├── application/   # PaymentController
│   │   ├── domain/        # MercadoPagoService, MercadoPagoPaymentResponse
│   │   └── dto/           # YapePaymentRequest
│   ├── Dashboard/         # Estadísticas del sistema
│   │   ├── application/   # DashboardController
│   │   ├── domain/        # DashboardService
│   │   └── dto/           # DashboardStatsDTO
│   ├── config/            # Configuración
│   │   ├── SecurityConfig.java
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetailsService.java
│   │   └── Beans.java
│   ├── exception/         # Manejo de excepciones
│   │   └── ResourceNotFoundException.java
│   └── GlobalExceptionHandler.java
```

Cada módulo sigue la estructura:
- **domain/**: Entidades JPA y lógica de negocio
- **application/**: Controladores REST (endpoints)
- **dto/**: Data Transfer Objects (request/response)
- **infrastructure/**: Repositorios JPA

## 🛠️ Tecnologías

- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA** - Persistencia de datos
- **Spring Security** - Seguridad y autenticación
- **JWT (JSON Web Tokens)** - Autenticación stateless (com.auth0:java-jwt)
- **PostgreSQL** - Base de datos relacional
- **Hibernate** - ORM
- **Maven** - Gestión de dependencias
- **Lombok** - Reducción de código boilerplate
- **Mercado Pago SDK** - Integración de pagos (com.mercadopago:sdk-java 2.1.14)
- **SLF4J** - Logging

## 📦 Dependencias Principales

```xml
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- postgresql (driver)
- lombok
- com.auth0:java-jwt (4.5.0)
- com.mercadopago:sdk-java (2.1.14)
- modelmapper (3.2.1)
```

## ⚙️ Configuración

### 1. Requisitos Previos

- **Java 17** o superior
- **Maven 3.6+** o usar Maven Wrapper incluido
- **PostgreSQL 12+**
- **IDE** (IntelliJ IDEA, Eclipse, VS Code)

### 2. Base de Datos

1. **Crear la base de datos PostgreSQL:**
```sql
CREATE DATABASE comedor_utec;
```

2. **Configurar las credenciales** en `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/comedor_utec
spring.datasource.username=postgres
spring.datasource.password=tu_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

3. **Configuración de JPA:**
```properties
spring.jpa.hibernate.ddl-auto=update  # update, create-drop, validate, none
spring.jpa.show-sql=true              # Muestra las queries SQL
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### 3. Configuración JWT

El secreto JWT se configura en `application.properties`:
```properties
jwt-secret=tu_secreto_jwt_aqui
```

**⚠️ IMPORTANTE**: Cambia el secreto en producción por uno seguro y aleatorio (mínimo 256 bits).

### 4. Configuración Mercado Pago

Para habilitar pagos con Yape, configura tus credenciales de Mercado Pago:

```properties
# Credenciales de Mercado Pago
mercado-pago.access-token=APP_USR-xxxxx
mercado-pago.public-key=APP_USR-xxxxx

# URL del webhook (usar Ngrok para desarrollo local)
app.webhook-url=https://tu-dominio.com/payment/webhook

# Deep linking para retorno desde Mercado Pago
app.deep-link-scheme=frontendproyectopi
```

**Nota**: Para Yape, se requieren credenciales de **producción** incluso en modo de prueba.

### 5. Logging

```properties
logging.level.com.example.proyecto_pi3_backend=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.springframework.security=DEBUG
```

## 🚀 Ejecución

### Opción 1: Maven Wrapper (Recomendado)

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

### Opción 2: Maven

```bash
mvn spring-boot:run
```

### Opción 3: IDE

Ejecutar la clase `ProyectoPi3BackendApplication.java` como aplicación Java.

### Opción 4: JAR Ejecutable

```bash
# Compilar
mvn clean package

# Ejecutar
java -jar target/proyecto_pi3_backend-0.0.1-SNAPSHOT.jar
```

El servidor iniciará en: **`http://localhost:8080`**

## 📡 API Endpoints

### 🔐 Autenticación

#### POST `/auth/register`
Registra un nuevo usuario.

**Request:**
```json
{
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "password": "Password123"
}
```

**Response:**
```json
{
  "id": 1,
  "email": "juan@example.com",
  "firstName": "Juan",
  "lastName": "Pérez",
  "role": "USER",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "vendorId": null
}
```

#### POST `/auth/login`
Inicia sesión con email y contraseña.

**Request:**
```json
{
  "email": "juan@example.com",
  "password": "Password123"
}
```

**Response:** (Igual que register)

---

### 👥 Usuarios

#### GET `/users/me`
Obtiene la información del usuario autenticado.

**Headers:** `Authorization: Bearer <token>`

**Response:**
```json
{
  "id": 1,
  "firstName": "Juan",
  "lastName": "Pérez",
  "email": "juan@example.com",
  "role": "USER",
  "vendorId": null
}
```

#### GET `/users/{id}`
Obtiene un usuario por ID.

#### GET `/users`
Lista todos los usuarios (solo ADMIN).

#### PUT `/users/{id}/role`
Actualiza el rol de un usuario (solo ADMIN).

**Request:**
```json
{
  "role": "VENDOR"
}
```

---

### 🏪 Vendors

#### GET `/vendors`
Lista todos los vendors.

**Response:**
```json
[
  {
    "id": 1,
    "name": "Puesto de Comida 1",
    "ubication": "Patio Principal",
    "openingTime": "08:00",
    "closingTime": "17:00"
  }
]
```

#### GET `/vendors/{id}`
Obtiene un vendor por ID.

#### POST `/vendors`
Crea un nuevo vendor (solo ADMIN).

**Request:**
```json
{
  "name": "Puesto de Comida 1",
  "ubication": "Patio Principal",
  "openingTime": "08:00",
  "closingTime": "17:00"
}
```

#### PUT `/vendors/{id}`
Actualiza un vendor (solo ADMIN).

#### DELETE `/vendors/{id}`
Elimina un vendor (solo ADMIN).

---

### 🍽️ Menús

#### GET `/menu-items`
Lista todos los menús (solo ADMIN).

#### GET `/menu-items/vendor/{vendorId}/all`
Obtiene todos los menús de un vendor con todas sus disponibilidades (VENDOR).

#### GET `/menu-items/today`
Obtiene menús disponibles hoy (USER).

#### GET `/menu-items/vendor/{vendorId}/today`
Obtiene menús de un vendor disponibles hoy.

#### GET `/menu-items/date/{date}`
Obtiene menús disponibles por fecha (formato: `YYYY-MM-DD`).

#### GET `/menu-items/vendor/{vendorId}/date/{date}`
Obtiene menús de un vendor por fecha.

#### GET `/menu-items/week/{weekStartDate}`
Obtiene menús disponibles de la semana (formato: `YYYY-MM-DD`).

#### GET `/menu-items/vendor/{vendorId}/week/{weekStartDate}`
Obtiene menús de un vendor de la semana.

#### GET `/menu-items/{id}`
Obtiene un menú por ID.

#### POST `/menu-items`
Crea un nuevo menú (ADMIN/VENDOR).

**Request:**
```json
{
  "itemName": "Arroz con Pollo",
  "description": "Delicioso arroz con pollo",
  "price": "12.50",
  "vendorId": 1,
  "stock": 50,
  "date": "2024-01-15"
}
```

#### PUT `/menu-items/{id}`
Actualiza un menú (ADMIN/VENDOR).

#### DELETE `/menu-items/{id}`
Elimina un menú completamente (solo ADMIN).

#### DELETE `/menu-items/{id}/availability?date=2024-01-15`
Elimina una disponibilidad específica (fecha) de un menú (VENDOR).

---

### 📦 Pedidos

#### POST `/orders`
Crea un nuevo pedido (USER).

**Request:**
```json
{
  "vendorId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    }
  ]
}
```

**Response:**
```json
{
  "id": 1,
  "status": "PENDIENTE_PAGO",
  "pickup_time": "2024-01-15T12:00:00",
  "userId": 1,
  "userName": "Juan Pérez",
  "vendorId": 1,
  "vendorName": "Puesto de Comida 1",
  "pickupCode": "ABC123",
  "paymentMethod": null,
  "items": [
    {
      "id": 1,
      "itemName": "Arroz con Pollo",
      "quantity": 2,
      "price": "12.50",
      "menuItemId": 1
    }
  ]
}
```

#### POST `/orders/{orderId}/pay`
Marca un pedido como pagado (USER). **Nota:** Este endpoint está obsoleto, el pago se realiza mediante Mercado Pago.

#### GET `/orders/{orderId}`
Obtiene un pedido por ID.

#### GET `/orders/user/{userId}`
Obtiene todos los pedidos de un usuario.

#### GET `/orders/vendor/{vendorId}`
Obtiene todos los pedidos de un vendor (VENDOR).

#### POST `/orders/{orderId}/ready`
Marca un pedido como listo para recoger (VENDOR).

**Response:** Pedido con `status: "LISTO_PARA_RECOJO"`

#### POST `/orders/{orderId}/complete`
Marca un pedido como completado (VENDOR).

**Response:** Pedido con `status: "COMPLETADO"`

#### DELETE `/orders/{orderId}`
Cancela un pedido (USER, solo si está `PENDIENTE_PAGO`).

---

### 💳 Pagos (Mercado Pago)

#### POST `/payment/yape/token`
Genera un token de Yape para autenticación.

**Request:**
```
POST /payment/yape/token?phoneNumber=999999999&otp=123456
```

**Response:**
```json
"token_generado_por_mercado_pago"
```

#### POST `/payment/yape/{orderId}`
Crea un pago Yape para un pedido.

**Request:**
```json
{
  "token": "token_generado_anteriormente",
  "payerEmail": "usuario@example.com"
}
```

**Response:**
```json
{
  "preferenceId": "1234567890",
  "total": 25.00,
  "paymentMethod": "YAPE"
}
```

#### POST `/payment/webhook`
Webhook de Mercado Pago para notificaciones de pago.

**Request:** (Enviado por Mercado Pago)
```json
{
  "data": {
    "id": "payment_id"
  },
  "type": "payment"
}
```

**Response:** `"OK"`

---

### 💬 Feedback

#### GET `/feedback`
Lista todos los comentarios (ADMIN).

#### GET `/feedback/item/{menuItemId}`
Obtiene comentarios de un item del menú.

#### GET `/feedback/vendor/{vendorId}`
Obtiene comentarios de menús de un vendor (VENDOR).

#### GET `/feedback/user/{userId}`
Obtiene comentarios de un usuario.

#### POST `/feedback`
Crea un comentario (USER, solo uno por pedido).

**Request:**
```json
{
  "orderId": 1,
  "menuItemId": 1,
  "rating": 5,
  "comment": "Excelente comida"
}
```

**Response:**
```json
{
  "id": 1,
  "rating": 5,
  "comment": "Excelente comida",
  "itemName": "Arroz con Pollo",
  "vendorName": "Puesto de Comida 1",
  "createdAt": "2024-01-15T12:00:00"
}
```

---

### 📊 Dashboard

#### GET `/dashboard/stats`
Obtiene estadísticas generales del sistema (ADMIN).

**Response:**
```json
{
  "totalUsers": 100,
  "totalVendors": 5,
  "totalMenuItems": 50,
  "totalOrders": 500,
  "totalCompletedOrders": 450,
  "totalPendingOrders": 30,
  "totalRevenue": 12500.50
}
```

---

## 🔐 Autenticación y Autorización

### Roles del Sistema

1. **USER** (Usuario Regular)
   - Ver menús disponibles
   - Crear pedidos
   - Pagar pedidos (Yape)
   - Ver sus pedidos
   - Dar feedback (uno por pedido)
   - Cancelar pedidos pendientes

2. **VENDOR** (Vendedor)
   - Gestionar sus menús (crear, editar, eliminar disponibilidades)
   - Ver todos sus menús (incluyendo pasados)
   - Ver pedidos de su vendor
   - Filtrar pedidos por estado
   - Marcar pedidos como listos/completados
   - Ver comentarios de sus menús

3. **ADMIN** (Administrador)
   - Gestión completa de usuarios (ver, cambiar roles)
   - Gestión de vendors (crear, editar, eliminar)
   - Gestión de menús (todos los vendors)
   - Ver todas las estadísticas
   - Ver todos los comentarios
   - Filtrar comentarios por fecha y calificación

### Uso de JWT

Todas las peticiones (excepto `/auth/login` y `/auth/register`) requieren el header:

```
Authorization: Bearer <token>
```

El token se obtiene al hacer login y contiene:
- ID del usuario
- Email
- Rol
- Vendor ID (si es VENDOR)

**Duración del token:** Configurable en `JwtService`.

---

## 📊 Modelo de Datos

### Entidades Principales

#### Users
- `id` (Long, PK)
- `firstName` (String)
- `lastName` (String)
- `email` (String, único)
- `password` (String, encriptado)
- `role` (Enum: USER, VENDOR, ADMIN)
- `vendorId` (Long, FK a Vendors, opcional)

#### Vendors
- `id` (Long, PK)
- `name` (String)
- `ubication` (String)
- `openingTime` (LocalTime)
- `closingTime` (LocalTime)

#### MenuItems
- `id` (Long, PK)
- `itemName` (String)
- `description` (String, opcional)
- `price` (BigDecimal)
- `vendorId` (Long, FK a Vendors)

#### Availability
- `id` (Long, PK)
- `menuItemId` (Long, FK a MenuItems)
- `date` (LocalDate)
- `stock` (Integer)
- `isAvailable` (Boolean, calculado)

#### Orders
- `id` (Long, PK)
- `status` (String: PENDIENTE_PAGO, PAGADO, LISTO_PARA_RECOJO, COMPLETADO, CANCELADO)
- `pickup_time` (Timestamp)
- `createdAt` (Timestamp)
- `pickupCode` (String, único)
- `paymentMethod` (String: YAPE)
- `mercadoPagoPaymentId` (String)
- `mercadoPagoPreferenceId` (String)
- `userId` (Long, FK a Users)
- `vendorId` (Long, FK a Vendors)

#### OrderDetails
- `id` (Long, PK)
- `orderId` (Long, FK a Orders)
- `menuItemId` (Long, FK a MenuItems)
- `quantity` (Integer)

#### Feedback
- `id` (Long, PK)
- `orderId` (Long, FK a Orders)
- `menuItemId` (Long, FK a MenuItems)
- `userId` (Long, FK a Users)
- `rating` (Integer, 1-5)
- `comment` (String, opcional)
- `createdAt` (Timestamp)

### Relaciones

- `User` → `Vendor` (Many-to-One, opcional para VENDOR)
- `Vendor` → `MenuItems` (One-to-Many)
- `Vendor` → `Orders` (One-to-Many)
- `MenuItems` → `Availability` (One-to-Many)
- `Orders` → `OrderDetails` (One-to-Many)
- `Orders` → `User` (Many-to-One)
- `Orders` → `Vendor` (Many-to-One)
- `Feedback` → `User` (Many-to-One)
- `Feedback` → `MenuItem` (Many-to-One)
- `Feedback` → `Order` (Many-to-One)

---

## 🔄 Flujo de Pedidos

1. **Creación** (`POST /orders`)
   - Usuario crea pedido → Estado: `PENDIENTE_PAGO`
   - Se descuenta stock automáticamente
   - Se genera código de recogida único

2. **Pago** (`POST /payment/yape/{orderId}`)
   - Usuario genera token Yape con número y OTP
   - Usuario crea pago con token y email
   - Si el pago es aprobado → Estado: `PAGADO`
   - Si el pago es rechazado → Estado: `PENDIENTE_PAGO` (se mantiene)

3. **Preparación** (`POST /orders/{orderId}/ready`)
   - Vendor marca como listo → Estado: `LISTO_PARA_RECOJO`

4. **Completado** (`POST /orders/{orderId}/complete`)
   - Vendor marca como completado → Estado: `COMPLETADO`

5. **Feedback** (`POST /feedback`)
   - Usuario puede dar feedback cuando está `COMPLETADO`
   - Solo un comentario por pedido
   - Comentarios son anónimos (no se muestra el usuario)

6. **Cancelación**
   - Usuario puede cancelar si está `PENDIENTE_PAGO`
   - Se devuelve stock automáticamente
   - Tarea programada cancela pedidos no pagados después de 5 minutos

---

## 📦 Gestión de Stock

- El stock se gestiona en la entidad `Availability` (uno por fecha)
- Cada `MenuItem` puede tener múltiples registros de `Availability`
- Al crear un pedido, se descuenta automáticamente el stock de la fecha del pedido
- Si el stock llega a 0, el item se marca como no disponible automáticamente
- Al cancelar un pedido, se devuelve el stock automáticamente

---

## 💳 Sistema de Pagos (Mercado Pago - Yape)

### Flujo de Pago Yape

1. Usuario genera token Yape:
   - Proporciona número de celular y código OTP de la app Yape
   - Backend llama a API de Mercado Pago para generar token

2. Usuario crea pago:
   - Proporciona token y email
   - Backend crea pago en Mercado Pago usando el token
   - Si es aprobado, el pedido cambia a `PAGADO`

3. Webhook:
   - Mercado Pago notifica cambios de estado del pago
   - Backend actualiza el estado del pedido automáticamente

### Requisitos

- **Credenciales de producción** de Mercado Pago (incluso para pruebas)
- **Email válido** del pagador (no puede ser de prueba)
- **Token Yape válido** (generado con número y OTP reales)

---

## ⏰ Tareas Programadas

El sistema incluye una tarea programada que se ejecuta cada minuto:

### Cancelación Automática de Pedidos

- Cancela pedidos con estado `PENDIENTE_PAGO` que tienen más de 5 minutos de antigüedad
- Devuelve el stock automáticamente
- Registra logs de todas las cancelaciones

**Configuración:** `OrderSchedulerService.java` con `@Scheduled(fixedRate = 60000)`

---

## 🐛 Manejo de Errores

### Excepciones Personalizadas

- **`ResourceNotFoundException`**: Recurso no encontrado (404)
- **`RuntimeException`**: Errores de validación y lógica de negocio (400)

### GlobalExceptionHandler

Maneja todas las excepciones y devuelve respuestas JSON consistentes:

```json
{
  "message": "Descripción del error",
  "timestamp": "2024-01-15T12:00:00"
}
```

### Códigos de Estado HTTP

- `200 OK`: Operación exitosa
- `201 Created`: Recurso creado
- `400 Bad Request`: Error de validación o lógica
- `401 Unauthorized`: No autenticado
- `403 Forbidden`: No autorizado (rol incorrecto)
- `404 Not Found`: Recurso no encontrado
- `500 Internal Server Error`: Error del servidor

---

## 🧪 Testing

```bash
# Ejecutar todos los tests
mvn test

# Ejecutar tests con cobertura
mvn test jacoco:report
```

---

## 📝 Logging

El sistema usa **SLF4J** para logging estructurado:

```java
@Slf4j
public class MiServicio {
    log.info("Mensaje informativo");
    log.error("Error", exception);
    log.warn("Advertencia");
    log.debug("Debug");
}
```

**Niveles configurados:**
- `DEBUG`: Lógica de negocio y servicios
- `INFO`: Operaciones importantes
- `WARN`: Advertencias
- `ERROR`: Errores

---

## 🔧 Variables de Entorno Recomendadas

Para producción, usa variables de entorno en lugar de hardcodear valores:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt-secret=${JWT_SECRET}
mercado-pago.access-token=${MP_ACCESS_TOKEN}
mercado-pago.public-key=${MP_PUBLIC_KEY}
app.webhook-url=${WEBHOOK_URL}
```

---

## 🚨 Consideraciones Importantes

### Seguridad

1. **JWT Secret**: Cambia el secreto en producción por uno seguro y aleatorio (mínimo 256 bits)
2. **HTTPS**: Usa HTTPS en producción
3. **Validación**: Valida todas las entradas del usuario
4. **CORS**: Configura CORS apropiadamente en producción
5. **Credenciales**: Nunca commitees credenciales reales al repositorio

### Base de Datos

1. **Backups**: Haz backups regulares
2. **DDL Auto**: No uses `ddl-auto=create-drop` en producción
3. **Migraciones**: Considera usar migraciones (Flyway/Liquibase) para cambios de esquema
4. **Índices**: Agrega índices para campos frecuentemente consultados

### Performance

1. **Logs SQL**: Los logs SQL pueden afectar el rendimiento en producción
2. **Caché**: Considera usar caché para consultas frecuentes
3. **Queries N+1**: Optimiza las queries N+1 usando `@EntityGraph` o `JOIN FETCH`
4. **Paginación**: Implementa paginación para listas grandes

### Mercado Pago

1. **Credenciales**: Usa credenciales de producción para Yape
2. **Webhook**: Configura el webhook URL correctamente
3. **Errores**: Maneja todos los posibles errores de Mercado Pago
4. **Testing**: Prueba el flujo completo de pago antes de producción

---

## 📚 Estructura de Respuestas

### Respuesta Exitosa

```json
{
  "id": 1,
  "name": "Ejemplo",
  ...
}
```

### Respuesta de Error

```json
{
  "message": "Descripción del error",
  "timestamp": "2024-01-15T12:00:00"
}
```

### Respuesta de Error de Mercado Pago

```json
{
  "message": "Error al crear pago Yape en Mercado Pago: [detalle]",
  "errorType": "RuntimeException"
}
```

---

## 🔍 Búsqueda y Filtrado

### Menús
- Búsqueda por nombre de item
- Filtro por vendor
- Filtro por fecha
- Filtro por disponibilidad

### Pedidos
- Búsqueda por código de recogida
- Filtro por estado
- Filtro por vendor (VENDOR)
- Filtro por usuario (USER)

### Comentarios
- Filtro por calificación (1-5 estrellas)
- Filtro por fecha (hoy, semana, mes, todos)
- Búsqueda por nombre de item o vendor

---

## 📞 Soporte

Para problemas o preguntas:
1. Revisa los logs de la aplicación
2. Consulta la documentación de Spring Boot
3. Verifica la configuración de Mercado Pago
4. Revisa los issues del proyecto

---

## 📄 Licencia

[Especificar licencia del proyecto]
