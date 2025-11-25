#!/usr/bin/env python3
"""
Example demonstrating sparse rewards for learning from final game outcomes only.

This example shows how to use the Forge Gym environment with sparse rewards,
where the agent only receives feedback (1 for win, -1 for loss) at the end
of each game, not for intermediate steps.

This is useful for:
- Monte Carlo methods
- Algorithms that work well with sparse rewards (PPO, SAC, etc.)
- Focusing on long-term strategy rather than immediate outcomes
"""

import forge_gym
import numpy as np


def sparse_reward_example():
    """Demonstrate sparse reward training."""
    print("=" * 60)
    print("Forge Gym - Sparse Reward Example")
    print("=" * 60)
    print("\nWith sparse rewards:")
    print("- Reward = 0.0 for all steps during the game")
    print("- Reward = 1.0 when you win")
    print("- Reward = -1.0 when you lose")
    print("\nThis trains the agent to learn from final outcomes only.")
    print("=" * 60)
    
    # Create environment with sparse rewards
    env = forge_gym.ForgeEnv(
        player1_is_human=True,   # RL agent controls player 1
        player2_is_human=False,  # AI controls player 2
        max_turns=30,
        render_mode="human",
        reward_mode="sparse"     # KEY: Use sparse rewards
    )
    
    try:
        print("\n🎮 Starting new game with sparse rewards...")
        observation, info = env.reset()
        
        episode_rewards = []
        step_count = 0
        
        terminated = False
        truncated = False
        
        while not terminated and not truncated:
            # Sample a random action
            num_actions = len(info.get('actions', []))
            if num_actions > 0:
                action = np.random.randint(0, num_actions)
            else:
                action = 0
            
            # Take step
            observation, reward, terminated, truncated, info = env.step(action)
            
            episode_rewards.append(reward)
            step_count += 1
            
            # Show step info
            print(f"\n🎯 Step {step_count}:")
            print(f"   Reward: {reward:.2f}")
            print(f"   Turn: {observation['turn'][0]}")
            print(f"   Player 0 life: {observation['player_0_life'][0]}")
            print(f"   Player 1 life: {observation['player_1_life'][0]}")
            
            # Only render occasionally to reduce output
            if step_count % 5 == 0:
                env.render()
            
            if terminated:
                print("\n🏁 Game ended!")
                env.render()
            elif truncated:
                print("\n⏱️  Episode truncated (max turns reached)")
        
        # Summary
        print(f"\n📈 Episode Summary:")
        print(f"   Total steps: {step_count}")
        print(f"   Total reward: {sum(episode_rewards):.2f}")
        print(f"   Non-zero rewards: {sum(1 for r in episode_rewards if r != 0)}")
        print(f"   Final reward: {episode_rewards[-1] if episode_rewards else 0:.2f}")
        
        # Analyze rewards
        print(f"\n📊 Reward Analysis:")
        print(f"   All rewards: {episode_rewards}")
        
        final_reward = sum(episode_rewards)
        if final_reward > 0:
            print(f"   ✅ Agent WON the game! (reward = {final_reward:.2f})")
        elif final_reward < 0:
            print(f"   ❌ Agent LOST the game (reward = {final_reward:.2f})")
        else:
            print(f"   ⏸️  No decisive outcome (reward = {final_reward:.2f})")
        
        print("\nNote: With sparse rewards, all intermediate steps have reward=0")
        print("Only the final outcome (win/loss) provides learning signal.")
        
    finally:
        env.close()
        print("\n✅ Environment closed")


def compare_reward_modes():
    """Compare sparse vs dense reward modes."""
    print("\n" + "=" * 60)
    print("Comparing Reward Modes")
    print("=" * 60)
    
    print("\n1️⃣  SPARSE REWARDS (reward_mode='sparse'):")
    print("   Best for: Learning overall game strategy")
    print("   - Reward = 0 during the game")
    print("   - Reward = 1 when you win")
    print("   - Reward = -1 when you lose")
    print("   - Use with: Monte Carlo, PPO, SAC, etc.")
    print("   - Pros: Focuses on winning, not intermediate tactics")
    print("   - Cons: Harder to learn (sparse signal)")
    
    print("\n2️⃣  DENSE REWARDS (reward_mode='dense'):")
    print("   Best for: Learning tactical play")
    print("   - Small rewards for opponent losing life (+0.1 per life)")
    print("   - Small penalties for losing life (-0.1 per life)")
    print("   - Large reward for winning (+10.0)")
    print("   - Large penalty for losing (-10.0)")
    print("   - Use with: DQN, A2C, PPO, etc.")
    print("   - Pros: Easier to learn (frequent feedback)")
    print("   - Cons: May focus on life totals over winning strategy")
    
    print("\n💡 Recommendation:")
    print("   Start with SPARSE for simple win/loss learning")
    print("   Switch to DENSE if training is too slow")
    print("   Or implement CUSTOM reward function (see examples)")
    print("=" * 60)


def custom_reward_example():
    """Show how to implement a custom reward function."""
    print("\n" + "=" * 60)
    print("Custom Reward Function Example")
    print("=" * 60)
    
    print("\nYou can also create your own reward function:")
    print("""
class CustomForgeEnv(forge_gym.ForgeEnv):
    def _calculate_reward(self, previous_state, current_state):
        # Option 1: Pure sparse - only final outcome
        # Check if game ended
        if not current_state or not current_state.get('players'):
            return 0.0
            
        players = current_state.get('players', [{}, {}])
        if len(players) >= 2:
            p0_life = players[0].get('life', 20)
            p1_life = players[1].get('life', 20)
            
            if p1_life <= 0:
                return 1.0  # Win
            elif p0_life <= 0:
                return -1.0  # Loss
        
        return 0.0  # Game still ongoing
        
        # Option 2: Custom shaping - reward for board presence
        # prev_creatures = len(previous_state.get('players', [{}])[0].get('battlefield', []))
        # curr_creatures = len(current_state.get('players', [{}])[0].get('battlefield', []))
        # reward = (curr_creatures - prev_creatures) * 0.5
        # 
        # # Add win/loss bonus
        # if len(current_state.get('players', [])) >= 2:
        #     if current_state['players'][1]['life'] <= 0:
        #         reward += 10.0  # Win bonus
        #     elif current_state['players'][0]['life'] <= 0:
        #         reward -= 10.0  # Loss penalty
        # 
        # return reward

# Use your custom environment
env = CustomForgeEnv(reward_mode="sparse")
""")
    print("=" * 60)


def main():
    """Main entry point."""
    print("\n" + "=" * 60)
    print("Forge Gym - Sparse Rewards Guide")
    print("=" * 60)
    print("\nThis demonstrates how to use sparse rewards where only")
    print("the final game outcome (win/loss) matters, not individual steps.")
    
    # Show comparison
    compare_reward_modes()
    
    # Show custom example
    custom_reward_example()
    
    # Ask user if they want to run the example
    try:
        user_input = input("\nPress Enter to run sparse reward example (or Ctrl+C to exit)...")
        sparse_reward_example()
    except KeyboardInterrupt:
        print("\n\nExiting...")
    
    print("\n" + "=" * 60)
    print("For more information, see GYM_README.md")
    print("=" * 60)


if __name__ == "__main__":
    main()
