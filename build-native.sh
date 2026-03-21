#!/bin/bash
set -e

OUTPUT_DIR="build/native/nativeCompile"
BINARY_NAME="movie-notifier-native"
BINARY_PATH="$OUTPUT_DIR/$BINARY_NAME"
ARM64_GRAALVM_IMAGE="ghcr.io/graalvm/native-image-community:25"

# Function to check for Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "Docker not found. Installing Docker..."
        # Try updating, but don't fail if some repos are unreachable
        sudo apt-get update || echo "Warning: apt-get update had errors, proceeding..."
        sudo apt-get install -y docker.io docker-buildx || { echo "Failed to install Docker via apt-get."; exit 1; }
        sudo systemctl start docker || echo "Warning: Could not start Docker service via systemctl."
        sudo systemctl enable docker || echo "Warning: Could not enable Docker service."

        # Add current user to docker group (requires re-login or newgrp usually)
        if ! getent group docker > /dev/null; then
            sudo groupadd docker
        fi
        sudo usermod -aG docker $USER || echo "Warning: Could not add user to docker group."
        echo "Docker installed. Please re-login for group changes to take effect, or run this script with sudo."
    else
        echo "Docker is already installed."
    fi
}

# Function for ARM64 build via Docker
build_arm64() {
    echo "Starting ARM64 build using Docker..."
    rm -f "$BINARY_PATH"

    # Ensure buildx is available
    if ! docker buildx version &> /dev/null; then
        echo "Installing/Enabling Docker Buildx..."
        # Check if buildx plugin is installed
        if [ ! -f ~/.docker/cli-plugins/docker-buildx ]; then
             mkdir -p ~/.docker/cli-plugins
             # Try to download specific version or use apt if available
             # Simply rely on 'docker buildx' command from docker-buildx package installed earlier
             echo "Assuming docker-buildx package installed correctly."
        fi
    fi

    # Enable QEMU for multi-arch
    echo "Enabling QEMU for multi-arch builds..."
    docker run --privileged --rm tonistiigi/binfmt --install all || echo "Warning: Failed to enable QEMU binfmt. Build might fail if not already enabled."

    # Create builder
    BUILDER_NAME="mybuilder"
    if ! docker buildx inspect $BUILDER_NAME &> /dev/null; then
        docker buildx create --name $BUILDER_NAME --use --driver docker-container
    else
        docker buildx use $BUILDER_NAME
    fi

    # Inspect builder just in case
    docker buildx inspect --bootstrap

    echo "Building Docker image for linux/arm64..."

    # Create Dockerfile
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
    ./gradlew clean nativeCompile --no-daemon \
      -Dorg.gradle.java.installations.paths="\$JAVA_HOME" \
      -Dorg.gradle.java.installations.auto-download=false

# Export binary
FROM scratch
COPY --from=builder /app/build/native/nativeCompile/movie-notifier-native /
EOF

    # Build and export
    docker buildx build --platform linux/arm64 -f Dockerfile.native -t movie-notifier-native:arm64 --output type=local,dest="$OUTPUT_DIR" .

    rm Dockerfile.native

    if [ ! -f "$BINARY_PATH" ]; then
        echo "ARM64 build finished but binary was not exported to '$BINARY_PATH'."
        exit 1
    fi

    chmod +x "$BINARY_PATH"
    echo "ARM64 Build SUCCESSFUL!"
    echo "Native binary exported to: $BINARY_PATH"

    if [ -f "serviceAccountKey.json" ]; then
        cp serviceAccountKey.json "$OUTPUT_DIR/"
        echo "Copied serviceAccountKey.json."
    fi
    if [ -f "src/main/resources/application.properties" ]; then
        cp src/main/resources/application.properties "$OUTPUT_DIR/"
        echo "Copied application.properties to $OUTPUT_DIR/."
    fi
}

# Main logic
if [ "$1" == "aarch64" ] || [ "$1" == "arm64" ]; then
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
         # Fallback to Docker for AMD64 if local GraalVM missing?
         echo "Attempting to fallback to Docker for AMD64 build..."
         check_docker
         # Use standard docker build for current arch
         docker build -t movie-notifier-native .
         # Extract binary... complex without buildx --output local
         # Use buildx for amd64 too for consistency
         docker buildx build --platform linux/amd64 -f Dockerfile.native -t movie-notifier-native:amd64 --output type=local,dest=build/native/nativeCompile .
         exit 0
    fi

    echo "Building locally for AMD64..."
    export JAVA_HOME="$JAVA_HOME_PATH"
    export GRAALVM_HOME="$GRAALVM_HOME_AMD64"
    export PATH="$JAVA_HOME/bin:$PATH"
    rm -f "$BINARY_PATH"

    # Clean first!
    ./gradlew clean nativeCompile

    if [ $? -eq 0 ]; then
        if [ ! -f "$BINARY_PATH" ]; then
            echo "AMD64 build reported success but binary was not found at '$BINARY_PATH'."
            exit 1
        fi

        chmod +x "$BINARY_PATH"
        echo "AMD64 Build SUCCESSFUL!"
        if [ -f "serviceAccountKey.json" ]; then
            cp serviceAccountKey.json "$OUTPUT_DIR/"
            echo "Copied serviceAccountKey.json."
        fi
        if [ -f "src/main/resources/application.properties" ]; then
            cp src/main/resources/application.properties "$OUTPUT_DIR/"
            echo "Copied application.properties to $OUTPUT_DIR/."
        fi
    else
        echo "Build FAILED."
        exit 1
    fi
fi
