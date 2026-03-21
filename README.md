# Movie Notifier Application

This is a Spring Boot application designed to notify users about new movie releases. It periodically polls a movie database API (YTS.ag) and sends push notifications via Firebase Cloud Messaging (FCM) for any newly detected movies.

## Features

*   **Scheduled Polling**: Automatically checks for new movie releases at regular intervals.
*   **Duplicate Prevention**: Keeps track of already notified movies to avoid sending redundant alerts.
*   **Firebase Cloud Messaging (FCM)**: Leverages FCM to deliver push notifications to subscribed clients.
*   **Native Image Support**: Optimized for GraalVM Native Image compilation, providing faster startup times and reduced memory consumption.
*   **Configurable Firebase**: Uses a `serviceAccountKey.json` file for secure Firebase integration.

## Architecture

The application follows a microservice-like architecture, with distinct services handling specific responsibilities.

```
+-------------------+       +-------------------+       +-------------------+
| MoviePollingService | <---> | RestTemplate      | <---> | YTS.ag API        |
+-------------------+       +-------------------+       +-------------------+
         |                                                       ^
         | (New Movie Detected)                                  |
         v                                                       |
+-------------------+       +-------------------+       +-------------------+
| NotificationService | <---> | FirebaseMessaging | <---> | FCM (Firebase)    |
+-------------------+       +-------------------+       +-------------------+
         ^                                                       |
         |                                                       |
         | (FirebaseConfig init)                                 |
+-------------------+                                            |
| FirebaseConfig    |                                            |
+-------------------+                                            |
         ^                                                       |
         |                                                       |
         | (Spring Boot App)                                     |
+----------------------------------------------------------------+
| MovieNotifierApplication (Spring Boot Context)                 |
+----------------------------------------------------------------+
```

*   **MovieNotifierApplication**: The main Spring Boot entry point, setting up the application context.
*   **FirebaseConfig**: Initializes the Firebase Admin SDK using a service account key.
*   **MoviePollingService**: Contains the scheduled logic to fetch movie data from the YTS.ag API.
*   **NotificationService**: Handles sending messages via Firebase Cloud Messaging.

## Sequence Flows

### Movie Polling Flow

This diagram illustrates how the application polls for new movies and triggers notifications.

```
+-----------------------+     +-------------------+     +-------------------+
| MoviePollingService   |     | RestTemplate      |     | YTS.ag API        |
+-----------------------+     +-------------------+     +-------------------+
           |                           |                           |
           |  1. @Scheduled poll()     |                           |
           |-------------------------->|                           |
           |                           |  2. GET /list_movies.json |
           |                           |-------------------------->|
           |                           |                           |  3. Returns JSON
           |                           |<--------------------------|
           |  4. Process Response      |                           |
           |  (Check for new movies)   |                           |
           |                           |                           |
           |  5. If new movie:         |                           |
           |     Call NotificationService.sendMovieNotification()  |
           |------------------------------------------------------>|
```

### Notification Sending Flow

This diagram shows how a notification is constructed and sent via FCM.

```
+-----------------------+     +-------------------+     +-------------------+
| NotificationService   |     | FirebaseMessaging |     | FCM (Firebase)    |
+-----------------------+     +-------------------+     +-------------------+
           |                           |                           |
           |  1. sendMovieNotification(title)                      |
           |-------------------------->|                           |
           |                           |  2. Build Message         |
           |                           |  (with title)             |
           |                           |-------------------------->|
           |                           |                           |  3. Send Push Notification
           |                           |<--------------------------|
           |                           |                           |
```

## Getting Started

### Prerequisites

*   **Java Development Kit (JDK)**: Version 17 or 21 (for running Gradle locally).
*   **Gradle**: The project uses the Gradle Wrapper, so a local Gradle installation is not strictly required.
*   **Docker**: Required for cross-compilation to AARCH64 or if local GraalVM is not available.
*   **Firebase Service Account Key**: A `serviceAccountKey.json` file from your Firebase project. Place this file in the `movie-notifier/` directory.

