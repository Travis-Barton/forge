#!/usr/bin/env python3
"""
Simple integration test for the Forge Gym environment.
This test verifies the basic functionality without complex interactions.
"""

import sys
import os
import subprocess
import time

# Add forge_gym to path if not installed
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def test_headless_directly():
    """Test that ForgeHeadless can be started directly."""
    print("Testing ForgeHeadless directly...")
    
    jar_path = "forge-gui-desktop/target/forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar"
    
    if not os.path.exists(jar_path):
        print(f"✗ JAR file not found at: {jar_path}")
        return False
    
    try:
        # Start ForgeHeadless with --help
        proc = subprocess.Popen(
            ["java", "-cp", jar_path, "forge.view.ForgeHeadless", "--help"],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        
        stdout, stderr = proc.communicate(timeout=10)
        
        if "ForgeHeadless" in stdout or "ForgeHeadless" in stderr:
            print("✓ ForgeHeadless can be started")
            return True
        else:
            print("✗ ForgeHeadless output unexpected")
            print(f"STDOUT: {stdout[:200]}")
            print(f"STDERR: {stderr[:200]}")
            return False
    except Exception as e:
        print(f"✗ Error starting ForgeHeadless: {e}")
        return False


def test_environment_import():
    """Test that the environment can be imported."""
    print("\nTesting environment import...")
    
    try:
        import forge_gym
        print("✓ forge_gym imported successfully")
        print(f"  Version: {forge_gym.__version__}")
        return True
    except Exception as e:
        print(f"✗ Failed to import forge_gym: {e}")
        return False


def test_environment_instantiation():
    """Test that the environment can be instantiated."""
    print("\nTesting environment instantiation...")
    
    try:
        import forge_gym
        
        env = forge_gym.ForgeEnv(
            player1_is_human=True,
            player2_is_human=False,
            max_turns=5,
            render_mode=None
        )
        
        print("✓ Environment instantiated successfully")
        print(f"  JAR path: {env.jar_path}")
        print(f"  Observation space: {env.observation_space}")
        print(f"  Action space: {env.action_space}")
        
        env.close()
        return True
    except Exception as e:
        print(f"✗ Failed to instantiate environment: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Run all tests."""
    print("=" * 60)
    print("Forge Gym Environment - Simple Integration Test")
    print("=" * 60)
    
    tests = [
        ("ForgeHeadless Direct", test_headless_directly),
        ("Environment Import", test_environment_import),
        ("Environment Instantiation", test_environment_instantiation),
    ]
    
    results = []
    for test_name, test_func in tests:
        try:
            success = test_func()
            results.append((test_name, success))
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
        print("\n🎉 Basic tests passed! The environment is set up correctly.")
        print("\nNote: Full integration tests with reset() and step() require")
        print("more complex subprocess I/O handling and may need adjustment")
        print("based on your specific use case.")
        return 0
    else:
        print("\n⚠️  Some tests failed. Please check the errors above.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
