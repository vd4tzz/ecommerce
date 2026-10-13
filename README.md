# Ecommerce

A Spring Boot backend for an online shop: catalog, cart, checkout, orders, stock, shipping fees and payments. It is a
single application split into modules. Each module owns its data and rules, and modules talk to each
other through small published interfaces and events.

## Stack

- **Java 25** and **Spring Boot 4.1**
- **PostgreSQL** for data, and an S3-compatible object store (SeaweedFS in development) for files
- **JWT** access and refresh tokens
- **Gradle** to build; **JUnit**, **Mockito** and **Testcontainers** to test

## Modules

| Module | What it does | Docs |
|---|---|---|
| `user` | Registration, email verification by OTP, login, token refresh, password reset, profile | [user](docs/user.md) |
| `catalog` | Categories, attributes, options, products and variants, and product search | [catalog](docs/catalog.md) |
| `storage` | Product images: signed uploads and public URLs | [storage](docs/storage.md) |
| `cart` | One cart per user, merging a guest cart after login | [cart](docs/cart.md) |
| `inventory` | Warehouses, stock per variant, reservations for orders, and the stock ledger | [inventory](docs/inventory.md) |
| `shipping` | Shipping fee of an order, one package per warehouse | |
| `checkout` | Turns the items a customer picked into an order and holds its stock | [checkout](docs/checkout.md) |
| `order` | The order and its status, from placing to delivery, return or cancellation | [order](docs/order.md) |
| `payment` | Money received and sent back, gateways, and refund requests | [payment](docs/payment.md) |

To see how checkout, inventory, order and payment fit together for one purchase, read
[From checkout to payment](docs/checkout-to-payment.md).

Inside a module the code is laid out the same way:

```
<module>/
├── domain/          Entities, value objects, rules and repository interfaces (plain Java)
├── application/     Use cases offered to callers and to other modules
├── query/           Read-side interfaces and the view records they return
├── inbound/         REST controllers, event listeners and scheduled jobs
└── infrastructure/  JPA entities, repository implementations and the SQL behind the queries
```

## Event publication registry

Modules tell each other that something happened with events, for example payment publishes `PaymentReceivedEvent` and
order reacts to it. Spring Modulith saves each event in the `event_publication` table, in the same transaction as the
publisher's changes, and keeps it until its listener has finished. If a listener crashes, the event is still in the table
and can be delivered again.

See [Event publication registry](docs/event-publication-registry.md) for how events are saved, completed and retried.

## Getting started

You need JDK 25 and Docker.

1. Start PostgreSQL, the object store and a mail catcher:

   ```bash
   docker compose up -d
   ```

   | Service | Port | Use |
   |---|---|---|
   | PostgreSQL | 5432 | Database `ecommerce` |
   | SeaweedFS | 8333 | S3 API for product images |
   | Mailpit | 1025, 8025 | SMTP, and a web page to read the emails the app sends (OTP codes) |

2. Run the application:

   ```bash
   ./gradlew bootRun
   ```

3. Open the API docs at <http://localhost:8080/swagger-ui.html>.

### Configuration

Defaults for local development are in [`application.yml`](src/main/resources/application.yml). These can be set with
environment variables:

| Variable | Meaning |
|---|---|
| `JWT_SECRET_KEY` | Key that signs the tokens |
| `JWT_ACCESS_TOKEN_VALIDITY`, `JWT_REFRESH_TOKEN_VALIDITY` | Token lifetimes, e.g. `15m` and `7d` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Database connection |
| `APP_STORAGE_S3_BUCKET`, `APP_STORAGE_S3_REGION`, `APP_STORAGE_S3_ENDPOINT`, `APP_STORAGE_S3_ACCESS_KEY`, `APP_STORAGE_S3_SECRET_KEY` | Object store for product images |
| `APP_STORAGE_S3_PUBLIC_BASE_URL`, `APP_STORAGE_S3_PRESIGNED_DURATION_MINUTES` | Public image URL, and how long a signed upload URL lasts |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | Outgoing mail |
| `SEPAY_MERCHANT`, `SEPAY_SECRET_KEY` | SePay merchant id and secret key |
| `SEPAY_CHECKOUT_URL` | SePay checkout page (the sandbox by default) |
| `SEPAY_RETURN_URL` | Where the customer lands after paying |

## Tests

```bash
./gradlew test
```

The persistence tests run against a PostgreSQL container started by Testcontainers, so Docker must be running. The
container is shared by the whole test run and never touches your development database.

## Documentation

- [User](docs/user.md)
- [Catalog](docs/catalog.md)
- [Storage](docs/storage.md)
- [Cart](docs/cart.md)
- [Inventory](docs/inventory.md)
- [Checkout](docs/checkout.md)
- [Order](docs/order.md)
- [Payment](docs/payment.md)
- [From checkout to payment](docs/checkout-to-payment.md)
- [Event publication registry](docs/event-publication-registry.md)

Endpoint details (parameters, bodies, errors) are in the Swagger UI; the pages above explain the concepts and rules
behind them.
