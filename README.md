# Meta WhatsApp Cloud API Simulator

A **stateless**, wire-compatible simulator of the Meta WhatsApp Cloud API, built for
testing a CPaaS platform without touching real Meta infrastructure. A CPaaS can point
its base URL at `http://localhost:8081` instead of `https://graph.facebook.com` and
everything else — endpoints, payloads, headers, status codes, error shapes, webhook
payloads — behaves the same.

This project **simulates Meta only**. It is not a CPaaS. It stores nothing:
no database, no JPA, no Kafka/RabbitMQ/Redis, no scheduler polling tables. Every
in-flight message exists only in memory for the duration of request handling and the
subsequent asynchronous DLR callback sequence; once those callbacks are dispatched,
the simulator forgets the message ever existed.

## Build status: Phase 1 of 5

Per the project plan, this is delivered incrementally:

- **Phase 1 (this delivery)** — project skeleton, `pom.xml`, `application.yml`,
  configuration classes, common DTOs (success/error envelopes), and shared utilities
  (WAMID generator, fbtrace_id generator, timestamp/phone-number helpers).
- Phase 2 — Authentication, Message API (all message types), validation, Meta-compatible
  success responses.
- Phase 3 — Async processing, webhook generator, Meta webhook payloads (statuses).
- Phase 4 — Failure simulation, retry logic, full error catalog, logging.
- Phase 5 — Provider abstraction (`ProviderSimulator` strategy interface) for future
  non-Meta simulators (Twilio, Gupshup, Infobip, ...), refactoring, unit tests.

## Tech stack

Java 21 · Spring Boot 3.3.x · Spring Web · Spring WebFlux (`WebClient` only, for
outbound webhook delivery) · Spring Validation · Spring `@Async` · Jackson · Lombok ·
Maven. No database, no ORM, no message broker.

## Project structure

```
src/main/java/com/simulator/metawhatsapp/
├── MetaWhatsAppSimulatorApplication.java
├── config/          # AsyncConfig (dedicated executor/scheduler), WebClientConfig
├── properties/       # SimulatorProperties - typed binding of application.yml
├── controller/        # (Phase 2) REST endpoints matching Meta's URL patterns
├── service/           # (Phase 2/3) business logic, kept out of controllers
├── dto/
│   ├── request/       # (Phase 2) inbound send-message payloads per message type
│   ├── response/       # Common success/error response envelopes (this phase)
│   └── webhook/         # (Phase 3) outbound webhook (DLR) payload structures
├── validator/          # (Phase 2) Meta-equivalent request validation
├── generator/           # WamidGenerator, FbTraceIdGenerator
├── util/                # TimestampUtil, PhoneNumberUtil
├── webhook/              # (Phase 3) webhook dispatch + retry
├── client/                # (Phase 3) WebClient wrapper for callback delivery
├── exception/              # MetaApiException + (Phase 4) global handler
└── provider/                # (Phase 5) ProviderSimulator abstraction
```

## Design note: snake_case field names on wire DTOs

Meta's JSON uses `snake_case` field names throughout (`wa_id`, `recipient_id`,
`error_subcode`, `fbtrace_id`, `messaging_product`, `phone_number_id`, ...). To
guarantee byte-for-byte wire compatibility with zero risk of a missed
`@JsonProperty` annotation, response/webhook DTOs in this project use the exact
Meta field name as the Java record component name (e.g. `wa_id`, not `waId`).
This is a deliberate tradeoff of Java naming convention purity for wire-format
correctness, which is the #1 requirement of this project.

## Configuration

Everything simulator-specific lives under the `simulator:` key in
`src/main/resources/application.yml`, bound immutably via
`SimulatorProperties` (a record hierarchy): supported/latest API version,
accepted bearer tokens, webhook verify token, callback URL, DLR delay timeline,
per-stage enable flags, and outcome probability distribution. Nothing is
hardcoded in application code.

## Running (once Phase 2+ land)

```bash
mvn spring-boot:run
```

The simulator listens on `8081` by default (`server.port`). Point your CPaaS's
Meta base URL at `http://localhost:8081` instead of `https://graph.facebook.com`.

> Note: this environment's build sandbox cannot reach Maven Central, so `mvn
> clean install` has not been executed here. Please build it in your own
> environment; the source has been written and reviewed for correctness against
> Spring Boot 3.3 / Java 21 APIs.
