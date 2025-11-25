"""
Forge Gymnasium Environment

This module provides a Gymnasium-compatible environment for the Forge MTG game engine.
The environment communicates with the ForgeHeadless Java process via subprocess.
"""

import json
import subprocess
import sys
from typing import Any, Dict, List, Optional, Tuple
import numpy as np
import gymnasium as gym
from gymnasium import spaces


class ForgeEnv(gym.Env):
    """
    A Gymnasium environment for Forge MTG headless interface.
    
    This environment wraps the ForgeHeadless Java application and provides
    a standard RL interface compatible with Gymnasium/OpenAI Gym.
    
    Observation Space:
        Dict containing:
        - turn: Current turn number
        - phase: Current phase name
        - active_player: ID of active player (0 or 1)
        - stack_size: Number of items on stack
        - players: Array of player states (life, library count, hand size, etc.)
        
    Action Space:
        Discrete(n) where n is dynamically determined by available actions
        Actions are indexed and correspond to the possible_actions list
        
    Example:
        >>> env = ForgeEnv()
        >>> observation, info = env.reset()
        >>> action = env.action_space.sample()  # Random action
        >>> observation, reward, terminated, truncated, info = env.step(action)
    """
    
    metadata = {"render_modes": ["human", "ansi"]}
    
    def __init__(
        self,
        jar_path: Optional[str] = None,
        java_home: Optional[str] = None,
        player1_is_human: bool = True,
        player2_is_human: bool = False,
        max_turns: int = 100,
        render_mode: Optional[str] = None,
    ):
        """
        Initialize the Forge Gym environment.
        
        Args:
            jar_path: Path to the forge-gui-desktop JAR file. If None, uses default.
            java_home: Path to Java home directory. If None, uses system java.
            player1_is_human: Whether player 1 is human-controlled (True) or AI (False)
            player2_is_human: Whether player 2 is human-controlled (True) or AI (False)
            max_turns: Maximum number of turns before episode terminates
            render_mode: How to render the environment ("human" or "ansi")
        """
        super().__init__()
        
        self.jar_path = jar_path or self._find_jar_path()
        self.java_cmd = self._get_java_cmd(java_home)
        self.player1_is_human = player1_is_human
        self.player2_is_human = player2_is_human
        self.max_turns = max_turns
        self.render_mode = render_mode
        
        # Process handle
        self.process: Optional[subprocess.Popen] = None
        
        # Game state
        self.current_state: Optional[Dict] = None
        self.current_actions: List[Dict] = []
        self.episode_turns = 0
        self.game_over = False
        
        # Define observation space (will be refined based on actual state)
        # This is a simplified version - real observations would be more complex
        self.observation_space = spaces.Dict({
            "turn": spaces.Box(low=0, high=self.max_turns, shape=(1,), dtype=np.int32),
            "phase": spaces.Discrete(10),  # Simplified - MTG has ~10 phases
            "active_player": spaces.Discrete(2),
            "stack_size": spaces.Box(low=0, high=100, shape=(1,), dtype=np.int32),
            "player_0_life": spaces.Box(low=-100, high=100, shape=(1,), dtype=np.int32),
            "player_1_life": spaces.Box(low=-100, high=100, shape=(1,), dtype=np.int32),
            "player_0_hand_size": spaces.Box(low=0, high=20, shape=(1,), dtype=np.int32),
            "player_1_hand_size": spaces.Box(low=0, high=20, shape=(1,), dtype=np.int32),
        })
        
        # Action space is dynamic but we'll use a large enough discrete space
        # Actions beyond the available count will be treated as pass_priority
        self.action_space = spaces.Discrete(200)
        
    def _find_jar_path(self) -> str:
        """Find the JAR file path in the forge-gui-desktop target directory."""
        import os
        import glob
        
        # Try to find the jar in the standard location
        jar_pattern = os.path.join(
            os.path.dirname(__file__),
            "..",
            "forge-gui-desktop",
            "target",
            "forge-gui-desktop-*-jar-with-dependencies.jar"
        )
        
        matches = glob.glob(jar_pattern)
        if matches:
            return os.path.abspath(matches[0])
        
        # Fallback to a fixed path
        default_path = os.path.join(
            os.path.dirname(__file__),
            "..",
            "forge-gui-desktop",
            "target",
            "forge-gui-desktop-2.0.07-SNAPSHOT-jar-with-dependencies.jar"
        )
        return os.path.abspath(default_path)
    
    def _get_java_cmd(self, java_home: Optional[str]) -> str:
        """Get the java command to use."""
        if java_home:
            import os
            return os.path.join(java_home, "bin", "java")
        return "java"
    
    def reset(
        self,
        seed: Optional[int] = None,
        options: Optional[Dict[str, Any]] = None,
    ) -> Tuple[Dict, Dict]:
        """
        Reset the environment to start a new episode.
        
        Args:
            seed: Random seed for reproducibility
            options: Additional options for reset
            
        Returns:
            observation: Initial observation
            info: Additional information
        """
        super().reset(seed=seed)
        
        # Close existing process if any
        if self.process is not None:
            self._close_process()
        
        # Start the ForgeHeadless process
        args = [
            self.java_cmd,
            "-Xmx4096m",
            "-cp",
            self.jar_path,
            "forge.view.ForgeHeadless"
        ]
        
        # Add player configuration flags
        if not self.player1_is_human and not self.player2_is_human:
            args.append("--both-ai")
        elif self.player1_is_human and self.player2_is_human:
            args.append("--both-human")
        else:
            if not self.player1_is_human:
                args.append("--p1-ai")
            if self.player2_is_human:
                args.append("--p2-human")
        
        self.process = subprocess.Popen(
            args,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
        )
        
        # Wait for game to initialize
        import time
        time.sleep(3)
        
        # Get initial state
        self.episode_turns = 0
        self.game_over = False
        self._update_state()
        
        observation = self._get_observation()
        info = {"state": self.current_state, "actions": self.current_actions}
        
        return observation, info
    
    def step(
        self, action: int
    ) -> Tuple[Dict, float, bool, bool, Dict]:
        """
        Execute an action in the environment.
        
        Args:
            action: Action index to execute
            
        Returns:
            observation: New observation after action
            reward: Reward for the action
            terminated: Whether the episode is over (game ended)
            truncated: Whether the episode was truncated (max turns)
            info: Additional information
        """
        if self.process is None:
            raise RuntimeError("Environment not initialized. Call reset() first.")
        
        # Execute the action
        if action < len(self.current_actions):
            # Play the specific action
            command = f"play_action {action}\n"
        else:
            # Default to passing priority if action is out of range
            command = "pass_priority\n"
        
        self._send_command(command)
        
        # Wait for action to be processed
        import time
        time.sleep(0.5)
        
        # Update state
        previous_state = self.current_state.copy() if self.current_state else None
        self._update_state()
        
        # Calculate reward
        reward = self._calculate_reward(previous_state, self.current_state)
        
        # Check if game is over
        terminated = self._is_terminated()
        truncated = self.episode_turns >= self.max_turns
        
        observation = self._get_observation()
        info = {
            "state": self.current_state,
            "actions": self.current_actions,
            "action_taken": action
        }
        
        return observation, reward, terminated, truncated, info
    
    def render(self):
        """Render the environment."""
        if self.render_mode == "human" or self.render_mode == "ansi":
            if self.current_state:
                print("\n" + "="*60)
                print(f"Turn {self.current_state.get('turn', 0)} - Phase: {self.current_state.get('phase', 'Unknown')}")
                print(f"Active Player: {self.current_state.get('activePlayerId', 0)}")
                print(f"Stack Size: {self.current_state.get('stack_size', 0)}")
                
                for player in self.current_state.get('players', []):
                    print(f"\n{player['name']} (ID: {player['id']}):")
                    print(f"  Life: {player['life']}")
                    print(f"  Hand: {len(player.get('hand', []))} cards")
                    print(f"  Library: {player['libraryCount']} cards")
                    print(f"  Battlefield: {len(player.get('battlefield', []))} cards")
                
                print("\nAvailable Actions:")
                for i, action in enumerate(self.current_actions):
                    action_type = action.get('type', 'unknown')
                    card_name = action.get('card_name', '')
                    print(f"  {i}: {action_type} - {card_name}")
                print("="*60 + "\n")
    
    def close(self):
        """Clean up resources."""
        self._close_process()
    
    def _send_command(self, command: str):
        """Send a command to the ForgeHeadless process."""
        if self.process and self.process.stdin:
            self.process.stdin.write(command)
            self.process.stdin.flush()
    
    def _read_response(self, timeout: float = 2.0) -> str:
        """Read response from ForgeHeadless process."""
        if not self.process or not self.process.stdout:
            return ""
        
        import select
        import time
        
        response_lines = []
        start_time = time.time()
        
        while time.time() - start_time < timeout:
            # Check if data is available
            if sys.platform == "win32":
                # Windows doesn't support select on pipes
                try:
                    line = self.process.stdout.readline()
                    if line:
                        response_lines.append(line)
                        # Look for JSON end marker
                        if line.strip() in ["}", "]"]:
                            break
                except:
                    break
            else:
                # Unix-like systems
                ready, _, _ = select.select([self.process.stdout], [], [], 0.1)
                if ready:
                    line = self.process.stdout.readline()
                    if line:
                        response_lines.append(line)
                        # Look for JSON end marker
                        if line.strip() in ["}", "]"]:
                            break
        
        return "".join(response_lines)
    
    def _update_state(self):
        """Update the current game state and available actions."""
        # Get current state
        self._send_command("get_state\n")
        import time
        time.sleep(0.3)
        state_response = self._read_response()
        
        try:
            self.current_state = json.loads(state_response)
            self.episode_turns = self.current_state.get('turn', 0)
        except json.JSONDecodeError:
            # If we can't parse state, game might be over
            self.current_state = {}
        
        # Get possible actions
        self._send_command("possible_actions\n")
        time.sleep(0.3)
        actions_response = self._read_response()
        
        try:
            actions_data = json.loads(actions_response)
            self.current_actions = actions_data.get('actions', [])
        except json.JSONDecodeError:
            self.current_actions = []
    
    def _get_observation(self) -> Dict:
        """Convert current state to observation format."""
        if not self.current_state:
            # Return default observation
            return {
                "turn": np.array([0], dtype=np.int32),
                "phase": 0,
                "active_player": 0,
                "stack_size": np.array([0], dtype=np.int32),
                "player_0_life": np.array([20], dtype=np.int32),
                "player_1_life": np.array([20], dtype=np.int32),
                "player_0_hand_size": np.array([0], dtype=np.int32),
                "player_1_hand_size": np.array([0], dtype=np.int32),
            }
        
        # Phase mapping (simplified)
        phase_map = {
            "UNTAP": 0, "UPKEEP": 1, "DRAW": 2,
            "MAIN1": 3, "COMBAT_BEGIN": 4, "COMBAT_DECLARE_ATTACKERS": 5,
            "COMBAT_DECLARE_BLOCKERS": 6, "COMBAT_FIRST_STRIKE_DAMAGE": 7,
            "COMBAT_DAMAGE": 8, "COMBAT_END": 9, "MAIN2": 3, "END_OF_TURN": 1,
            "CLEANUP": 1
        }
        
        players = self.current_state.get('players', [{}, {}])
        
        return {
            "turn": np.array([self.current_state.get('turn', 0)], dtype=np.int32),
            "phase": phase_map.get(self.current_state.get('phase', 'UNTAP'), 0),
            "active_player": self.current_state.get('activePlayerId', 0),
            "stack_size": np.array([self.current_state.get('stack_size', 0)], dtype=np.int32),
            "player_0_life": np.array([players[0].get('life', 20) if len(players) > 0 else 20], dtype=np.int32),
            "player_1_life": np.array([players[1].get('life', 20) if len(players) > 1 else 20], dtype=np.int32),
            "player_0_hand_size": np.array([len(players[0].get('hand', [])) if len(players) > 0 else 0], dtype=np.int32),
            "player_1_hand_size": np.array([len(players[1].get('hand', [])) if len(players) > 1 else 0], dtype=np.int32),
        }
    
    def _calculate_reward(
        self, previous_state: Optional[Dict], current_state: Dict
    ) -> float:
        """
        Calculate reward based on state transition.
        
        Simple reward function:
        - Positive reward for opponent losing life
        - Negative reward for losing life
        - Large positive reward for winning
        - Large negative reward for losing
        """
        if not previous_state or not current_state:
            return 0.0
        
        reward = 0.0
        
        prev_players = previous_state.get('players', [{}, {}])
        curr_players = current_state.get('players', [{}, {}])
        
        if len(prev_players) >= 2 and len(curr_players) >= 2:
            # Player 0's perspective (assuming agent is player 0)
            prev_p0_life = prev_players[0].get('life', 20)
            curr_p0_life = curr_players[0].get('life', 20)
            prev_p1_life = prev_players[1].get('life', 20)
            curr_p1_life = curr_players[1].get('life', 20)
            
            # Reward for opponent losing life
            if curr_p1_life < prev_p1_life:
                reward += (prev_p1_life - curr_p1_life) * 0.1
            
            # Penalty for losing life
            if curr_p0_life < prev_p0_life:
                reward -= (prev_p0_life - curr_p0_life) * 0.1
            
            # Large reward/penalty for game ending
            if curr_p1_life <= 0:
                reward += 10.0  # Win
            elif curr_p0_life <= 0:
                reward -= 10.0  # Lose
        
        return reward
    
    def _is_terminated(self) -> bool:
        """Check if the game has ended."""
        if not self.current_state:
            return True
        
        players = self.current_state.get('players', [])
        
        # Game ends if any player is at 0 or less life
        for player in players:
            if player.get('life', 20) <= 0:
                return True
        
        return False
    
    def _close_process(self):
        """Close the ForgeHeadless process."""
        if self.process:
            try:
                # Try to send concede command
                self._send_command("concede\n")
                import time
                time.sleep(0.5)
            except:
                pass
            
            try:
                self.process.terminate()
                self.process.wait(timeout=2)
            except:
                self.process.kill()
                self.process.wait()
            
            self.process = None
