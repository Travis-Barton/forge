#!/usr/bin/env python3
"""
Simple Agent Loop for ForgeHeadlessServer

Connects the Forge game server (port 8080) to a policy function (port 5005).
The policy receives the full game state JSON and returns an action index.

Usage:
  python agent_loop.py                    # Normal mode with policy
  python agent_loop.py --local-debug      # Interactive manual mode
  python agent_loop.py puzzle.pzl         # Load puzzle file
"""

import urllib.request
import urllib.error
import json
import time
import sys
import socket
import argparse

# Configuration
FORGE_URL = "http://localhost:8080"
POLICY_URL = "http://localhost:5005"
LOCAL_DEBUG_MODE = False

def forge_request(endpoint, data=None):
    """Make a request to ForgeHeadlessServer."""
    url = f"{FORGE_URL}{endpoint}"
    req = urllib.request.Request(url)
    
    if data is not None:
        if isinstance(data, dict):
            req.data = json.dumps(data).encode('utf-8')
            req.add_header('Content-Type', 'application/json')
        else:
            req.data = data.encode('utf-8')
    
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.load(resp)
    except urllib.error.URLError as e:
        print(f"Forge request failed: {e}")
        return None

def policy_request(game_state):
    """Send game state to policy and get action index."""
    
    # Parse hand - handle both string and object formats
    raw_hand = game_state.get("hand", [])
    hand = []
    for card in raw_hand:
        if isinstance(card, str):
            hand.append(card)
        elif isinstance(card, dict):
            hand.append(card.get("name", str(card)))
        else:
            hand.append(str(card))
    
    # Transform to cleaner format for LLM consumption
    structured_payload = {
        "gameState": {
            "game_id": game_state.get("game_id", ""),
            "turn": game_state.get("turn", 0),
            "phase": game_state.get("phase", "UNKNOWN"),
            "game_over": game_state.get("game_over", False),
            "hand": hand,
            "library_count": game_state.get("library_count", 0),
            "battlefield": game_state.get("battlefield", {}),
            "player1": game_state.get("player1", {}),
            "player2": game_state.get("player2", {}),
            "combat": game_state.get("combat", {})
        },
        "actionState": {
            "possible_actions": [
                {
                    "index": i,
                    "type": action.get("type", "unknown"),
                    "card": action.get("card_name", ""),
                    "mana_cost": action.get("mana_cost", ""),
                    "requires_targets": action.get("requires_targets", False)
                }
                for i, action in enumerate(game_state.get("possible_actions", {}).get("actions", []))
            ]
        }
    }
    
    req = urllib.request.Request(POLICY_URL)
    payload = json.dumps(structured_payload).encode('utf-8')
    req.data = payload
    req.add_header('Content-Type', 'application/json')
    
    # Debug: show what we're sending
    actions = structured_payload['actionState']['possible_actions']
    print(f"  [DEBUG] Sending to policy: {len(payload)} bytes")
    print(f"  [DEBUG] Actions available ({len(actions)}):")
    for act in actions:
        desc = f"{act['type']}"
        if act.get('card'):
            desc += f": {act['card']}"
        print(f"    [{act['index']}] {desc}")
    
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            result = json.load(resp)
            print(f"  [DEBUG] Policy response: {result}")
            # Handle various response formats
            if isinstance(result, dict):
                idx = result.get('action_index', result.get('index', 0))
                if idx is None:
                    print("\033[91m" + "=" * 60)
                    print("⚠️  FALLBACK: Policy returned None, defaulting to 0")
                    print("=" * 60 + "\033[0m")
                    return 0
                return idx
            elif isinstance(result, int):
                return result
            else:
                return int(result)
    except urllib.error.URLError as e:
        print("\033[91m" + "=" * 60)
        print(f"⚠️  FALLBACK: Policy request failed: {e}")
        print("    Defaulting to action index 0!")
        print("=" * 60 + "\033[0m")
        return 0  # Default to first action
    except socket.timeout:
        print("\033[91m" + "=" * 60)
        print("⚠️  FALLBACK: Policy request timed out!")
        print("    Defaulting to action index 0!")
        print("=" * 60 + "\033[0m")
        return 0
    except (ValueError, TypeError) as e:
        print("\033[91m" + "=" * 60)
        print(f"⚠️  FALLBACK: Failed to parse policy response: {e}")
        print("    Defaulting to action index 0!")
        print("=" * 60 + "\033[0m")
        return 0

