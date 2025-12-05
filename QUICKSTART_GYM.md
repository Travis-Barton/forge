# Forge Gym Environment - Quick Start Guide

## Overview

This guide will help you get started with the Forge Gym environment for reinforcement learning.

## Prerequisites

1. **Java 17 or later** installed
2. **Python 3.8 or later** installed
3. **Forge JAR built** (see Building section below)

## Building Forge

Before using the Gym environment, you need to build the Forge JAR file:

```bash
# From the forge repository root
mvn clean install -DskipTests

# This creates:
# forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar
```

## Installation

### Option 1: Install as Package (Recommended)

```bash
# From the forge repository root
pip install -e .
```

This installs the `forge-gym` package in editable mode.

### Option 2: Install Dependencies Only

```bash
pip install -r requirements.txt
```

Then add the forge directory to your Python path in your scripts:

```python
import sys
sys.path.insert(0, '/path/to/forge')
import forge_gym
```

## Quick Test

Run the simple integration test to verify everything is working:

```bash
python3 test_forge_gym_simple.py
```

Expected output:
```
============================================================
Forge Gym Environment - Simple Integration Test
============================================================
Testing ForgeHeadless directly...
✓ ForgeHeadless can be started

Testing environment import...
✓ forge_gym imported successfully
  Version: 0.1.0

Testing environment instantiation...
✓ Environment instantiated successfully
...
```

## Basic Usage

### Understanding Reward Modes

**IMPORTANT:** The environment supports two reward modes:

1. **Sparse Rewards (Default - Recommended for your use case):**
   ```python
   env = forge_gym.ForgeEnv(reward_mode="sparse")
   ```
   - Reward = 0 for all steps during the game
   - Reward = +1 when you WIN the game
   - Reward = -1 when you LOSE the game
   - **This is what you want for learning from entire games!**

2. **Dense Rewards:**
   ```python
   env = forge_gym.ForgeEnv(reward_mode="dense")
   ```
   - Small rewards/penalties for life changes each step
   - Large reward/penalty for win/loss
   - Better for faster initial learning but may focus on wrong objectives

### Example 1: Sparse Rewards (Win/Loss Only)

```python
import forge_gym

# Create environment with SPARSE rewards (default)
# This is for learning from the entire game outcome
env = forge_gym.ForgeEnv(
    player1_is_human=True,   # RL agent controls player 1
    player2_is_human=False,  # AI controls player 2
    max_turns=20,
    reward_mode="sparse"     # Only win/loss matters!
)

# Reset and run
observation, info = env.reset()
episode_reward = 0

for _ in range(100):
    # Sample random action (or use your trained policy)
    action = env.action_space.sample()
    
    # Take step
    obs, reward, terminated, truncated, info = env.step(action)
    episode_reward += reward
    
    # During the game, reward will be 0
    # At the end: +1 for win, -1 for loss
    
    if terminated or truncated:
        print(f"Game ended! Final reward: {episode_reward}")
        # episode_reward will be 1, -1, or 0
        observation, info = env.reset()
        episode_reward = 0

env.close()
```

### Example 2: Random Agent

```python
import forge_gym

# Create environment (sparse rewards by default)
env = forge_gym.ForgeEnv(
    player1_is_human=True,   # RL agent controls player 1
    player2_is_human=False,  # AI controls player 2
    max_turns=20,
    render_mode="human",
    reward_mode="sparse"     # Learn from win/loss only
)

# Reset and run
observation, info = env.reset()

for _ in range(100):
    # Sample random action
    action = env.action_space.sample()
    
    # Take step
    obs, reward, terminated, truncated, info = env.step(action)
    
    # Optionally render
    env.render()
    
    if terminated or truncated:
        observation, info = env.reset()

env.close()
```

### Example 3: Using with Stable-Baselines3

First install stable-baselines3:

```bash
pip install stable-baselines3
```

Then:

```python
import forge_gym
from stable_baselines3 import PPO
from stable_baselines3.common.env_checker import check_env

# Create environment with sparse rewards (default)
env = forge_gym.ForgeEnv(max_turns=30, reward_mode="sparse")

# Verify environment follows Gym API
check_env(env)

# Train a PPO agent
model = PPO("MultiInputPolicy", env, verbose=1)
model.learn(total_timesteps=10000)

# Save the model
model.save("forge_ppo_model")

# Test the trained agent
obs, info = env.reset()
for i in range(100):
    action, _states = model.predict(obs, deterministic=True)
    obs, reward, terminated, truncated, info = env.step(action)
    
    if terminated or truncated:
        obs, info = env.reset()

env.close()
```

## Environment API

### Constructor Parameters

