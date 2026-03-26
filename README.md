<div align="center">
<p align="right">
  <b>English</b> | <a href="README_RU.md">Русский</a>
</p>

<img src="icon.png" width="128" height="128" alt="Archetype Icon">

# ⚔ Archetype

### Class System for Minecraft

![MC](https://img.shields.io/badge/Minecraft-1.20.1-62b447?style=flat-square&logo=minecraft&logoColor=white)
![Forge](https://img.shields.io/badge/Forge-multiloader-e07a33?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-multiloader-c9b88a?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-f89820?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square)

*Every bonus is compensated by a penalty. There is no perfect class — only your choice.*

</div>

---

## Overview

Archetype introduces a comprehensive class system that fundamentally alters the gameplay experience for every player. Classes redefine core attributes, grant unique active and passive abilities, and impose strategic restrictions.

**Core Philosophy — Balance through Compromise:**

| Class | Strength | Weakness |
|-------|----------|----------|
| 🧛 Vi | High damage, crit mechanics | Vulnerable without stored resource |
| 🗿 Ram | Fire, rage, durability | Limited mobility |
| 🌀 Lin Qi | Chaotic magic | Unstable effects |
| ☯ Ru Yi | Resource balancing | Highly state-dependent |
| 🦷 Summoner | Fang summoning (line / target / circle) | Carnivore diet, villagers refuse trading |
| 🎭 Morph | Transformation into 5 forms with unique bonuses | Weakened and slowed while in base form |

Classes are defined via JSON files — server owners can add custom classes via datapacks without writing a single line of Java.

---

## Architecture

A multiloader project powered by [Architectury](https://github.com/architectury/architectury-api) — featuring a shared core module and platform-specific layers:

```
archetype/
├── common/     95% of code — core logic, abilities, networking, GUI, commands
├── forge/      Forge implementation: Capabilities, SimpleChannel, events
└── fabric/     Fabric implementation: Data Attachments, Networking API, events
```

Platform-dependent code is isolated using ServiceLoader (`NetworkHandler`, `PlayerDataAccess`, `PlatformHelper`).

### Modules

| Module | Purpose |
|--------|---------|
| `core/` | `ClassManager` lifecycle. `PlayerClass` definitions. |
| `ability/` | 25 passive + 14 active types. Factories, interfaces, registry. |
| `condition/` | 12 condition types + logic gates (`and` / `or` / `not`). |
| `data/` | `PlayerClassData` — player state and NBT serialization. |
| `network/` | 8 packet types, server-side validation, client-side sync. |
| `registry/` | `ClassRegistry` — JSON loading via `SimpleJsonResourceReloadListener`. |
| `gui/` | Selection screen, dossier, and HUD overlay. |
| `command/` | `/archetype set/remove/get/list/reload` |
| `item/` | Rebirth Scroll — held item for class switching. |
| `config/` | Server and client configurations. |
| `advancement/` | Custom advancements for classes and abilities. |

---

## Class Format (JSON)

Define a new class in `data/<namespace>/archetype_classes/`:

```json
{
  "id": "archetype:my_class",
  "color": "#8B0000",
  "icon": "archetype:textures/gui/icons/my_class.png",
  "attributes": [
    { "attribute": "generic.max_health", "amount": 4.0, "operation": "addition" }
  ],
  "passives": [
    { "type": "archetype:regen", "params": { "amount": 0.5, "interval": 20 } }
  ],
  "actives": [
    { "type": "archetype:dash", "cooldown": 100, "params": { "power": 1.5 } }
  ]
}
```

---

## Modding API

```java
// Register new ability types
ArchetypeAPI.registerAbilityType(id, factory);
ArchetypeAPI.registerPassiveType(id, factory);
ArchetypeAPI.registerConditionType(id, factory);

// Manage player classes
ArchetypeAPI.getPlayerClass(player);
ArchetypeAPI.assignClass(serverPlayer, classId);
```

---

## Build Instructions
 
 ю б

```bash
./gradlew build
```

Artifacts are found in: `forge/build/libs/` · `fabric/build/libs/`

**Dependencies:** Minecraft 1.20.1 · Architectury API · Fabric API (for Fabric build) · Java 17

---

## License

All Rights Reserved © ZorahM
