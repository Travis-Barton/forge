# Forge Gym Environment

A [Gymnasium](https://gymnasium.farama.org/)-compatible reinforcement learning environment for the Forge Magic: The Gathering game engine.

## Overview

This package provides a Python interface to connect the Forge headless MTG game engine to reinforcement learning frameworks like Stable-Baselines3, RLlib, or any other library that supports the Gymnasium API (formerly OpenAI Gym).

## Features

- **Standard Gymnasium API**: Compatible with `reset()`, `step()`, `render()`, and `close()` methods
- **Flexible Player Configuration**: Support for human-controlled, AI-controlled, or mixed players
- **Rich Observations**: Access to game state including life totals, hand sizes, battlefield, and more
- **Action Space**: Dynamic action space based on available game actions (play lands, cast spells, activate abilities)
- **Customizable Rewards**: Built-in reward shaping based on life totals and game outcomes

## Installation

### Prerequisites

1. **Java 17 or later** - Required to run the Forge game engine
2. **Python 3.8 or later**
3. **Built Forge JAR file** - Must build the project first

### Step 1: Build Forge

```bash
# From the forge repository root
mvn clean install -DskipTests
```

This will create the JAR file at:
```
forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar
```

### Step 2: Install Python Dependencies

```bash
# Install required packages
pip install -r requirements.txt

# Or install the forge-gym package in development mode
pip install -e .
```

## Quick Start

### Basic Usage

```python
import forge_gym

# Create the environment
env = forge_gym.ForgeEnv(
    player1_is_human=True,   # Player 1 controlled by RL agent
    player2_is_human=False,  # Player 2 controlled by AI
    max_turns=50,
    render_mode="human"
)

# Reset to start a new episode
observation, info = env.reset()

# Run a simple loop
terminated = False
truncated = False
total_reward = 0

while not terminated and not truncated:
    # Sample a random action (replace with your RL policy)
    action = env.action_space.sample()
    
    # Take a step
    observation, reward, terminated, truncated, info = env.step(action)
    total_reward += reward
    
    # Optionally render the state
    env.render()

# Clean up
env.close()
print(f"Episode finished with total reward: {total_reward}")
```

### Example with Stable-Baselines3

```python
import forge_gym
from stable_baselines3 import PPO
from stable_baselines3.common.env_checker import check_env

# Create and check the environment
env = forge_gym.ForgeEnv(max_turns=30)
check_env(env)

# Train an agent
model = PPO("MultiInputPolicy", env, verbose=1)
model.learn(total_timesteps=10000)

# Test the trained agent
obs, info = env.reset()
for i in range(100):
    action, _states = model.predict(obs, deterministic=True)
    obs, reward, terminated, truncated, info = env.step(action)
    if terminated or truncated:
        obs, info = env.reset()

env.close()
```

## API Reference

### ForgeEnv

The main environment class.

#### Constructor Parameters

- `jar_path` (str, optional): Path to the Forge JAR file. Auto-detected if not provided.
- `java_home` (str, optional): Path to Java installation. Uses system Java if not provided.
- `player1_is_human` (bool, default=True): Whether player 1 is controlled by the RL agent (True) or AI (False)
- `player2_is_human` (bool, default=False): Whether player 2 is controlled by the RL agent (True) or AI (False)
- `max_turns` (int, default=100): Maximum number of turns before episode truncates
- `render_mode` (str, optional): Rendering mode ("human" or "ansi")

#### Methods

##### `reset(seed=None, options=None) -> (observation, info)`

Reset the environment and start a new game.

**Returns:**
- `observation`: Dict containing game state observations
- `info`: Dict with additional information (full state, available actions)

##### `step(action) -> (observation, reward, terminated, truncated, info)`

Execute an action in the environment.

**Parameters:**
- `action` (int): Index of the action to take

**Returns:**
- `observation`: New observation after the action
- `reward`: Reward for the action
- `terminated`: Whether the game ended (player died)
- `truncated`: Whether the episode was cut short (max turns)
- `info`: Additional information

##### `render()`

Display the current game state (if render_mode is set).

##### `close()`

Clean up and terminate the Forge process.

### Observation Space

The observation is a dictionary containing:

```python
{
    "turn": np.array([turn_number], dtype=np.int32),
    "phase": int,  # 0-9 representing game phases
    "active_player": int,  # 0 or 1
    "stack_size": np.array([stack_size], dtype=np.int32),
    "player_0_life": np.array([life], dtype=np.int32),
    "player_1_life": np.array([life], dtype=np.int32),
    "player_0_hand_size": np.array([hand_size], dtype=np.int32),
    "player_1_hand_size": np.array([hand_size], dtype=np.int32),
}
```

**Note**: The `info` dictionary returned by `reset()` and `step()` contains the full game state JSON and list of available actions for more advanced use cases.

### Action Space

- **Type**: `Discrete(200)`
- **Description**: Actions are indexed from 0 to 199
  - Indices 0 to N-1 correspond to specific available actions (play land, cast spell, activate ability)
  - Indices >= N (where N is the number of available actions) are treated as "pass priority"
- **Dynamic Actions**: The actual available actions change each step. Use `info['actions']` to see what each index represents.

## Advanced Usage

### Custom Reward Function

You can subclass `ForgeEnv` to implement your own reward function:

```python
import forge_gym

class CustomForgeEnv(forge_gym.ForgeEnv):
    def _calculate_reward(self, previous_state, current_state):
        # Your custom reward logic
        reward = 0.0
        
        # Example: Reward for playing creatures
        prev_battlefield = len(previous_state.get('players', [{}])[0].get('battlefield', []))
        curr_battlefield = len(current_state.get('players', [{}])[0].get('battlefield', []))
        
        if curr_battlefield > prev_battlefield:
            reward += 1.0
        
        # Call parent for standard rewards
        reward += super()._calculate_reward(previous_state, current_state)
        
        return reward

# Use your custom environment
env = CustomForgeEnv()
```

### Accessing Full Game State

The `info` dictionary provides complete access to the game state:

```python
observation, info = env.reset()

# Full game state as JSON
state = info['state']
print(f"Turn: {state['turn']}")
print(f"Phase: {state['phase']}")

# See all players
for player in state['players']:
    print(f"{player['name']}: {player['life']} life")
    print(f"  Hand: {[card['name'] for card in player['hand']]}")
    print(f"  Battlefield: {[card['name'] for card in player['battlefield']]}")

# Available actions
for i, action in enumerate(info['actions']):
    print(f"Action {i}: {action['type']} - {action.get('card_name', '')}")
```

### Multi-Agent Setup

To train both players:

```python
env = forge_gym.ForgeEnv(
    player1_is_human=True,  # Controlled by RL agent
    player2_is_human=True,  # Also controlled by RL agent
    max_turns=30
)

# You'll need to implement turn-taking logic in your training loop
# based on the 'activePlayerId' in the observation
```

## Examples

See the `examples/` directory for complete examples:

- `forge_gym_example.py`: Random agent and manual control examples
- Run with: `python examples/forge_gym_example.py`

## Architecture

```
┌─────────────────┐
│   Python RL     │
│   Agent/Policy  │
└────────┬────────┘
         │ Gymnasium API
         │ (reset, step, etc.)
┌────────▼────────┐
│   ForgeEnv      │
│  (forge_gym)    │
└────────┬────────┘
         │ subprocess + stdin/stdout
         │ JSON communication
┌────────▼────────┐
│  ForgeHeadless  │
│  (Java Process) │
└────────┬────────┘
         │
┌────────▼────────┐
│  Forge Game     │
│     Engine      │
└─────────────────┘
```

## Troubleshooting

### JAR file not found

If you get an error about the JAR file not being found:

```python
env = forge_gym.ForgeEnv(
    jar_path="/absolute/path/to/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar"
)
```

### Java not found

Set the `JAVA_HOME` environment variable or pass it to the constructor:

```python
env = forge_gym.ForgeEnv(
    java_home="/path/to/jdk-17"
)
```

### Process hangs or doesn't respond

Try increasing the sleep times in the environment or reducing the game speed. The environment includes built-in delays for process communication.

## Limitations

1. **Performance**: Communication via subprocess I/O has overhead. For high-throughput training, consider implementing a native Java binding.
2. **Observation Space**: The current observation space is simplified. Full card information is available in `info['state']` but not in the main observation.
3. **Action Masking**: Invalid actions are filtered by the game engine, but action masking isn't directly exposed to the RL algorithm.

## Contributing

Contributions are welcome! Please see the main [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

GNU General Public License v3.0 - see [LICENSE](../LICENSE) for details.

## Acknowledgments

- Built on top of the [Forge](https://github.com/Card-Forge/forge) MTG game engine
- Compatible with [Gymnasium](https://gymnasium.farama.org/) (successor to OpenAI Gym)
