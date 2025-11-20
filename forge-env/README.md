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

### Basic Usage - Game Initialization

```java
import forge.env.ForgeEnv;

public class Example {
    public static void main(String[] args) {
        // Initialize once at startup with path to forge resources
        ForgeEnv.initialize("/path/to/forge-gui/res");
        
        // Create a new game and get initial state
        String gameStateJson = ForgeEnv.newGame();
        System.out.println(gameStateJson);
    }
}
```

### Step-by-Step Game Progression

```java
import forge.env.ForgeEnv;

public class StepExample {
    public static void main(String[] args) {
        // Initialize
        ForgeEnv.initialize("/path/to/forge-gui/res");
        
        // Start a new game
        String initialState = ForgeEnv.newGame();
        
        // Game loop
        while (!ForgeEnv.isTerminal()) {
            // Get valid actions
            String validActions = ForgeEnv.getValidActions();
            System.out.println("Valid actions: " + validActions);
            
            // Execute an action (e.g., pass priority)
            String action = "{\"type\": \"pass_priority\"}";
            String newState = ForgeEnv.step(action);
            
            System.out.println("New state: " + newState);
        }
        
        // Game ended
        int winner = ForgeEnv.getWinner();
        System.out.println("Winner: Player " + winner);
    }
}
```

### Playing Cards

```java
// Get valid actions
String actionsJson = ForgeEnv.getValidActions();
// Parse to find a playable card
// Then play it:
String playAction = "{\"type\": \"play_spell\", \"cardId\": 12345}";
String newState = ForgeEnv.step(playAction);
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

### `ForgeEnv.step(String actionJson)`
Execute an action in the current game and return the new state.

**Parameters:**
- `actionJson`: JSON string describing the action, e.g., `{"type": "pass_priority"}` or `{"type": "play_spell", "cardId": 123}`

**Returns:**
- JSON string representation of the new game state

**Throws:**
- `IllegalStateException` if no active game exists

**Example actions:**
```json
{"type": "pass_priority"}
{"type": "play_spell", "cardId": 12345}
{"type": "activate_ability", "cardId": 12345}
```

### `ForgeEnv.getValidActions()`
Get all valid actions for the current game state.

**Returns:**
- JSON string with array of valid actions, including action IDs, types, card IDs/names, and descriptions

**Example response:**
```json
{
  "actions": [
    {"id": 0, "type": "play_spell", "cardId": 123, "cardName": "Forest", "description": "Play Forest"},
    {"id": 1, "type": "pass_priority", "description": "Pass priority"}
  ],
  "activePlayerId": 1,
  "phase": "Main Phase 1",
  "turn": 1
}
```

### `ForgeEnv.getState()`
Get the current game state without executing an action.

**Returns:**
- JSON string of current game state

### `ForgeEnv.isTerminal()`
Check if the current game has ended.

**Returns:**
- `true` if game is over, `false` otherwise

### `ForgeEnv.getWinner()`
Get the winner of the current game (if terminal).

**Returns:**
- Player ID (1 or 2) of winner, or -1 if no winner or game not over

### `ForgeEnv.isInitialized()`
Checks if the environment has been initialized.

**Returns:**
- `true` if initialized, `false` otherwise

### `ForgeEnv.getStandardDeckCount()`
Gets the number of Standard decks available.

**Returns:**
- Number of loaded Standard decks, or 0 if not initialized

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

## Python Integration

**For Python/RL integration**, see the comprehensive guide: [PYTHON_INTEGRATION.md](PYTHON_INTEGRATION.md)

The guide covers:
- Using JPype, Py4J, or REST API to bridge Python and Java
- Building OpenAI Gym-style interfaces
- Complete code examples for each approach

**Current V2 Scope:**
- ✅ Game initialization (random decks, initial state)
- ✅ State observation (JSON export with `getState()`)
- ✅ Action execution (`.step()` method) - **NOW IMPLEMENTED**
- ✅ Valid action enumeration (`getValidActions()`)
- ✅ Terminal state detection (`isTerminal()`)
- ✅ Winner determination (`getWinner()`)
- ❌ Reward calculation - **manual implementation needed**
- ❌ Advanced targeting/choices - **basic implementation only**

### Quick Python Example

```python
import jpype
import json

jpype.startJVM(classpath=['forge-env-jar-with-dependencies.jar'])
from forge.env import ForgeEnv

# Initialize
ForgeEnv.initialize('/path/to/forge-gui/res')

# Start game
state = json.loads(ForgeEnv.newGame())

# Game loop
while not ForgeEnv.isTerminal():
    # Get valid actions
    actions = json.loads(ForgeEnv.getValidActions())
    print(f"Turn {actions['turn']}, {actions['phase']}")
    print(f"Valid actions: {len(actions['actions'])}")
    
    # Take action (pass priority for this example)
    new_state = json.loads(ForgeEnv.step('{"type": "pass_priority"}'))
    
print(f"Game over! Winner: Player {ForgeEnv.getWinner()}")
```

For a full RL interface with reward signals and Gym compatibility, see the Python integration guide.

## Future Enhancements

### Completed in V2
- ✅ **Action execution interface**: `step(String actionJson)` to execute moves
- ✅ **Game progression**: Advance turns and phases via priority passing
- ✅ **Terminal state detection**: `isTerminal()` to detect game end
- ✅ **Valid action enumeration**: `getValidActions()` lists available moves
- ✅ **Winner determination**: `getWinner()` returns winning player

### High Priority (for Enhanced RL Support)
- **Reward calculation**: `getReward(int playerId)` for training signals
- **Advanced targeting**: Handle spell targets, combat choices, modal selections
- **Mana payment**: Automatic or interactive mana payment for spells
- **Combat system**: Full declare attackers/blockers support
- **Stack interaction**: Responding to opponent's spells

### Additional Features
- Support for mulligan decisions
- Configurable starting life totals
- Support for formats other than Standard
- Ability to specify exact decks instead of random selection
- Hidden information variants in JSON output
- Support for multiplayer games
- Partial information views (from one player's perspective)
- State-based action snapshots
- Replay/undo functionality

## License

This code is part of the Forge project and is licensed under GPL 3.0. See the LICENSE file in the root directory for details.
