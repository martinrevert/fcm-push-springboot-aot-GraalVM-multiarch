# Movie Notifier

Spring Boot service that polls YTS and sends Firebase push notifications for new movies.

## What It Does

- Polls movie data on a schedule.
- Avoids duplicate notifications.
- Sends push notifications through Firebase Admin SDK.
- Supports JVM run and GraalVM native executable builds.

## Project Layout

- App entrypoint: `src/main/java/com/example/movienotifier/MovieNotifierApplication.java`
- Data source config: `src/main/java/com/example/movienotifier/config/DataSourceConfig.java`
- Main config: `src/main/resources/application.properties`
- Native reflection config: `src/main/resources/META-INF/native-image/com.example/movie-notifier/reflect-config.json`
- Native build script: `build-native.sh`

## Prerequisites

- Linux (script is Bash-based).
- Java 21 installed at `/usr/lib/jvm/java-21-openjdk-amd64` (or adapt `build-native.sh`).
- GraalVM installed at `/opt/graalvm-amd64` for local AMD64 native builds (or adapt `build-native.sh`).
- Docker + Buildx for AARCH64 cross-builds.
- Firebase key file: `serviceAccountKey.json` in project root.

## Known Good Build Matrix

This matrix is an evidence-backed snapshot from project files and recent local build logs, not a universal compatibility guarantee.

| Component | Version / Value | Source |
|---|---|---|
| Spring Boot plugin | `4.0.4` | `build.gradle` |
| GraalVM Native Build Tools plugin | `0.11.1` | `build.gradle` |
| Java source/target toolchain | `21` | `build.gradle` |
| Native Graal launcher constraint | GraalVM Java `25` | `build.gradle` |
| Gradle wrapper | `8.14.3` | `gradle/wrapper/gradle-wrapper.properties` |
| Foojay resolver plugin | `0.9.0` | `settings.gradle` |
| Local AMD64 GraalVM path expected by script | `/opt/graalvm-amd64` | `build-native.sh` |
| Local Java path expected by script | `/usr/lib/jvm/java-21-openjdk-amd64` | `build-native.sh` |
| ARM64 Docker image used by script | `ghcr.io/graalvm/native-image-community:21` | `build-native.sh` |
| Tomcat at native runtime (observed) | `11.0.18` | recent native run logs |
| Hibernate ORM at native runtime (observed) | `7.2.7.Final` | recent native run logs |

## Run on JVM

Use Spring Boot task `bootRun` (not `runBoot`).

```bash
cd /home/tincho/VS_STUDIO_PROJECTS/FCM_La_Torrentola/movie-notifier
./gradlew bootRun
```

## Build Native (Recommended: Script)

### AMD64 local build

```bash
cd /home/tincho/VS_STUDIO_PROJECTS/FCM_La_Torrentola/movie-notifier
./build-native.sh
```

### AARCH64 cross-build (Docker)

```bash
cd /home/tincho/VS_STUDIO_PROJECTS/FCM_La_Torrentola/movie-notifier
./build-native.sh aarch64
```

Both flows place artifacts in:

- Binary: `build/native/nativeCompile/movie-notifier-native`
- Copied config: `build/native/nativeCompile/application.properties`
- Copied Firebase key (if present): `build/native/nativeCompile/serviceAccountKey.json`

## Run Native Binary

```bash
cd /home/tincho/VS_STUDIO_PROJECTS/FCM_La_Torrentola/movie-notifier/build/native/nativeCompile
./movie-notifier-native
```

## Configuration

Main runtime config is in `src/main/resources/application.properties`.

Current app expects:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `firebase.service-account-file=serviceAccountKey.json`

`DataSourceConfig` builds Hikari using `org.mariadb.jdbc.MariaDbDataSource` and passes URL/user/password as datasource properties.

## Native Notes

### Toolchain behavior

`build.gradle` uses:

- Spring Boot `4.0.4`
- Graal plugin `0.11.1`
- Java toolchain source/target 21
- Native launcher lookup for GraalVM Java 25

If native compile fails with toolchain lookup errors, use `./build-native.sh` and verify GraalVM path in the script.

### Reflection metadata

Hibernate 7 on native may require explicit reflection entries. This project keeps them in:

- `src/main/resources/META-INF/native-image/com.example/movie-notifier/reflect-config.json`

Recent required entries include:

- `org.hibernate.event.spi.PreFlushEventListener[]`
- `org.hibernate.event.spi.PostFlushEventListener[]`

If you get `MissingReflectionRegistrationError` for another type, add that exact type in `reflect-config.json`, rebuild, and rerun.

## Troubleshooting

- `Task 'runBoot' not found`:
  - Use `./gradlew bootRun`.
- `./movie-notifier-native: No such file or directory`:
  - Build did not finish successfully. Re-run `./build-native.sh` and verify binary exists in `build/native/nativeCompile`.
- Native app starts but DB auth fails:
  - Check values in `application.properties` and that runtime config file is copied into native output directory.
- Native build fails with SLF4J image heap/provider errors:
  - Re-check native build args in `build.gradle` and use the project defaults.

## Security Reminder

Do not commit real production secrets.

- `serviceAccountKey.json` should stay private.
- Consider moving DB credentials to environment variables or external config for production.
