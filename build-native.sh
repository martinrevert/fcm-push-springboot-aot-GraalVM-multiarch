#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

OUTPUT_DIR="build/native/nativeCompile"
BINARY_NAME="movie-notifier-native"
BINARY_PATH="$OUTPUT_DIR/$BINARY_NAME"
ARM64_GRAALVM_IMAGE="ghcr.io/graalvm/native-image-community:25"
IMAGE_PLATFORM="linux/arm64"
ARM64_MARCH="${ARM64_MARCH:-compatibility}"
MAX_RAM_GB="${MAX_RAM_GB:-8}"
GRADLE_WORKERS="${GRADLE_WORKERS:-2}"
GRADLE_HEAP_XMX="${GRADLE_HEAP_XMX:-6g}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$SCRIPT_DIR/.gradle}"
BUILDX_CACHE_DIR="${BUILDX_CACHE_DIR:-$SCRIPT_DIR/.buildx-cache}"
IMAGE_REPO="${IMAGE_REPO:-movie-notifier-native}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
IMAGE_NAME_ARM64="${IMAGE_REPO}:${IMAGE_TAG}-arm64"
IMAGE_TAR_PATH="${IMAGE_TAR_PATH:-build/native/docker/${BINARY_NAME}-${IMAGE_TAG}-arm64.tar}"
BUILDER_NAME="movie-notifier-builder"
ARM64_NO_CACHE="${ARM64_NO_CACHE:-false}"
BUILDX_CACHE_ARGS=(--cache-from=type=local,src="$BUILDX_CACHE_DIR" --cache-to=type=local,mode=max,dest="$BUILDX_CACHE_DIR")

mkdir -p "$GRADLE_USER_HOME"
mkdir -p "$BUILDX_CACHE_DIR"
export GRADLE_USER_HOME

# Keep the native compilation under ~8 GiB total memory to avoid swapping.
# Prefer a smaller heap than the host RAM cap, leaving headroom for the OS and Docker.
export GRADLE_OPTS="-Xmx${GRADLE_HEAP_XMX} -Xms512m -XX:MaxMetaspaceSize=1g -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=${GRADLE_WORKERS}"
export JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=${GRADLE_WORKERS} -Xmx${GRADLE_HEAP_XMX} -Xms512m -XX:MaxMetaspaceSize=1g"

# Default to a safer, lower-memory build profile so the command is simple for day-to-day use.
if [ -z "${MAX_RAM_GB:-}" ]; then
    MAX_RAM_GB=8
fi
if [ -z "${GRADLE_WORKERS:-}" ]; then
    GRADLE_WORKERS=2
fi
if [ -z "${GRADLE_HEAP_XMX:-}" ]; then
    GRADLE_HEAP_XMX=6g
fi

copy_runtime_artifacts() {
    mkdir -p "$OUTPUT_DIR"
    if [ -f "src/main/resources/application.properties" ]; then
        cp src/main/resources/application.properties "$OUTPUT_DIR/"
        echo "Copied application.properties to $OUTPUT_DIR/."
    fi
    echo "Firebase credentials are expected to be mounted as a Docker volume at runtime; not copied into the image."
}

write_native_dockerfile() {
    cat <<EOF > Dockerfile.native
FROM $ARM64_GRAALVM_IMAGE AS builder
WORKDIR /app
COPY . .
# Install dependencies for gradlew (findutils provides xargs)
RUN microdnf install -y findutils bash
RUN chmod +x gradlew
# Run native build (CLEAN first to avoid stale AOT classes)
# Force Gradle to use the container's Java install instead of host-specific paths.
RUN export JAVA_HOME="\$(dirname "\$(dirname "\$(readlink -f "\$(command -v java)")")")" && \
    export GRADLE_USER_HOME="/root/.gradle" && \
    export GRADLE_OPTS="-Xmx${GRADLE_HEAP_XMX} -Xms512m -XX:MaxMetaspaceSize=1g -Dorg.gradle.daemon=false -Dorg.gradle.workers.max=${GRADLE_WORKERS} -Dorg.gradle.caching=true -Dorg.gradle.configuration-cache=true" && \
    export JAVA_TOOL_OPTIONS="-XX:ActiveProcessorCount=${GRADLE_WORKERS} -Xmx${GRADLE_HEAP_XMX} -Xms512m -XX:MaxMetaspaceSize=1g" && \
    ./gradlew --no-daemon --build-cache --configuration-cache --parallel --max-workers=${GRADLE_WORKERS} nativeCompile \
      -Dorg.gradle.jvmargs="-Xmx${GRADLE_HEAP_XMX} -Xms512m -XX:MaxMetaspaceSize=1g -Dorg.gradle.daemon=false -Dorg.gradle.caching=true -Dorg.gradle.configuration-cache=true" \
      -PnativeTargetArch=arm64 \
      -PnativeArmMarch="$ARM64_MARCH" \
      -Dorg.gradle.java.installations.paths="\$JAVA_HOME" \
      -Dorg.gradle.java.installations.auto-download=false

FROM scratch AS binary-export
COPY --from=builder /app/build/native/nativeCompile/movie-notifier-native /

FROM debian:bookworm-slim AS runtime
WORKDIR /opt/movie-notifier
COPY --from=builder /app/build/native/nativeCompile/movie-notifier-native ./movie-notifier-native
COPY --from=builder /app/src/main/resources/application.properties ./application.properties
# Firebase credentials are mounted as a Docker volume at runtime and are intentionally not baked into the image.
RUN chmod +x /opt/movie-notifier/movie-notifier-native
EXPOSE 10000
ENTRYPOINT ["/opt/movie-notifier/movie-notifier-native"]
EOF
}