```python
ForgeEnv(
    jar_path=None,              # Path to JAR (auto-detected if None)
    java_home=None,             # Java installation path
    player1_is_human=True,      # Player 1 controlled by RL agent
    player2_is_human=False,     # Player 2 controlled by AI
    max_turns=100,              # Max turns before episode ends
    render_mode=None            # "human" or "ansi" or None
)
```

### Observation Space

Dictionary with the following keys:
- `turn`: Current turn number (int32 array)
- `phase`: Current phase (0-9 discrete)
- `active_player`: Active player ID (0 or 1)
- `stack_size`: Number of items on stack (int32 array)
- `player_0_life`: Player 0's life total (int32 array)
- `player_1_life`: Player 1's life total (int32 array)
- `player_0_hand_size`: Number of cards in Player 0's hand (int32 array)
- `player_1_hand_size`: Number of cards in Player 1's hand (int32 array)

### Action Space

Discrete(200) - Actions are indexed:
- 0 to N-1: Specific available actions (play land, cast spell, etc.)
- N to 199: Default to "pass priority"

The actual available actions can be found in `info['actions']` after `reset()` or `step()`.

### Info Dictionary

Both `reset()` and `step()` return an `info` dictionary containing:
- `state`: Full game state as JSON
- `actions`: List of available actions with details

## Accessing Full Game State

```python
observation, info = env.reset()

# Full game state
state = info['state']
print(f"Turn: {state['turn']}")
print(f"Phase: {state['phase']}")

# Player information
for player in state['players']:
    print(f"{player['name']}: {player['life']} life")
    print(f"  Hand: {[card['name'] for card in player['hand']]}")
    print(f"  Battlefield: {[card['name'] for card in player['battlefield']]}")

# Available actions
for i, action in enumerate(info['actions']):
    print(f"Action {i}: {action['type']} - {action.get('card_name', '')}")
```

## Known Limitations

1. **Subprocess I/O**: The current implementation uses subprocess stdin/stdout for communication, which may have latency issues for high-throughput training. For production use, consider implementing a native Java binding or REST API.

2. **Observation Space**: The observation space is simplified. Full card information is available in `info['state']` but not included in the main observation to keep it manageable.

3. **Action Masking**: While invalid actions are filtered by the game engine, explicit action masking isn't directly exposed to RL algorithms. You may want to implement this based on `info['actions']`.

4. **Performance**: Starting a new Java process for each episode has overhead. For better performance, consider keeping the process alive across episodes (future enhancement).

## Troubleshooting

### JAR file not found

Ensure you've built the project:
```bash
mvn clean install -DskipTests
```

Or specify the JAR path explicitly:
```python
env = forge_gym.ForgeEnv(jar_path="/absolute/path/to/jar/file.jar")
```

### Java not found

Ensure Java 17+ is installed and accessible:
```bash
java --version
```

Or set JAVA_HOME:
```python
env = forge_gym.ForgeEnv(java_home="/path/to/jdk-17")
```

### Environment hangs during reset() or step()

This may be due to subprocess communication issues. Try:
1. Reducing `max_turns` to see if it completes faster
2. Check stderr output for Java errors
3. Verify ForgeHeadless works standalone: `./forge-headless --help`

## Advanced Usage

### Custom Reward Function

```python
import forge_gym

class CustomForgeEnv(forge_gym.ForgeEnv):
    def _calculate_reward(self, previous_state, current_state):
        # Your custom logic
        reward = 0.0
        
        # Example: Reward for playing more creatures
        if current_state:
            prev_creatures = len(previous_state.get('players', [{}])[0].get('battlefield', []))
            curr_creatures = len(current_state.get('players', [{}])[0].get('battlefield', []))
            reward += (curr_creatures - prev_creatures) * 0.5
        
        # Add base rewards
        reward += super()._calculate_reward(previous_state, current_state)
        return reward

env = CustomForgeEnv()
```

### Multi-Agent Training

For training both players:

```python
env = forge_gym.ForgeEnv(
    player1_is_human=True,
    player2_is_human=True,  # Both controlled by RL
    max_turns=30
)

# Implement turn-taking logic based on info['state']['activePlayerId']
```

## Examples

See the `examples/` directory for complete working examples:

- `examples/forge_gym_example.py`: Random agent and manual control demos

Run with:
```bash
python3 examples/forge_gym_example.py
```

## Next Steps

1. Train a simple RL agent using PPO or DQN
2. Experiment with different reward functions
3. Implement curriculum learning (start with simpler decks)
4. Try multi-agent self-play
5. Visualize learning progress

For more details, see [GYM_README.md](GYM_README.md).

## Support

If you encounter issues or have questions:
- Check the troubleshooting section above
- Review the full documentation in [GYM_README.md](GYM_README.md)
- Join the Forge Discord community
- Open an issue on GitHub
