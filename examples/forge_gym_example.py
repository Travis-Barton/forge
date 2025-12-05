#!/usr/bin/env python3
"""
Example script demonstrating how to use the Forge Gym environment.

This script shows:
1. How to create and initialize the environment
2. How to run a simple random agent
3. How to access observations and actions
4. How to render the game state
"""

import forge_gym
import time


def random_agent_example():
    """Run a random agent that takes random actions."""
    print("=" * 60)
    print("Forge Gym Environment - Random Agent Example")
    print("=" * 60)
    
    # Create the environment
    # By default, Player 1 is human-controlled (the RL agent)
    # and Player 2 is AI-controlled
    env = forge_gym.ForgeEnv(
        player1_is_human=True,
        player2_is_human=False,
        max_turns=20,
        render_mode="human"
    )
    
    try:
        # Reset the environment to start a new episode
        print("\n🎮 Starting new game...")
        observation, info = env.reset()
        
        print(f"\n📊 Initial observation keys: {list(observation.keys())}")
        print(f"   Turn: {observation['turn'][0]}")
        print(f"   Player 0 life: {observation['player_0_life'][0]}")
        print(f"   Player 1 life: {observation['player_1_life'][0]}")
        
        # Render initial state
        env.render()
        
        episode_reward = 0
        step_count = 0
        
        # Run the episode
        terminated = False
        truncated = False
        
        while not terminated and not truncated:
            # Get available actions
            num_actions = len(info.get('actions', []))
            print(f"\n🎯 Step {step_count + 1}: {num_actions} actions available")
            
            # Sample a random action (in a real RL scenario, this would be from your policy)
            if num_actions > 0:
                action = env.action_space.sample() % max(num_actions, 1)
            else:
                action = 0  # Pass priority
            
            # Take the action
            observation, reward, terminated, truncated, info = env.step(action)
            
            episode_reward += reward
            step_count += 1
            
            print(f"   Action taken: {action}")
            print(f"   Reward: {reward:.2f}")
            print(f"   Cumulative reward: {episode_reward:.2f}")
            print(f"   Turn: {observation['turn'][0]}")
            
            # Render the current state
            env.render()
            
            # Small delay to make it easier to follow
            time.sleep(1)
            
            if terminated:
                print("\n🏁 Game ended!")
            elif truncated:
                print("\n⏱️  Episode truncated (max turns reached)")
        
        print(f"\n📈 Episode Summary:")
        print(f"   Total steps: {step_count}")
        print(f"   Total reward: {episode_reward:.2f}")
        print(f"   Final turn: {observation['turn'][0]}")
        print(f"   Player 0 final life: {observation['player_0_life'][0]}")
        print(f"   Player 1 final life: {observation['player_1_life'][0]}")
        
    finally:
        # Clean up
        env.close()
        print("\n✅ Environment closed")


def manual_control_example():
    """Example showing manual control with specific actions."""
    print("\n" + "=" * 60)
    print("Forge Gym Environment - Manual Control Example")
    print("=" * 60)
    
    env = forge_gym.ForgeEnv(
        player1_is_human=True,
        player2_is_human=False,
        max_turns=10,
        render_mode="human"
    )
    
    try:
        observation, info = env.reset()
        env.render()
        
        # Example: Take specific actions
        actions_to_take = [
            0,  # First available action (likely a land)
            len(info.get('actions', [])) - 1,  # Pass priority (last action)
            0,  # Another action
        ]
        
        for i, action_idx in enumerate(actions_to_take):
            print(f"\n🎯 Taking action {action_idx}...")
            observation, reward, terminated, truncated, info = env.step(action_idx)
            env.render()
            
            if terminated or truncated:
                break
            
            time.sleep(2)
        
    finally:
        env.close()


def main():
    """Main entry point."""
    print("\nForge Gym Environment Examples\n")
    print("This script demonstrates how to use the Forge Gym environment")
    print("with the Gymnasium API for reinforcement learning.\n")
    
    # Check if ForgeHeadless is built
    print("Prerequisites:")
    print("1. Build the Forge JAR file:")
    print("   mvn clean install -DskipTests")
    print("\n2. Install Python dependencies:")
    print("   pip install -r requirements.txt")
    print("\n3. Optionally install forge-gym package:")
    print("   pip install -e .\n")
    
    user_input = input("Press Enter to run the random agent example (or Ctrl+C to exit)...")
    
    # Run the random agent example
    random_agent_example()
    
    # Optionally run manual control example
    user_input = input("\nPress Enter to run the manual control example (or Ctrl+C to exit)...")
    manual_control_example()
    
    print("\n" + "=" * 60)
    print("Examples completed!")
    print("=" * 60)


if __name__ == "__main__":
    main()