ensure_buildx_builder() {
    if ! docker buildx inspect "$BUILDER_NAME" &> /dev/null; then
        docker buildx create --name "$BUILDER_NAME" --use --driver docker-container
    else
        docker buildx use "$BUILDER_NAME"
    fi

    docker buildx inspect --bootstrap > /dev/null
}

# Function to check for Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "Error: Docker is not installed or not in PATH."
        echo "Install Docker + Buildx first, then run this script again."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        echo "Error: Docker daemon is not accessible by user '$USER'."
        echo "Run one-time host setup (outside this script), then re-login:"
        echo "  sudo usermod -aG docker $USER"
        echo "  sudo systemctl enable --now docker"
        echo "  newgrp docker"
        exit 1
    fi

    if ! docker buildx version &> /dev/null; then
        echo "Error: Docker Buildx is not available."
        echo "Install Docker Buildx plugin and retry."
        exit 1
    fi

    echo "Docker and Buildx are available."
}

# Function for ARM64 build via Docker
build_arm64() {
    echo "Starting ARM64 build using Docker..."
    echo "Using ARM64 native baseline: -march=$ARM64_MARCH"
    if [ "${#BUILDX_CACHE_ARGS[@]}" -gt 0 ]; then
        echo "Docker Buildx cache mode: enabled (local cache reused from $BUILDX_CACHE_DIR)"
    fi
    mkdir -p "$OUTPUT_DIR"
    mkdir -p "$(dirname "$IMAGE_TAR_PATH")"
    rm -f "$BINARY_PATH"
    rm -f "$IMAGE_TAR_PATH"

    # Enable QEMU for multi-arch
    echo "Enabling QEMU for multi-arch builds..."
    docker run --privileged --rm tonistiigi/binfmt --install all || echo "Warning: Failed to enable QEMU binfmt. Build might fail if not already enabled."

    ensure_buildx_builder

    echo "Building ARM64 native binary..."
    write_native_dockerfile
    docker buildx build \
        "${BUILDX_CACHE_ARGS[@]}" \
        --platform "$IMAGE_PLATFORM" \
        --target binary-export \
        --memory "${MAX_RAM_GB}g" \
        --memory-swap "${MAX_RAM_GB}g" \
        -f Dockerfile.native \
        --output type=local,dest="$OUTPUT_DIR" \
        .

    if [ ! -f "$BINARY_PATH" ]; then
        echo "ARM64 build finished but binary was not exported to '$BINARY_PATH'."
        exit 1
    fi

    chmod +x "$BINARY_PATH"
    echo "ARM64 Build SUCCESSFUL!"
    echo "Native binary exported to: $BINARY_PATH"

    echo "Building ARM64 runtime Docker image archive..."
    docker buildx build \
        "${BUILDX_CACHE_ARGS[@]}" \
        --platform "$IMAGE_PLATFORM" \
        --target runtime \
        --memory "${MAX_RAM_GB}g" \
        --memory-swap "${MAX_RAM_GB}g" \
        -f Dockerfile.native \
        -t "$IMAGE_NAME_ARM64" \
        --output type=docker,dest="$IMAGE_TAR_PATH" \
        .

    rm Dockerfile.native

    if [ ! -f "$IMAGE_TAR_PATH" ]; then
        echo "ARM64 image build finished but archive was not generated at '$IMAGE_TAR_PATH'."
        exit 1
    fi

    echo "Docker image archive exported to: $IMAGE_TAR_PATH"
    echo "Image tag inside archive: $IMAGE_NAME_ARM64"
    echo "Native image CPU baseline used: -march=$ARM64_MARCH"
    echo
    echo "Next steps:"
    echo "  docker load -i \"$IMAGE_TAR_PATH\""
    echo "  docker push \"$IMAGE_NAME_ARM64\""

    copy_runtime_artifacts
}

# Main logic
ARCH_ARG="${1:-amd64}"

if [ "$ARM64_NO_CACHE" == "true" ] || [ "$ARM64_NO_CACHE" == "1" ]; then
    BUILDX_CACHE_ARGS=(--no-cache --pull)
fi

if [ "${2:-}" == "--no-cache" ]; then
    BUILDX_CACHE_ARGS=(--no-cache --pull)
elif [ -n "${2:-}" ]; then
    echo "Error: Unknown option '$2'."
    echo "Usage: ./build-native.sh [aarch64|arm64] [--no-cache]"
    exit 1
fi

if [ "$ARCH_ARG" == "aarch64" ] || [ "$ARCH_ARG" == "arm64" ]; then
    check_docker
    build_arm64
else
    # AMD64 Local Build Logic
    JAVA_HOME_PATH="/opt/graalvm-amd64"
    GRAALVM_HOME_AMD64="/opt/graalvm-amd64"

    # Verify local tools
    if [ ! -d "$JAVA_HOME_PATH" ]; then
        echo "Error: JAVA_HOME '$JAVA_HOME_PATH' not found."
        echo "Please install GraalVM Java 25 or update script path."
        exit 1
    fi

    if [ ! -d "$GRAALVM_HOME_AMD64" ]; then
         echo "Error: GraalVM '$GRAALVM_HOME_AMD64' not found."
         exit 1
    fi

    echo "Building locally for AMD64..."
    export JAVA_HOME="$JAVA_HOME_PATH"
    export GRAALVM_HOME="$GRAALVM_HOME_AMD64"
    export PATH="$JAVA_HOME/bin:$PATH"
    rm -f "$BINARY_PATH"

    # Clean first!
    ./gradlew nativeCompile --no-daemon

    if [ ! -f "$BINARY_PATH" ]; then
        echo "AMD64 build reported success but binary was not found at '$BINARY_PATH'."
        exit 1
    fi

    chmod +x "$BINARY_PATH"
    echo "AMD64 Build SUCCESSFUL!"
    copy_runtime_artifacts
fi
