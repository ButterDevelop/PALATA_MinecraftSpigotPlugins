# PALATA Minecraft Plugins & Mods

Welcome to **PALATA**, a comprehensive collection of custom plugins and mods designed for a unique, team-based Minecraft gamemode. Built for both **Fabric** and **Spigot**, PALATA delivers a competitive experience where players join teams, battle in custom arenas and raids, and progress through exciting challenges — all while maintaining a fair and secure environment with our built-in anti-cheat systems.

## Table of Contents

- [Features](#features)
- [Plugins & Mods Overview](#plugins--mods-overview)
  - [BanPlayerOnDeath](#banplayerondeath)
  - [Anti-Cheat Modules](#anti-cheat-modules)
  - [Raid & Arena System](#raid--arena-system)
  - [TPS & Ping Monitor](#tpsandping)
  - [Villager Expensive Trades](#villager-expensivetrades)
  - [Custom Resource Pack](#custom-resource-pack)
- [Upcoming Features](#upcoming-features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Team vs. Team Gameplay:** Form teams (e.g., RED and BLUE) and compete in structured battles, arenas, and raids.
- **Custom Arenas & Raids:** Dynamic arena scheduling, countdowns, and raid mechanics where teams attack each other’s bases (Nexus) and homes.
- **Anti-Cheat Systems:** Multiple layers of anti-cheat checks – both clientside (Fabric) and serverside – that verify file integrity (e.g., mod, texture, and shader hashing) and enforce fair play.
- **Death Penalties:** A unique mechanic that bans players for a configurable period upon death, complete with a countdown and notification of death coordinates.
- **TPS & Ping Monitoring:** A lightweight module for players to check their server ping and TPS.
- **Enhanced Villager Trades:** Modify villager recipes so that standard emeralds are replaced with emerald blocks, and disable certain curing effects to maintain trading difficulty.
- **Custom Resource Pack:** A dedicated resource pack that changes sounds, textures, and adds new assets to enhance the overall game experience.

## Plugins & Mods Overview

### BanPlayerOnDeath

This Spigot plugin monitors player deaths and automatically kicks and temporarily bans players for a configurable number of minutes. Key features:
- **Configurable Ban Duration:** Set via `config.yml` with the key `minutesToBan`.
- **Death Notification:** Players are informed of their death and the coordinates where they died.
- **Automated Rejoin Check:** On join, the plugin checks if a player’s ban time has expired; if not, the player is kicked with a remaining time message.
- **Toggle Command:** Administrators can enable or disable the death ban system via `/deathban on|off|info`.

### Anti-Cheat Modules

PALATA includes two distinct anti-cheat solutions:

#### Fabric Client Anti-Cheat

A client-side mod that:
- **Inspects Mods, Textures, and Shaders:** Calculates file checksums for every mod, texture, and shader.
- **Sends Inspection Data:** Communicates the results to the server via a dedicated channel.
- **Ensures Integrity:** Filters out system files and nested mods to focus only on relevant game modifications.

#### Server Anti-Cheat Plugin

A Spigot plugin that:
- **Receives and Validates Data:** Listens for incoming anti-cheat packets from clients.
- **Logs Suspicious Activity:** Writes timestamped logs to a file and console for further review.
- **Integrates with Player Events:** Monitors player join events and other interactions to enforce fair play.

### Raid & Arena System

This suite of plugins forms the heart of PALATA’s competitive gameplay:
- **Arena Manager:**  
  - **Scheduled Arenas:** Automatically schedules arena battles with countdown notifications.
  - **Team Teleportation:** Teleports players to designated team spawns (RED and BLUE) within an arena.
  - **Dynamic Arena Mechanics:** Includes extra damage in the final minute and restores arena structures post-match.
  - **Join Commands:** Players can join arenas using `/joinarena`, with time checks ensuring arenas are joined only when appropriate.

- **Raid System:**  
  - **Open & Start Raid:** Captains open raids using `/openraid` and initiate them with `/startraid` after a delay.
  - **Objective-Based Gameplay:** Attackers must destroy a specified number of obsidian blocks in the defender’s Nexus to win.
  - **Team Base & Home Management:** Commands such as `/setraidbase` and `/sethomebase` allow team captains to define their bases and homes with built-in protection checks.
  - **In-Game Commands:** Additional commands like `/joinraid`, `/cancelraid`, and `/getenemyraidbasecoords` provide raid management and situational awareness.
  - **Dragon Mechanics in The End:**  
    - **Dragon Respawn Management:** Monitors and schedules respawn of the Ender Dragon based on custom configurations.
    - **Crystal Handling:** Spawns custom-named end crystals and controls dragon battle phases.
    - **Custom XP drop:** dragon always drops the same amount of XP.
    - **Player Interaction Restrictions:** Prevents unauthorized interactions with the End portal and its surroundings.

- **Game Logic:**  
  - **Team Management:** Handles team members, captains, scoreboards, and points awarded for kills, wins, and successful raids.
  - **Safe Zones & Private Areas:** Ensures players cannot modify protected areas (Nexus, home bases) unless during an active raid.
  - **PvP Control:** Implements custom PvP rules based on world settings and proximity to team bases.

### TPSAndPing

A lightweight plugin that provides:
- **Ping Command:** Players can check their ping using `/ping`.
- **TPS Monitoring:** (If expanded) Monitor server TPS to help diagnose lag issues.

### VillagerExpensiveTrades

A plugin that modifies villager trading to increase challenge:
- **Recipe Modification:** Replaces standard emeralds in trades with emerald blocks.
- **Curing Prevention:** Cancels curing events to prevent villagers from receiving discounts or other benefits.
- **Interaction Hook:** Automatically adjusts trades when a player interacts with a villager merchant.

### Custom Resource Pack

PALATA includes its own resource pack which:
- **Replaces Sounds & Textures:** Offers new sounds (e.g., custom "dota" sounds for arena events, death, respawn) and textures.
- **Adds New Assets:** Introduces new visual elements that tie into the custom gamemode’s theme.
- **Enhances Immersion:** Provides a consistent aesthetic that aligns with PALATA’s competitive and strategic gameplay.

## Upcoming Features

- **Simultaneous End Access:** Soon, The End will be accessible by two teams at the same time — each from a different platform — with a third separate platform available for non-team players.
- **Further Anti-Cheat Enhancements:** Continued improvements to both clientside and serverside anti-cheat measures.
- **Additional Arenas & Raid Variants:** More maps, gameplay modes, and dynamic objectives to keep the competitive experience fresh.

## Installation

### Requirements

- **Minecraft Version:** Compatible with the current stable release (1.21.4).
- **Server Type:** Works with Spigot environment.
- **Java:** Ensure you have the appropriate Java version installed (21).

### Steps

1. **Clone or Download:**  
   Clone the repository or download the ZIP from [GitHub](https://github.com/ButterDevelop/PALATA_MinecraftSpigotPlugins).

2. **Build/Install Plugins & Mods:**  
   - For Spigot plugins, compile the project (e.g., using Maven or Gradle) and place the resulting JARs in your server's `plugins` folder.  
   - For Fabric mods, build the mod JAR and add it to your client’s and/or server’s `mods` folder.

3. **Resource Pack:**  
   Place the resource pack in your server resource pack folder and inform players of its URL or automatically prompt its download.

4. **Configuration:**  
   Edit the provided `config.yml` files for each module to adjust settings such as ban duration, arena timings, raid parameters, and more.

5. **Launch Server:**  
   Start your server. Ensure that all plugins load without errors and that the resource pack is applied.

## Configuration

Each module includes its own configuration file where you can adjust parameters like:
- **Ban Duration & Toggle:** `minutesToBan` and `isEnabled` in the BanPlayerOnDeath config.
- **Arena Settings:** Scheduling intervals, spawn coordinates, countdown durations, and damage multipliers.
- **Raid Settings:** Minimum player requirements, delay times, nexus/home coordinates, cooldowns, and scoring.
- **Dragon & End Settings:** Respawn intervals, custom XP drop, crystal positions, and end portal configurations.
- **TPS & Ping:** No configuration required for basic functionality.
- **Villager Trades:** Adjustments to trade recipes occur automatically on interaction.

Refer to the inline comments in each plugin’s source for further details.

## Contributing

Contributions are welcome! If you’d like to improve the code or add new features:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes with clear messages.
4. Push to your branch and open a pull request.

Please ensure your code follows the project’s style guidelines and is well-tested.

## License

This project is licensed under the [MIT License](LICENSE).

---

Dive into PALATA to experience innovative team-based strategy, challenging PvP battles, and immersive gameplay enhancements in Minecraft. Enjoy the game and remember: teamwork, strategy, and fair play are key!