def local_debug_policy(game_state):
    """Interactive manual policy - YOU are the policy!"""
    
    # Build structured payload same as normal
    raw_hand = game_state.get("hand", [])
    hand = []
    for card in raw_hand:
        if isinstance(card, str):
            hand.append(card)
        elif isinstance(card, dict):
            hand.append(card.get("name", str(card)))
        else:
            hand.append(str(card))
    
    structured_payload = {
        "gameState": {
            "game_id": game_state.get("game_id", ""),
            "turn": game_state.get("turn", 0),
            "phase": game_state.get("phase", "UNKNOWN"),
            "active_player": game_state.get("active_player", ""),
            "priority_player": game_state.get("priority_player", ""),
            "game_over": game_state.get("game_over", False),
            "hand": game_state.get("hand", []),  # Full hand objects
            "library_count": game_state.get("library_count", 0),
            "battlefield": game_state.get("battlefield", {}),
            "player1": game_state.get("player1", {}),
            "player2": game_state.get("player2", {}),
            "mana_pool": game_state.get("mana_pool", {}),
            "stack": game_state.get("stack", []),
            "combat": game_state.get("combat", {}),
            "can_play_land": game_state.get("can_play_land", True),
            "player1_exile": game_state.get("player1_exile", []),
            "player2_exile": game_state.get("player2_exile", [])
        },
        "actionState": {
            "possible_actions": [
                {
                    "index": i,
                    "type": action.get("type", "unknown"),
                    "card": action.get("card_name", ""),
                    "description": action.get("description", ""),
                    "mana_cost": action.get("mana_cost", ""),
                    "is_instant": action.get("is_instant", False),
                    "requires_targets": action.get("requires_targets", False)
                }
                for i, action in enumerate(game_state.get("possible_actions", {}).get("actions", []))
            ]
        }
    }
    
    print("\n" + "=" * 80)
    print("🎮 LOCAL DEBUG MODE - Full JSON Payload:")
    print("=" * 80)
    print(json.dumps(structured_payload, indent=2))
    print("=" * 80)
    
    actions = structured_payload['actionState']['possible_actions']
    print(f"\n📋 Available Actions ({len(actions)}):")
    for act in actions:
        desc = f"{act['type']}"
        if act.get('card'):
            desc += f": {act['card']}"
        if act.get('mana_cost'):
            desc += f" ({act['mana_cost']})"
        if act.get('is_instant'):
            desc += " [instant]"
        print(f"  [{act['index']}] {desc}")
    
    print()
    while True:
        try:
            user_input = input("Enter action index (or 'q' to quit, 'j' for raw JSON): ").strip()
            if user_input.lower() == 'q':
                print("Quitting...")
                sys.exit(0)
            elif user_input.lower() == 'j':
                print(json.dumps(game_state, indent=2))
                continue
            idx = int(user_input)
            if 0 <= idx < len(actions):
                return idx
            print(f"Invalid index. Must be 0-{len(actions)-1}")
        except ValueError:
            print("Please enter a number")
        except EOFError:
            print("\nEOF - quitting")
            sys.exit(0)

