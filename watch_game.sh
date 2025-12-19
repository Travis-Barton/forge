#!/bin/bash
#
# watch_game.sh - Launch Forge GUI to watch/debug AI agent games
#
# Usage:
#   ./watch_game.sh --ai-endpoint http://localhost:5005  # With AI agent endpoint
#   ./watch_game.sh --verbose                            # Verbose logging
#   ./watch_game.sh --both-ai                            # AI vs AI mode
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Find Java 17 installation
if [ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/opt/openjdk@17/bin/java"
    echo "Using Homebrew Java 17: $JAVA_CMD"
elif [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/opt/openjdk/bin/java"
    echo "Using Homebrew Java: $JAVA_CMD"
elif [ -x "/opt/homebrew/opt/openjdk@21/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/opt/openjdk@21/bin/java"
    echo "Using Homebrew Java 21: $JAVA_CMD"
elif [ -x "/usr/local/opt/openjdk@17/bin/java" ]; then
    JAVA_CMD="/usr/local/opt/openjdk@17/bin/java"
    echo "Using Homebrew Java 17 (Intel): $JAVA_CMD"
elif [ -d "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home" ]; then
    JAVA_CMD="/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/bin/java"
    echo "Using system Java 17: $JAVA_CMD"
else
    JAVA_CMD="java"
    echo "WARNING: Java 17 not found, using system java (may fail)"
fi

# Find the Forge JAR file
FORGE_JAR=$(find "$SCRIPT_DIR/forge-gui-desktop/target" -name "forge-gui-desktop-*-jar-with-dependencies.jar" 2>/dev/null | head -1)

if [ -z "$FORGE_JAR" ]; then
    echo "ERROR: Could not find Forge JAR file."
    echo "Make sure you've built Forge with: mvn package -DskipTests"
    echo "Looking in: $SCRIPT_DIR/forge-gui-desktop/target/"
    exit 1
fi

echo "Using Forge JAR: $FORGE_JAR"
echo "Starting Forge in watch-game mode..."
echo ""

# Pass all arguments plus the --watch-game and --verbose flags
# Use -cp and specify main class to bypass default Main (which doesn't support our flags)
"$JAVA_CMD" -cp "$FORGE_JAR" forge.view.ForgeHeadless --gui --verbose "$@"
