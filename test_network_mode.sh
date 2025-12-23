#!/bin/bash
# Test script for ForgeHeadlessServer Network Mode
# This script demonstrates how to start the headless server with network mode enabled

set -e

echo "==================================================="
echo "ForgeHeadlessServer Network Mode Test"
echo "==================================================="
echo ""

# Check if JAR exists
JAR_PATH="forge-gui-desktop/target/forge-gui-desktop-2.0.08-SNAPSHOT-jar-with-dependencies.jar"
if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found at $JAR_PATH"
    echo "Please build the project first with: mvn clean package -DskipTests"
    exit 1
fi

echo "✓ Found JAR file: $JAR_PATH"
echo ""

# Display help
echo "=== Displaying Help ==="
java -cp "$JAR_PATH" forge.view.ForgeHeadlessServer --help
echo ""

echo "==================================================="
echo "Starting headless server with network mode..."
echo "Press Ctrl+C to stop the server"
echo "==================================================="
echo ""
echo "To connect from GUI:"
echo "1. Launch Forge Desktop Application"
echo "2. Go to: Online Multiplayer > Lobby"  
echo "3. Click: Connect to Server"
echo "4. Enter: localhost:9999"
echo "5. Select your deck and click Ready"
echo ""
echo "==================================================="
echo ""

# Start the server
java -cp "$JAR_PATH" forge.view.ForgeHeadlessServer --network --network-port 9999
