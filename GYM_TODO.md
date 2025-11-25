# Forge Gym Environment - Future Improvements

This document tracks potential improvements for the Forge Gym environment.

## High Priority

### 1. Improve Subprocess Communication
**Current Issue:** Using stdin/stdout for JSON communication has latency and can be unreliable.

**Potential Solutions:**
- Implement a REST API in ForgeHeadless (using embedded Jetty)
- Use gRPC for Java-Python communication
- Implement JNI (Java Native Interface) for direct Python bindings
- Use py4j or JPype for Python-Java bridge

### 2. Process Lifecycle Management
**Current Issue:** Starting a new Java process for each episode is expensive.

**Potential Solutions:**
- Keep the Java process alive across episodes
- Implement a process pool
- Add a reset mechanism in ForgeHeadless that doesn't require process restart

### 3. Enhanced Observation Space
**Current Issue:** Observation space is simplified and doesn't include full card information.

**Potential Solutions:**
- Add card embeddings/encodings to observations
- Include mana pool state
- Add card types and abilities as features
- Implement graph-based observation (cards as nodes, relationships as edges)

## Medium Priority

### 4. Action Masking
**Issue:** Invalid actions are filtered by the game engine, but action masking isn't exposed.

**Solution:**
- Add a method to get valid action mask
- Integrate with RL libraries that support action masking (e.g., SB3 with ActionMasker wrapper)

### 5. Vectorized Environments
**Issue:** Training is slow with single environment.

**Solution:**
- Implement SubprocVecEnv or DummyVecEnv compatibility
- Support multiple parallel game instances

### 6. Observation Normalization
**Issue:** Observations aren't normalized, which can hurt RL performance.

**Solution:**
- Add optional observation normalization
- Provide statistics for normalization (min/max values, running mean/std)

### 7. Better Reward Shaping
**Issue:** Current reward function is very simple.

**Solution:**
- Add more sophisticated reward signals:
  - Board state evaluation
  - Card advantage
  - Tempo metrics
  - Strategic objectives (e.g., control, aggro, combo)
- Make reward function configurable/pluggable

## Low Priority

### 8. Curriculum Learning Support
**Issue:** Learning from scratch is very hard.

**Solution:**
- Support deck filtering by complexity
- Gradual difficulty increase
- Predefined learning curricula

### 9. Replay/Episode Recording
**Issue:** No way to record and replay episodes.

**Solution:**
- Add episode recording to file
- Support loading and replaying episodes
- Integration with tools like Weights & Biases

### 10. Enhanced Rendering
**Issue:** Text-based rendering is limited.

**Solution:**
- Add graphical rendering mode
- Support for screenshots/videos
- Integration with Pygame or similar

### 11. Deck Configuration
**Issue:** Decks are randomly generated.

**Solution:**
- Support loading specific decks
- Deck-building curriculum
- Meta-game considerations

### 12. Multi-Agent Features
**Issue:** Multi-agent training not well-supported.

**Solution:**
- Better support for self-play
- Population-based training
- Agent versioning and matchmaking

## Performance Optimizations

### 13. Batch Processing
- Process multiple actions in batch
- Parallel game execution

### 14. State Caching
- Cache frequently accessed state information
- Reduce JSON parsing overhead

### 15. Native Code Paths
- Critical paths in native code (C/C++)
- JIT compilation optimization

## Documentation

### 16. More Examples
- Complete RL training pipelines
- Integration with different RL frameworks (RLlib, Acme, etc.)
- Best practices guide

### 17. Benchmarks
- Baseline agent performance
- Training time benchmarks
- Performance comparisons

## Testing

### 18. Integration Tests
- Full end-to-end tests with reset() and step()
- Multi-episode tests
- Stress tests

### 19. Unit Tests
- Test individual components
- Mock subprocess communication
- Test edge cases

## Community Features

### 20. Leaderboard
- Track agent performance
- Compare different approaches
- Share trained models

### 21. Tournament System
- Agent vs agent competitions
- Automated matchmaking

## Contributing

If you'd like to work on any of these improvements:

1. Check if there's already an issue for it on GitHub
2. Open a new issue describing your proposed approach
3. Submit a PR with your implementation

For questions or discussion, join the Forge Discord community.
