# Movie Notifier

Spring Boot service that polls YTS and sends Firebase Cloud Messaging (FCM) push notifications for new movies.

## What It Does

- Polls YTS on a configurable schedule.
- Persists notified movie IDs in `notified_movies` to avoid resend after restart.
- Manages FCM subscriptions through a single REST controller.
- Sends cross-platform push payloads (notification + data) via Firebase Admin SDK.
- Removes invalid/uninstalled device tokens when FCM returns terminal token errors.
- Supports JVM run and GraalVM native builds (AMD64 and ARM64).

## Project Layout

- Entrypoint: `src/main/java/ar/com/martinrevert/movienotifier/MovieNotifierApplication.java`
- Config package: `src/main/java/ar/com/martinrevert/movienotifier/config` (`DataSourceConfig`, `RestClientConfig`, `FirebaseConfig`, `SchedulingConfig`)
- Controller package: `src/main/java/ar/com/martinrevert/movienotifier/controller` (`SubscriptionController`)
- Service package: `src/main/java/ar/com/martinrevert/movienotifier/service` (`SubscriptionService`, `MoviePollingService`, `NotificationService`)
- Repository package: `src/main/java/ar/com/martinrevert/movienotifier/repository` (`SubscriptionRepository`, `NotifiedMovieRepository`)
- Model package: `src/main/java/ar/com/martinrevert/movienotifier/model` (`Subscription`, `NotifiedMovie`, `MovieResponse`)
- Runtime config file: `src/main/resources/application.properties`
- Native reflection config: `src/main/resources/META-INF/native-image/ar.com.martinrevert/movie-notifier/reflect-config.json`
- Native build script: `build-native.sh`

## Architecture

```mermaid
flowchart LR
    Client[Client App]
    Api[SubscriptionController]
    SubSvc[SubscriptionService]
    SubRepo[(subscriptions table)]
    Poller[MoviePollingService]
    YTS[YTS API]
    NotifiedRepo[(notified_movies table)]
    NotifSvc[NotificationService]
    FCM[Firebase Cloud Messaging]
    Device[Mobile Device]

    Client -->|subscribe/unsubscribe| Api
    Api --> SubSvc
    SubSvc --> SubRepo

    Poller -->|GET list_movies.json| YTS
    Poller -->|save new movie id| NotifiedRepo
    Poller -->|notify title| NotifSvc
    NotifSvc -->|load subscribers| SubSvc
    NotifSvc -->|send message| FCM
    FCM --> Device
    NotifSvc -->|remove invalid token| SubSvc
```

## Service Flows

### 1) Subscribe (idempotent)

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as SubscriptionController
    participant Svc as SubscriptionService
    participant Repo as SubscriptionRepository
    participant DB as MariaDB

    C->>Ctrl: POST /api/subscriptions/subscribe?token=...
    Ctrl->>Svc: subscribe(token)
    Svc->>Repo: findByRegistrationToken(token)
    Repo->>DB: SELECT by token
    alt token exists
        DB-->>Repo: existing row
        Repo-->>Svc: existing subscription
        Svc-->>Ctrl: existing subscription
    else token does not exist
        DB-->>Repo: empty
        Svc->>Repo: save(subscription)
        Repo->>DB: INSERT
        alt concurrent insert race
            DB-->>Repo: unique constraint error
            Svc->>Repo: findByRegistrationToken(token)
            Repo->>DB: SELECT by token
            Repo-->>Svc: existing subscription
        else insert success
            Repo-->>Svc: saved subscription
        end
        Svc-->>Ctrl: subscription
    end
    Ctrl-->>C: 200 OK + Subscription JSON
```

### 2) Unsubscribe

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as SubscriptionController
    participant Svc as SubscriptionService
    participant Repo as SubscriptionRepository
    participant DB as MariaDB

    C->>Ctrl: POST /api/subscriptions/unsubscribe?token=...
    Ctrl->>Svc: unsubscribe(token)
    Svc->>Repo: findByRegistrationToken(token)
    Repo->>DB: SELECT by token
    alt exists
        Repo->>DB: DELETE
    else not found
        Svc->>Svc: no-op
    end
    Ctrl-->>C: 204 No Content
```

### 3) Poll and dedupe using `notified_movies`

