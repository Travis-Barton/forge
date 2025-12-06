# Python Integration Guide for Forge Headless Environment

This guide explains how to integrate the Forge headless environment with Python for reinforcement learning (RL) applications.

## Current State (V1)

The current `forge-env` module provides:
- Game initialization (via `ForgeEnv.initialize()`)
- Initial state retrieval (via `ForgeEnv.newGame()`)

**What's NOT included yet:**
- Action execution (`.step()` method)
- Reward calculation
- Episode termination detection
- Turn/phase progression

## Python-Java Bridge Options

To use the Java-based Forge environment from Python, you have several options:

### Option 1: JPype (Recommended)

JPype allows you to use Java classes directly in Python.

**Installation:**
```bash
pip install JPype1
```

**Python Wrapper Example:**

```python
import jpype
import jpype.imports
from jpype.types import *
import json

class ForgeRLEnvironment:
    """
    Python wrapper around Forge headless environment.
    Provides gym-like interface for RL training.
    """
    
    def __init__(self, forge_jar_path, resources_path):
        """
        Initialize the Forge environment.
        
        Args:
            forge_jar_path: Path to forge-env JAR with dependencies
            resources_path: Path to forge-gui/res directory
        """
        # Start JVM if not already started
        if not jpype.isJVMStarted():
            jpype.startJVM(
                classpath=[forge_jar_path],
                convertStrings=False
            )
        
        # Import Java class
        from forge.env import ForgeEnv
        self.ForgeEnv = ForgeEnv
        
        # Initialize Forge
        self.ForgeEnv.initialize(resources_path)
        self.current_state = None
        
    def reset(self):
        """
        Reset the environment to a new game.
        
        Returns:
            observation: Initial game state as dict
        """
        json_state = str(self.ForgeEnv.newGame())
        self.current_state = json.loads(json_state)
        return self._get_observation()
    
    def _get_observation(self):
        """
        Convert internal state to observation for RL agent.
        
        Returns:
            dict: Observation with relevant game state
        """
        return {
            'players': self.current_state['players'],
            'active_player': self.current_state['activePlayerId'],
            'phase': self.current_state['phase'],
            'turn': self.current_state['turn']
        }
    
    def get_state_json(self):
        """Get the full game state as JSON string."""
        return json.dumps(self.current_state, indent=2)
    
    def close(self):
        """Cleanup resources."""
        if jpype.isJVMStarted():
            jpype.shutdownJVM()

# Usage example
if __name__ == "__main__":
    # Initialize environment
    env = ForgeRLEnvironment(
        forge_jar_path="/path/to/forge-env-with-dependencies.jar",
        resources_path="/path/to/forge-gui/res"
    )
    
    # Reset to get initial state
    obs = env.reset()
    print(f"Initial observation: {obs}")
    
    # Get full JSON state
    print(env.get_state_json())
```

### Option 2: Py4J

Py4J creates a gateway between Python and Java processes.

**Installation:**
```bash
pip install py4j
```

**Java Gateway Server (needed in addition to ForgeEnv):**

```java
// Add this class to forge-env module
package forge.env.gateway;

import forge.env.ForgeEnv;
import py4j.GatewayServer;

public class ForgeGatewayServer {
    private final ForgeEnv env;
    
    public ForgeGatewayServer() {
        this.env = new ForgeEnv();
    }
    
    public void initialize(String resourcePath) {
        ForgeEnv.initialize(resourcePath);
    }
    
    public String newGame() {
        return ForgeEnv.newGame();
    }
    
    public boolean isInitialized() {
        return ForgeEnv.isInitialized();
    }
    
    public int getStandardDeckCount() {
        return ForgeEnv.getStandardDeckCount();
    }
    
    public static void main(String[] args) {
        ForgeGatewayServer app = new ForgeGatewayServer();
        GatewayServer server = new GatewayServer(app);
        server.start();
        System.out.println("Forge Gateway Server Started");
    }
}
```

**Python Client:**

```python
from py4j.java_gateway import JavaGateway
import json

class ForgeEnvWrapper:
    def __init__(self):
        self.gateway = JavaGateway()
        self.forge_env = self.gateway.entry_point
        
    def initialize(self, resource_path):
        self.forge_env.initialize(resource_path)
        
    def reset(self):
        json_str = self.forge_env.newGame()
        return json.loads(json_str)
        
    def close(self):
        self.gateway.close()

# Usage
env = ForgeEnvWrapper()
env.initialize("/path/to/forge-gui/res")
state = env.reset()
print(state)
```

### Option 3: REST API with Spring Boot

Create a REST API wrapper around ForgeEnv.

**Add to forge-env pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>
```

**REST Controller:**

```java
package forge.env.api;

import forge.env.ForgeEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/forge")
public class ForgeRestAPI {
    
    @PostMapping("/initialize")
    public String initialize(@RequestParam String resourcePath) {
        ForgeEnv.initialize(resourcePath);
        return "{\"status\": \"initialized\"}";
    }
    
    @PostMapping("/newGame")
    public String newGame() {
        return ForgeEnv.newGame();
    }
    
    @GetMapping("/status")
    public String getStatus() {
        return String.format(
            "{\"initialized\": %b, \"deckCount\": %d}",
            ForgeEnv.isInitialized(),
            ForgeEnv.getStandardDeckCount()
        );
    }
    
    public static void main(String[] args) {
        SpringApplication.run(ForgeRestAPI.class, args);
    }
}
```

**Python Client:**

```python
import requests
import json

class ForgeRESTClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
        
    def initialize(self, resource_path):
        response = requests.post(
            f"{self.base_url}/api/forge/initialize",
            params={"resourcePath": resource_path}
        )
        return response.json()
        
    def reset(self):
        response = requests.post(f"{self.base_url}/api/forge/newGame")
        return response.json()
        
    def get_status(self):
        response = requests.get(f"{self.base_url}/api/forge/status")
        return response.json()

# Usage
client = ForgeRESTClient()
client.initialize("/path/to/forge-gui/res")
state = client.reset()
print(state)
```

## Building a Full RL Interface (Future Enhancement)

To support full RL workflows with `.step()`, `.reset()`, etc., the following additions are needed:

### 1. Action Interface

Add methods to ForgeEnv for taking actions:

```java
// Pseudo-code for future implementation
public class ForgeEnv {
    private static Game currentGame;
    
    // Execute an action and return new state
    public static String step(String actionJson) {
        // Parse action (e.g., play card, attack, pass priority)
        // Execute action in currentGame
        // Return new game state as JSON
    }
    
    // Check if game is over
    public static boolean isTerminal() {
        return currentGame != null && currentGame.isGameOver();
    }
    
    // Get reward for current state
    public static double getReward(int playerId) {
        // Calculate reward based on game state
    }
}
```

### 2. Action Space Definition

Define the possible actions in JSON format:

```json
{
  "action_type": "play_land",
  "card_id": 12345
}

{
  "action_type": "cast_spell",
  "card_id": 12346,
  "targets": [{"type": "player", "id": 2}]
}

{
  "action_type": "attack",
  "attacker_ids": [12347, 12348],
  "defender_id": 2
}

{
  "action_type": "pass_priority"
}
```

### 3. Complete Python Gym Interface

```python
import gym
from gym import spaces
import numpy as np

class ForgeMTGEnv(gym.Env):
    """
    OpenAI Gym interface for Forge MTG.
    """
    
    def __init__(self, forge_interface):
        super().__init__()
        self.forge = forge_interface
        
        # Define action and observation spaces
        # (requires understanding of full action space)
        self.action_space = spaces.Discrete(1000)  # Placeholder
        self.observation_space = spaces.Dict({
            # Define based on game state structure
        })
        
    def reset(self):
        """Start a new game."""
        state = self.forge.reset()
        return self._process_observation(state)
        
    def step(self, action):
        """
        Execute action and return (observation, reward, done, info).
        
        Args:
            action: Action to take
            
        Returns:
            observation: New game state
            reward: Reward for the action
            done: Whether episode is finished
            info: Additional information
        """
        # Convert action to JSON
        action_json = self._action_to_json(action)
        
        # Execute action (requires step() method in ForgeEnv)
        new_state_json = self.forge.step(action_json)
        
        # Process response
        observation = self._process_observation(new_state_json)
        reward = self._calculate_reward(new_state_json)
        done = self.forge.is_terminal()
        info = {}
        
        return observation, reward, done, info
        
    def _process_observation(self, state_json):
        """Convert JSON state to numpy observation."""
        # Implementation depends on state representation choice
        pass
        
    def _action_to_json(self, action):
        """Convert action index to JSON action."""
        # Implementation depends on action space design
        pass
        
    def _calculate_reward(self, state_json):
        """Calculate reward from state."""
        # Example: life difference, card advantage, etc.
        pass
```

## Immediate Next Steps (For You)

To use the **current V1 implementation** from Python:

1. **Build the JAR with dependencies:**
   ```bash
   cd forge-env
   mvn clean package -DskipTests
   # Creates forge-env-2.0.07-SNAPSHOT.jar
   ```

2. **Create a fat JAR (includes all dependencies):**
   Add to forge-env/pom.xml:
   ```xml
   <build>
       <plugins>
           <plugin>
               <artifactId>maven-assembly-plugin</artifactId>
               <configuration>
                   <archive>
                       <manifest>
                           <mainClass>forge.env.example.ForgeEnvExample</mainClass>
                       </manifest>
                   </archive>
                   <descriptorRefs>
                       <descriptorRef>jar-with-dependencies</descriptorRef>
                   </descriptorRefs>
               </configuration>
           </plugin>
       </plugins>
   </build>
   ```
   
   Build with:
   ```bash
   mvn clean compile assembly:single
   ```

3. **Use JPype wrapper** (from Option 1 above)

4. **For full RL support**, you'll need to extend ForgeEnv with:
   - `step(String actionJson)` method
   - `isTerminal()` method
   - `getReward(int playerId)` method
   - Action parsing and execution logic

## Example: Current Capabilities in Python

Here's what you can do **right now** with the current implementation:

```python
import jpype
import json

# Start JVM with forge-env JAR
jpype.startJVM(classpath=['forge-env-jar-with-dependencies.jar'])

# Import ForgeEnv
from forge.env import ForgeEnv

# Initialize
ForgeEnv.initialize('/path/to/forge-gui/res')

# Create games
for i in range(5):
    json_state = str(ForgeEnv.newGame())
    state = json.loads(json_state)
    
    print(f"Game {i+1}:")
    print(f"  Player 1 deck: {state['players'][0]['deckName']}")
    print(f"  Player 2 deck: {state['players'][1]['deckName']}")
    print(f"  Starting player: {state['activePlayerId']}")
    print()

jpype.shutdownJVM()
```

This gives you the ability to:
- Generate random starting positions
- Access complete game state as structured data
- Create training datasets from initial positions

For a **full RL loop**, you'll need the action execution capabilities described above.