### Building the Application

Navigate to the `movie-notifier/` directory in your terminal.

#### 1. Local AMD64 Build (Native Image)

This builds a native executable for your current AMD64 (x86_64) system.

```bash
# Ensure your JAVA_HOME points to a stable JDK (e.g., OpenJDK 21)
# Ensure GRAALVM_HOME points to your GraalVM installation (e.g., /opt/graalvm-amd64)
# The build-native.sh script handles these environment variables.
./build-native.sh
```

The executable will be generated in `build/native/nativeCompile/movie-notifier-native`.

#### 2. AARCH64 (ARM) Build using Docker Buildx

This builds a native executable for ARM64 architecture using Docker's multi-architecture capabilities.

```bash
./build-native.sh aarch64
```

The executable will be generated in `build/native/nativeCompile/movie-notifier-native`.

### Running the Application

After a successful build, navigate to the output directory and run the executable:

```bash
cd build/native/nativeCompile
./movie-notifier-native
```

The application will start, initialize Firebase, and begin polling for movies.

## Cross-Compilation for AARCH64 (ARM)

The `build-native.sh` script is configured to build a native executable for AARCH64 (ARM) architecture using Docker Buildx. This allows you to build ARM binaries even if your host machine is AMD64.

### Prerequisites for Host Machine

To make the AARCH64 cross-compilation work, your host machine (e.g., your AMD64 Ubuntu desktop) needs the following:

1.  **Docker Installed and Running**:
    *   Install: `sudo apt-get update && sudo apt-get install -y docker.io docker-buildx`
    *   Start: `sudo systemctl start docker`
    *   Enable: `sudo systemctl enable docker`
    *   **User Permissions**: To run Docker commands without `sudo`, add your user to the `docker` group: `sudo usermod -aG docker $USER`. You will need to **log out and log back in** for this change to take effect. If you don't do this, you'll need to prefix Docker commands (or the script) with `sudo`.

2.  **Docker Buildx Plugin**: This is usually installed with the `docker-buildx` package. It's crucial for multi-architecture builds.

3.  **QEMU binfmt support**: Docker Buildx uses QEMU to emulate ARM architecture. The `build-native.sh` script attempts to enable this automatically by running `docker run --privileged --rm tonistiigi/binfmt --install all`. This requires Docker to be running and your host kernel to support `binfmt_misc`.

4.  **Internet Connection**: Required to pull Docker images (e.g., `ghcr.io/graalvm/native-image-community:21`, `tonistiigi/binfmt`) which provide the ARM build environment.

5.  **`serviceAccountKey.json`**: Your Firebase service account key file must be placed in the project root (`movie-notifier/`) for the application to run. The script will copy it to the build output directory.

### How it Works

When you run `./build-native.sh aarch64`:

*   The script checks for Docker and sets up Buildx.
*   It generates a `Dockerfile.native` on the fly.
*   This Dockerfile uses a `ghcr.io/graalvm/native-image-community:21` image (which is a GraalVM distribution for ARM64).
*   Docker Buildx then builds this image, running the `gradlew nativeCompile` command *inside an emulated ARM64 environment*.
*   Finally, the resulting ARM64 native executable is copied back to your host machine's `build/native/nativeCompile/` directory.

### Usage

To build the AARCH64 native executable:

```bash
./build-native.sh aarch64
```

### Important Notes

*   **Performance**: Building ARM64 binaries on an AMD64 host via QEMU emulation can be significantly slower than building natively on an ARM64 machine.
*   **Running ARM64 Executables**: The generated `movie-notifier-native` executable for AARCH64 can only be run on an actual ARM64 machine or on an AMD64 machine with QEMU user emulation properly configured (which Docker handles for the build process, but you'd need to set up for running the executable directly).
*   **`serviceAccountKey.json`**: Remember to place your `serviceAccountKey.json` in the `movie-notifier/` directory before building, so it can be copied alongside the executable.
