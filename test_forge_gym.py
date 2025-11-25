#!/usr/bin/env python3
"""
Simple test script to verify the Forge Gym environment is working.
This can be run before diving into full RL training.
"""

import sys
import os

# Add forge_gym to path if not installed
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_import():
    """Test that forge_gym can be imported."""
    print("Testing import...")
    try:
        import forge_gym
        print("✓ forge_gym imported successfully")
        return True
    except ImportError as e:
        print(f"✗ Failed to import forge_gym: {e}")
        return False


def test_environment_creation():
    """Test that the environment can be created."""
    print("\nTesting environment creation...")
    try:
        import forge_gym
        env = forge_gym.ForgeEnv(
            player1_is_human=True,
            player2_is_human=False,
            max_turns=5,
            render_mode=None
        )
        print("✓ Environment created successfully")
        env.close()
        return True
    except Exception as e:
        print(f"✗ Failed to create environment: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_jar_exists():
    """Test that the JAR file exists."""
    print("\nTesting JAR file...")
    try:
        import forge_gym
        env = forge_gym.ForgeEnv()
        jar_path = env.jar_path
        env.close()
        
        if os.path.exists(jar_path):
            print(f"✓ JAR file found at: {jar_path}")
            return True
        else:
            print(f"✗ JAR file not found at: {jar_path}")
            print("  Please build the project first: mvn clean install -DskipTests")
            return False
    except Exception as e:
        print(f"✗ Error checking JAR: {e}")
        return False


def test_reset():
    """Test that the environment can be reset."""
    print("\nTesting environment reset...")
    try:
        import forge_gym
        env = forge_gym.ForgeEnv(
            player1_is_human=True,
            player2_is_human=False,
            max_turns=5,
            render_mode=None
        )
        
        observation, info = env.reset()
        
        # Check observation structure
        expected_keys = ['turn', 'phase', 'active_player', 'stack_size', 
                        'player_0_life', 'player_1_life', 
                        'player_0_hand_size', 'player_1_hand_size']
        
        for key in expected_keys:
            if key not in observation:
                print(f"✗ Missing key in observation: {key}")
                env.close()
                return False
        
        print("✓ Environment reset successfully")
        print(f"  - Observation keys: {list(observation.keys())}")
        print(f"  - Turn: {observation['turn'][0]}")
        print(f"  - Player 0 life: {observation['player_0_life'][0]}")
        print(f"  - Player 1 life: {observation['player_1_life'][0]}")
        
        env.close()
        return True
    except Exception as e:
        print(f"✗ Failed to reset environment: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_step():
    """Test that the environment can execute a step."""
    print("\nTesting environment step...")
    try:
        import forge_gym
        env = forge_gym.ForgeEnv(
            player1_is_human=True,
            player2_is_human=False,
            max_turns=5,
            render_mode=None
        )
        
        observation, info = env.reset()
        
        # Take a few steps
        for i in range(3):
            action = 0  # Try first action
            obs, reward, terminated, truncated, info = env.step(action)
            
            if terminated or truncated:
                print(f"  - Episode ended at step {i+1}")
                break
        
        print("✓ Environment step executed successfully")
        print(f"  - Final turn: {obs['turn'][0]}")
        print(f"  - Terminated: {terminated}, Truncated: {truncated}")
        
        env.close()
        return True
    except Exception as e:
        print(f"✗ Failed to step environment: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_gymnasium_compatibility():
    """Test compatibility with gymnasium API checker."""
    print("\nTesting Gymnasium API compatibility...")
    try:
        import gymnasium as gym
        from gymnasium.utils.env_checker import check_env
        import forge_gym
        
        env = forge_gym.ForgeEnv(
            player1_is_human=True,
            player2_is_human=False,
            max_turns=5,
            render_mode=None
        )
        
        # This will raise an error if the environment doesn't comply
        check_env(env.unwrapped, skip_render_check=True)
        
        print("✓ Environment passes Gymnasium API checks")
        env.close()
        return True
    except ImportError:
        print("⚠ Gymnasium not installed, skipping API check")
        print("  Install with: pip install gymnasium")
        return True  # Don't fail if gymnasium isn't installed
    except Exception as e:
        print(f"✗ Environment failed Gymnasium API check: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Run all tests."""
    print("=" * 60)
    print("Forge Gym Environment - Test Suite")
    print("=" * 60)
    
    tests = [
        ("Import", test_import),
        ("JAR exists", test_jar_exists),
        ("Environment creation", test_environment_creation),
        ("Environment reset", test_reset),
        ("Environment step", test_step),
        ("Gymnasium compatibility", test_gymnasium_compatibility),
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            success = test_func()
            results.append((test_name, success))
        except KeyboardInterrupt:
            print("\n\nTests interrupted by user")
            sys.exit(1)
        except Exception as e:
            print(f"\n✗ Unexpected error in {test_name}: {e}")
            results.append((test_name, False))
    
    # Summary
    print("\n" + "=" * 60)
    print("Test Summary")
    print("=" * 60)
    
    passed = sum(1 for _, success in results if success)
    total = len(results)
    
    for test_name, success in results:
        status = "✓ PASS" if success else "✗ FAIL"
        print(f"{status}: {test_name}")
    
    print(f"\nTotal: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n🎉 All tests passed! The environment is ready to use.")
        return 0
    else:
        print("\n⚠️  Some tests failed. Please check the errors above.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
