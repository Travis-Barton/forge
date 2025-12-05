# Understanding Sparse vs Dense Rewards in Forge Gym

## Visual Comparison

### Sparse Rewards (Default - Recommended for Win/Loss Learning)

```
Game Episode Timeline:
┌─────────────────────────────────────────────────────────────┐
│ Step 1   Step 2   Step 3   ...   Step N-1   Step N (END)   │
│ Play     Play     Pass          Cast       Player 2         │
│ Land     Spell    Priority       Spell      dies            │
│                                                              │
│ ↓        ↓        ↓        ...   ↓          ↓               │
│ reward=0 reward=0 reward=0  ...  reward=0   reward=+1 (WIN!)│
└─────────────────────────────────────────────────────────────┘

Total Episode Reward: +1.0 (Win) or -1.0 (Loss)

✅ Advantages:
   - Focuses on the ultimate goal: winning
   - Avoids reward hacking (e.g., stalling to avoid losing life)
   - Simpler, more robust learning signal
   - Works well with PPO, SAC, Monte Carlo methods

⚠️  Consideration:
   - May take longer to learn (sparse signal)
   - Solution: Use more episodes, better exploration
```

### Dense Rewards (Optional - For Tactical Learning)

```
Game Episode Timeline:
┌─────────────────────────────────────────────────────────────┐
│ Step 1   Step 2   Step 3   ...   Step N-1   Step N (END)   │
│ Play     Spell    Opponent       Attack     Player 2        │
│ Land     damages  blocks         deals 3    dies            │
│          opp 2                   damage                     │
│ ↓        ↓        ↓        ...   ↓          ↓               │
│ reward=0 reward=  reward=  ...   reward=    reward=+10.3    │
│          +0.2     -0.5           +0.3       (Win bonus)     │
└─────────────────────────────────────────────────────────────┘

Total Episode Reward: +10.0 (varied based on game events)

✅ Advantages:
   - Faster initial learning (more feedback)
   - Encourages specific behaviors (e.g., dealing damage)
   - Can help with exploration

⚠️  Considerations:
   - May learn suboptimal strategies (focus on life, not winning)
   - More complex reward landscape
   - Risk of reward hacking
```

## Which Should You Use?

### Use SPARSE (Default) When:
- ✅ You want to learn **overall winning strategy**
- ✅ You're using modern RL algorithms (PPO, SAC, TRPO)
- ✅ You have enough episodes for training
- ✅ You want to avoid reward hacking
- ✅ **This is what you asked for: learning from entire games!**

### Use DENSE When:
- 📊 You need faster initial learning
- 📊 You want to encourage specific tactical behaviors
- 📊 You're using simpler algorithms that struggle with sparse rewards

## Code Examples

### Sparse Rewards (Your Use Case)

```python
import forge_gym

# Default: Learn from win/loss only
env = forge_gym.ForgeEnv(reward_mode="sparse")

# Training loop
for episode in range(1000):
    obs, info = env.reset()
    episode_reward = 0
    
    while True:
        action = agent.select_action(obs)  # Your policy
        obs, reward, terminated, truncated, info = env.step(action)
        
        episode_reward += reward
        # reward will be 0 during the game
        
        if terminated or truncated:
            # episode_reward is now +1 (win), -1 (loss), or 0 (truncated)
            print(f"Episode {episode}: reward = {episode_reward}")
            break
    
    # Update your agent with the episode
    # The agent learns to associate all actions in the episode
    # with the final outcome (+1 or -1)
```

### Dense Rewards (Alternative)

```python
env = forge_gym.ForgeEnv(reward_mode="dense")

# Training loop
obs, info = env.reset()
while True:
    action = agent.select_action(obs)
    obs, reward, terminated, truncated, info = env.step(action)
    
    # reward includes:
    # - Small values for life changes: -0.1 to +0.1
    # - Large values at game end: +10 (win) or -10 (loss)
    
    agent.update(obs, action, reward)  # Update after each step
    
    if terminated or truncated:
        obs, info = env.reset()
```

## Technical Details

### How Sparse Rewards Work with RL

With sparse rewards, the RL agent learns through:

1. **Episode Collection**: Play many complete games
2. **Credit Assignment**: Associate all actions in an episode with final outcome
3. **Policy Update**: Increase probability of actions from winning episodes
4. **Exploration**: Try different strategies to find what wins

Algorithms that work well with sparse rewards:
- **PPO** (Proximal Policy Optimization) - recommended
- **SAC** (Soft Actor-Critic)
- **TRPO** (Trust Region Policy Optimization)
- **Monte Carlo** methods

### Reward Comparison Table

| Aspect | Sparse | Dense |
|--------|--------|-------|
| **Reward during game** | 0 | ±0.1 per life change |
| **Reward for win** | +1 | +10 |
| **Reward for loss** | -1 | -10 |
| **Learning speed** | Slower | Faster initially |
| **Final performance** | Better strategy | May be suboptimal |
| **Recommended for** | Overall strategy | Tactical play |

## Frequently Asked Questions

### Q: Why is my agent not learning?
**A:** With sparse rewards, learning takes more episodes. Make sure:
- You're running enough episodes (1000+)
- Your exploration is sufficient
- Your neural network can handle the observation space
- You're using an algorithm designed for sparse rewards (PPO, SAC)

### Q: Can I mix sparse and dense?
**A:** Yes! Create a custom reward function that combines both approaches:

```python
class MixedRewardEnv(forge_gym.ForgeEnv):
    def _calculate_reward(self, prev, curr):
        reward = 0.0
        
        # Small sparse component: encourage progress
        if curr.get('turn', 0) > prev.get('turn', 0):
            reward += 0.01
        
        # Main sparse component: win/loss
        players = curr.get('players', [{}, {}])
        if len(players) >= 2:
            if players[1]['life'] <= 0:
                reward += 1.0  # Win
            elif players[0]['life'] <= 0:
                reward -= 1.0  # Loss
        
        return reward
```

### Q: How many episodes do I need?
**A:** With sparse rewards:
- **Minimum**: 1,000 episodes to see any learning
- **Recommended**: 10,000-100,000 episodes
- **Advanced**: 1M+ episodes for competitive play

Use parallel environments to speed up training:
```python
from stable_baselines3.common.vec_env import SubprocVecEnv

# Create 8 parallel environments
def make_env():
    return forge_gym.ForgeEnv(reward_mode="sparse")

env = SubprocVecEnv([make_env for _ in range(8)])
```

## Summary

✅ **For your use case** (learning from entire games): Use the **default sparse mode**
- Set `reward_mode="sparse"` (or just use the default)
- Train for many episodes
- Use algorithms like PPO or SAC
- Focus on long-term strategy

📚 **See Also:**
- `examples/sparse_rewards_example.py` - Working code
- `GYM_README.md` - Full API reference
- `QUICKSTART_GYM.md` - Getting started guide