def run_game(scenario=None):
    """Run a single game using the policy."""
    print("=" * 60)
    print("Starting new game...")
    
    # Reset/start game
    reset_options = scenario if scenario else {}
    state = forge_request("/api/reset", reset_options)
    
    if not state:
        print("Failed to start game!")
        return None
    
    turn = 0
    step = 0
    max_steps = 500  # Safety limit
    game_log = []  # Collect game log entries
    
    while not state.get("game_over") and step < max_steps:
        step += 1
        current_turn = state.get("turn", 0)
        phase = state.get("phase", "?")
        
        if current_turn != turn:
            turn = current_turn
            print(f"\n--- Turn {turn} ---")
            game_log.append(f"\n=== Turn {turn} ===")
        
        actions = state.get("possible_actions", {}).get("actions", [])
        action_count = len(actions)
        
        if action_count == 0:
            print("No actions available!")
            game_log.append("No actions available!")
            break
        
        # Get action from policy
        if LOCAL_DEBUG_MODE:
            action_index = local_debug_policy(state)
        else:
            action_index = policy_request(state)
        
        # Validate action index
        if action_index < 0 or action_index >= action_count:
            print(f"Invalid action index {action_index}, using 0")
            action_index = 0
        
        # Log the action
        chosen_action = actions[action_index]
        action_type = chosen_action.get("type", "unknown")
        card_name = chosen_action.get("card_name", "")
        
        if action_type == "pass_priority":
            log_entry = f"[{phase}] Pass priority"
        else:
            log_entry = f"[{phase}] {action_type}: {card_name}"
        
        print(f"  {log_entry}")
        game_log.append(log_entry)
        
        # Execute the action
        state = forge_request("/api/step", f"play_action {action_index}")
        
        if not state:
            print("Failed to get state after action!")
            game_log.append("ERROR: Failed to get state after action!")
            break
    
    if step >= max_steps:
        print(f"\nGame stopped after {max_steps} steps (safety limit)")
    
    # Game over
    if state and state.get("game_over"):
        print("\n" + "=" * 60)
        print("GAME OVER")
        
        # Show winner
        winner = state.get("winner", "Unknown")
        print(f"\nWinner: {winner}")
        
        # Show final life totals
        p1 = state.get("player1", {})
        p2 = state.get("player2", {})
        p1_name = p1.get("name", "Player 1")
        p2_name = p2.get("name", "Player 2")
        p1_life = p1.get("life", "?")
        p2_life = p2.get("life", "?")
        print(f"{p1_name}: {p1_life} life")
        print(f"{p2_name}: {p2_life} life")
    
    # Save game log
    if game_log:
        log_file = f"game_log_{int(time.time())}.txt"
        with open(log_file, 'w') as f:
            f.write("=" * 60 + "\n")
            f.write("FORGE HEADLESS GAME LOG\n")
            f.write("=" * 60 + "\n\n")
            for entry in game_log:
                f.write(entry + "\n")
        print(f"\nGame log saved to: {log_file}")
    
    return state

def main():
    global LOCAL_DEBUG_MODE
    
    parser = argparse.ArgumentParser(description='Agent Loop for ForgeHeadlessServer')
    parser.add_argument('--local-debug', action='store_true', 
                        help='Interactive mode - YOU are the policy')
    parser.add_argument('puzzle', nargs='?', default=None,
                        help='Path to puzzle file (.pzl)')
    
    args = parser.parse_args()
    
    LOCAL_DEBUG_MODE = args.local_debug
    
    scenario = None
    if args.puzzle:
        print(f"Loading puzzle: {args.puzzle}")
        scenario = {"puzzle_file": args.puzzle}
    
    if LOCAL_DEBUG_MODE:
        print("🎮 LOCAL DEBUG MODE ENABLED - You are the policy!")
        print("   Type action index to choose, 'j' for raw JSON, 'q' to quit")
    
    # Run the game
    final_state = run_game(scenario)
    
    if final_state:
        print("\nFinal state summary:")
        print(f"  Turn: {final_state.get('turn')}")
        print(f"  Game Over: {final_state.get('game_over')}")

if __name__ == "__main__":
    main()