```mermaid
sequenceDiagram
    participant Sch as Spring Scheduler
    participant Poll as MoviePollingService
    participant YTS as YTS API
    participant Notified as NotifiedMovieRepository
    participant DB as MariaDB
    participant Notif as NotificationService

    Sch->>Poll: pollMovies() every movie.polling.fixed-rate-ms
    Poll->>YTS: GET /api/v2/list_movies.json
    YTS-->>Poll: MovieResponse
    loop each movie
        Poll->>Notified: existsById(movieId)
        Notified->>DB: SELECT by movieId
        alt already notified
            Poll->>Poll: skip movie
        else new movie
            Poll->>Notified: saveAndFlush(movieId, title)
            Notified->>DB: INSERT notified_movies
            Poll->>Notif: sendMovieNotification(title)
        end
    end
```

### 4) Notification send and invalid-token cleanup

```mermaid
sequenceDiagram
    participant Notif as NotificationService
    participant SubSvc as SubscriptionService
    participant FCM as FirebaseMessaging
    participant DB as MariaDB

    Notif->>SubSvc: getAllSubscriptions()
    SubSvc->>DB: SELECT subscriptions
    DB-->>SubSvc: tokens
    SubSvc-->>Notif: token list

    loop each token
        Notif->>FCM: send(Message with setToken + notification + data)
        alt success
            FCM-->>Notif: message id
        else terminal token error
            FCM-->>Notif: UNREGISTERED / SENDER_ID_MISMATCH / invalid token
            Notif->>SubSvc: unsubscribe(token)
            SubSvc->>DB: DELETE subscription
        else transient error
            FCM-->>Notif: other exception
            Notif->>Notif: log and continue
        end
    end
```

## REST API

Base path: `/api/subscriptions`

### Subscribe

- `POST /api/subscriptions/subscribe?token=<FCM_REGISTRATION_TOKEN>`
- Returns `200 OK` and `Subscription` JSON.
- Repeating the same token is idempotent (returns existing subscription).

```bash
curl -X POST "http://localhost:10000/api/subscriptions/subscribe?token=<FCM_REGISTRATION_TOKEN>"
```

### Unsubscribe

- `POST /api/subscriptions/unsubscribe?token=<FCM_REGISTRATION_TOKEN>`
- Returns `204 No Content`.

```bash
curl -i -X POST "http://localhost:10000/api/subscriptions/unsubscribe?token=<FCM_REGISTRATION_TOKEN>"
```

Validation:

- Missing or blank `token` returns `400 Bad Request`.

## Configuration

Main file: `src/main/resources/application.properties`

- `server.port=${SERVER_PORT:10000}`
- `spring.threads.virtual.enabled=true`
- `movie.polling.fixed-rate-ms=60000`
- `firebase.service-account-file=serviceAccountKey.json`
- `spring.datasource.url=${SPRING_DATASOURCE_URL:...}`
- `spring.datasource.username=${SPRING_DATASOURCE_USERNAME:kodi}`
- `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:kodi}`

Environment variables take precedence over defaults because properties use `${ENV_VAR:default}` syntax.

## Build and Run (Java 25)

### JVM

```bash
cd /path/to/movie-notifier
./gradlew clean test
./gradlew bootRun
```

### Native AMD64

```bash
cd /path/to/movie-notifier
./build-native.sh
./build/native/nativeCompile/movie-notifier-native
```

### Native ARM64 (cross-build with Docker Buildx)

```bash
cd /path/to/movie-notifier
./build-native.sh aarch64
file build/native/nativeCompile/movie-notifier-native
```

Notes:

- Use `./build-native.sh` (not a Gradle task name).
- Runtime files are copied to `build/native/nativeCompile`:
  - `application.properties`
  - `serviceAccountKey.json` (if present)
- Native image build threads are currently configured in `build.gradle`:
  - `-H:NumberOfThreads=6`

## Portainer (run the native image)

If you export/publish your image and deploy with Portainer, run the container with port mapping `10000:10000` and provide required env vars (or mount config/secret files):

- `SERVER_PORT=10000` (optional, default already 10000)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- Firebase service account file available in container path expected by `firebase.service-account-file`

## Troubleshooting

- `Task 'runBoot' not found`
  - Use `./gradlew bootRun`.
- Native binary missing after build
  - Re-run `./build-native.sh` and verify `build/native/nativeCompile/movie-notifier-native`.
- FCM send returns recipient/token errors
  - Verify client token, Firebase project credentials, and that token belongs to the same sender/project.
- Duplicate push after restart
  - Verify `notified_movies` table persists and `spring.jpa.hibernate.ddl-auto` is not dropping schema.

## Security Reminder

- Do not commit production secrets.
- Keep `serviceAccountKey.json` private.
- Prefer environment-based DB credentials in production.
