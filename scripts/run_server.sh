#!/bin/bash
# Build and run ForgeHeadlessServer
# Usage: ./scripts/run_server.sh [--skip-build]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
JAR_PATH="$PROJECT_DIR/forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar"

cd "$PROJECT_DIR"

# Kill any existing server on port 8080
EXISTING_PID=$(lsof -ti :8080 2>/dev/null || true)
if [ -n "$EXISTING_PID" ]; then
    echo "🔪 Killing existing server (PID: $EXISTING_PID)..."
    kill $EXISTING_PID 2>/dev/null || true
    sleep 1
fi

# Build unless --skip-build is passed
if [[ "$1" != "--skip-build" ]]; then
    echo "🔨 Building..."
    ./apache-maven-3.9.6/bin/mvn clean package -DskipTests -pl forge-gui-desktop -am -q
    echo "✅ Build complete"
fi

echo "🚀 Starting ForgeHeadlessServer on port 8080..."
java -cp "$JAR_PATH" forge.view.ForgeHeadlessServer
