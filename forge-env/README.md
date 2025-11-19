# Forge Headless Environment

A headless, programmatic environment wrapper for the Forge MTG engine. This module allows you to initialize and run Magic: The Gathering games without GUI, perfect for AI training, automated testing, or integration with other systems.

## Features

- **Headless Operation**: No GUI dependencies - runs purely in-memory
- **Random Deck Selection**: Automatically selects two random prebuilt Standard decks
- **JSON State Export**: Game state is exported as structured JSON
- **1v1 Standard Format**: Initialized for constructed 1v1 matches
- **Auto-Keep Mulligans**: Both players automatically keep opening hands (configurable for V2)
- **Reusable**: Can create multiple games in the same JVM process

## Requirements

- Java 17 or higher
- Maven 3.8.1 or higher
- Forge resources directory (containing cardsfolder, editions, decks, etc.)

## Usage

### Basic Usage

```java
import forge.env.ForgeEnv;

public class Example {
    public static void main(String[] args) {
        // Initialize once at startup with path to forge resources
        ForgeEnv.initialize("/path/to/forge-gui/res");
        
        // Create a new game
        String gameStateJson = ForgeEnv.newGame();
        System.out.println(gameStateJson);
        
        // Create another game
        String anotherGame = ForgeEnv.newGame();
        System.out.println(anotherGame);
    }
}
```

### Using Default Resource Path

```java
// Uses default path "forge-gui/res" relative to working directory
ForgeEnv.initialize();
String gameStateJson = ForgeEnv.newGame();
```

### Checking Initialization Status

```java
if (!ForgeEnv.isInitialized()) {
    ForgeEnv.initialize();
}

int deckCount = ForgeEnv.getStandardDeckCount();
System.out.println("Loaded " + deckCount + " Standard decks");
```

## JSON Output Format

The `newGame()` method returns a JSON string with the following structure:

```json
{
  "players": [
    {
      "id": 1,
      "name": "Player 1",
      "life": 20,
      "deckName": "Abzan Siege",
      "libraryCount": 53,
      "hand": [
        { "id": 12345, "cardName": "Forest" },
        { "id": 12346, "cardName": "Plains" },
        // ... 7 cards total
      ],
      "graveyard": [],
      "exile": [],
      "battlefield": []
    },
    {
      "id": 2,
      "name": "Player 2",
      "life": 20,
      "deckName": "Another Deck",
      "libraryCount": 53,
      "hand": [
        // ... 7 cards
      ],
      "graveyard": [],
      "exile": [],
      "battlefield": []
    }
  ],
  "activePlayerId": 1,
  "phase": "Main Phase 1",
  "turn": 1
}
```

## Implementation Details

### Game Initialization Process

1. **Deck Selection**: Randomly selects two distinct prebuilt Standard decks
2. **Player Creation**: Creates two players with AI controllers
3. **Match Setup**: Creates a 1v1 constructed match with standard rules
4. **Game Start**: 
   - Shuffles both libraries
   - Draws 7 cards for each player
   - Both players automatically keep (no mulligan phase)
   - Randomly determines starting player
   - Begins at Turn 1, Main Phase 1
5. **State Export**: Serializes the complete game state to JSON

### Controller Behavior

Both players are assigned `PlayerControllerAi` instances from the forge-ai module. These controllers:
- Automatically keep opening hands (no mulligan decisions)
- Do not make any game actions automatically
- Are ready for programmatic control or AI decision-making

### Thread Safety

`ForgeEnv` uses static state and is not thread-safe. If you need concurrent game creation, synchronize access to `initialize()` and `newGame()` methods.

## API Reference

### `ForgeEnv.initialize()`
Initializes the Forge environment with the default resource directory path (`"forge-gui/res"`).

### `ForgeEnv.initialize(String resourcePath)`
Initializes the Forge environment with a custom resource directory path.

**Parameters:**
- `resourcePath`: Absolute or relative path to the Forge resource directory

**Throws:**
- `IllegalStateException` if the resource directory is not found

### `ForgeEnv.newGame()`
Creates and initializes a new 1v1 Standard game with random prebuilt decks.

**Returns:**
- JSON string representation of the initial game state

**Throws:**
- `IllegalStateException` if called before `initialize()`

### `ForgeEnv.isInitialized()`
Checks if the environment has been initialized.

**Returns:**
- `true` if initialized, `false` otherwise

### `ForgeEnv.getStandardDeckCount()`
Gets the number of Standard decks available.

**Returns:**
- Number of loaded Standard decks, or 0 if not initialized

### `ForgeEnv.reset()` (Package-private)
Resets the environment state. Used primarily for testing.

## Building

```bash
# Build just the forge-env module
mvn clean compile -pl forge-env -am

# Build and run tests
mvn clean test -pl forge-env -am

# Build the entire project
mvn clean install
```

## Dependencies

- `forge-core`: Core Forge data structures and utilities
- `forge-game`: Forge game engine
- `forge-ai`: AI player controllers
- `gson`: JSON serialization

## Future Enhancements

- Support for mulligan decisions
- Configurable starting life totals
- Support for formats other than Standard
- Ability to specify exact decks instead of random selection
- Hidden information variants in JSON output
- Support for multiplayer games
- Incremental game state updates (take actions, get new state)
- Partial information views (from one player's perspective)

## License

This code is part of the Forge project and is licensed under GPL 3.0. See the LICENSE file in the root directory for details.
