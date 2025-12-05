# ⚔️  Forge: The Magic: The Gathering Rules Engine

Join the **Forge community** on [Discord](https://discord.gg/HcPJNyD66a)!

[![Test build](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml/badge.svg)](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml)

---

## ✨ Introduction

**Forge** is a dynamic and open-source **Rules Engine** tailored for **Magic: The Gathering** enthusiasts. Developed by a community of passionate programmers, Forge allows players to explore the rich universe of MTG through a flexible, engaging platform. 

**Note:** Forge operates independently and is not affiliated with Wizards of the Coast.

---

## 🌟 Key Features

- **🌐 Cross-Platform Support:** Play on **Windows, Mac, Linux,** and **Android**.
- **🔧 Extensible Architecture:** Built in **Java**, Forge encourages developers to contribute by adding features and cards.
- **🎮 Versatile Gameplay:** Dive into single-player modes or challenge opponents online!

---

## 🛠️ Installation Guide

### 📥 Desktop Installation
1. **Latest Releases:** Download the latest version [here](https://github.com/Card-Forge/forge/releases/latest).
2. **Snapshot Build:** For the latest development version, grab the `forge-gui-desktop` tarball from our [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots).
   - **Tip:** Extract to a new folder to prevent version conflicts.
3. **User Data Management:** Previous players’ data is preserved during upgrades.
4. **Java Requirement:** Ensure you have **Java 17 or later** installed.

### 📱 Android Installation
- _(Note: **Android 11** is the minimum requirement with at least **6GB RAM** to run smoothly. You need to enable **"Install unknown apps"** for Forge to initialize and update itself)_
- Download the **APK** from the [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots). On the first launch, Forge will automatically download all necessary assets.

---

## 🎮 Modes of Play

Forge offers various exciting gameplay options:

### 🌍 Adventure Mode
Embark on a thrilling single-player journey where you can:
- Explore an overworld map.
- Challenge diverse AI opponents.
- Collect cards and items to boost your abilities.

<img width="1282" height="752" alt="Shandalar World" src="https://github.com/user-attachments/assets/9af31471-d688-442f-9418-9807d8635b72" />

### 🔍 Quest Modes
Engage in focused gameplay without the overworld exploration—perfect for quick sessions!

<img width="1282" height="752" alt="Quest Duels" src="https://github.com/user-attachments/assets/b9613b1c-e8c3-4320-8044-6922c519aad4" />

### 🤖 AI Formats
Test your skills against AI in multiple formats:
- **Sealed**
- **Draft**
- **Commander**
- **Cube**

For comprehensive gameplay instructions, visit our [User Guide](https://github.com/Card-Forge/forge/wiki/User-Guide).

<img width="1282" height="752" alt="Sealed" src="https://github.com/user-attachments/assets/ae603dbd-4421-4753-a333-87cb0a28d772" />

---

## 🤖 Reinforcement Learning & AI Development

Forge includes a **Gymnasium-compatible environment** for training reinforcement learning agents!

### 🐍 Python Gym Environment

Train AI agents to play Magic: The Gathering using popular RL frameworks like Stable-Baselines3, RLlib, or any Gymnasium-compatible library.

```python
import forge_gym

# Create the environment
env = forge_gym.ForgeEnv()

# Standard Gym API
observation, info = env.reset()
action = env.action_space.sample()
observation, reward, terminated, truncated, info = env.step(action)
```

**📚 Full Documentation:** See [GYM_README.md](GYM_README.md) for complete setup instructions and examples.

**✨ Features:**
- Standard Gymnasium API (reset, step, render, close)
- **Sparse rewards by default** (learn from win/loss, not individual steps)
- Configurable reward modes (sparse or dense)
- Configurable player modes (human/AI controlled)
- Rich observations (life, hand size, battlefield state)
- Dynamic action space for playing lands, spells, and abilities
- Compatible with popular RL libraries

---

## 💬 Support & Community

Need help? Join our vibrant Discord community! 
- 📜 Read the **#rules** and explore the **FAQ**.
- ❓ Ask your questions in the **#help** channel for assistance.

---

## 🤝 Contributing to Forge

We love community contributions! Interested in helping? Check out our [Contributing Guidelines](CONTRIBUTING.md) for details on how to get started.

---

## ℹ️ About Forge

Forge aims to deliver an immersive and customizable Magic: The Gathering experience for fans around the world. 

### 📊 Repository Statistics

| Metric         | Count                                                       |
|----------------|-------------------------------------------------------------|
| **⭐ Stars:**   | [![GitHub stars](https://img.shields.io/github/stars/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/stargazers) |
| **🍴 Forks:**   | [![GitHub forks](https://img.shields.io/github/forks/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/network) |
| **👥 Contributors:** | [![GitHub contributors](https://img.shields.io/github/contributors/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/graphs/contributors) |

---

**📄 License:** [GPL-3.0](LICENSE)
<div align="center" style="display: flex; align-items: center; justify-content: center;">
    <div style="margin-left: auto;">
        <a href="#top">
            <img src="https://img.shields.io/badge/Back%20to%20Top-000000?style=for-the-badge&logo=github&logoColor=white" alt="Back to Top">
        </a>
    </div>
</div>
