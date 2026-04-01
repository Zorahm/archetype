# Archetype — Minecraft Class System Mod

## Overview

A class system mod for Minecraft 1.20.1. Multi-loader (Forge + Fabric) via Architectury. Java 17, Official Mojang Mappings.

**Core principle:** balance through compromise — every bonus is offset by a penalty.

## Branch Workflow

The repository has two active branches: **`master`** and **`1.20.1`**.

**All commits and changes MUST be applied to both branches.** We are actively developing for Minecraft 1.20.1, so both branches must stay in sync at all times.

```bash
# After committing to master, switch and cherry-pick (or merge) to 1.20.1
git checkout 1.20.1
git cherry-pick <commit-hash>
git checkout master
```

Never leave changes only in one branch — both must always reflect the latest state.

## Project Structure

```
archetype/
├── common/    # ~95% of code. Core, abilities, conditions, network, GUI, commands
├── fabric/    # Fabric layer: Data Attachments, Fabric Networking API, events
├── forge/     # Forge layer: Capabilities, SimpleChannel, events
```

Package: `com.mod.archetype` (common), `com.mod.archetype.fabric`, `com.mod.archetype.forge`.

## Build

```bash
./gradlew build
```

Artifacts: `fabric/build/libs/*.jar`, `forge/build/libs/*.jar`.

Shadow JAR includes common in each platform module.

## Architectural Decisions

- **Platform abstraction via ServiceLoader**: `NetworkHandler`, `PlayerDataAccess`, `PlatformHelper` — interfaces in `platform/`, implementations in forge/fabric modules
- **Singleton + private constructor**: `ClassManager.getInstance()`, `AbilityRegistry.getInstance()`, `ConditionRegistry.getInstance()`
- **Factory pattern**: `PassiveAbilityFactory`, `ActiveAbilityFactory`, `ConditionFactory` — create instances from JSON parameters
- **Immutable records**: `PlayerClass`, `AttributeModifierEntry`, `ConditionalAttributeEntry`, `ResourceDefinition`, `ConditionDefinition`
- **JSON datapacks**: classes are loaded from `data/<namespace>/archetype_classes/*.json` via `ClassRegistry` (extends `SimpleJsonResourceReloadListener`)

## Key Modules (common)

| Package | Purpose |
|---------|---------|
| `core/` | `ClassManager` — lifecycle (assign/tick/remove). `PlayerClass` — class definition. `ActiveClassInstance` — cache of instantiated abilities |
| `ability/` | 25 passive + 14 active types. Interfaces `PassiveAbility`, `ActiveAbility`. Registry `AbilityRegistry` |
| `condition/` | 12 condition types + combinators (And/Or/Not). `ConditionRegistry` |
| `data/` | `PlayerClassData` — player state: class, level, xp, resource, cooldowns, toggles. NBT serialization |
| `network/` | 8 packets (ClassSelect, AbilityUse/Release, OpenClassSelection, SyncClassData, ClassAssignResult, PlayerClassSync). Server-side validation |
| `registry/` | `ClassRegistry` — loads JSON classes from datapacks |
| `gui/` | `ClassSelectionScreen`, `ClassDetailScreen`, `ClassInfoScreen`, `AbilityHudOverlay` |
| `command/` | `/archetype set/remove/get/list/reload` |
| `item/` | `RebirthScrollItem` — class-change item |

## Class Lifecycle

1. **Assign**: apply attributes → passives.onApply() → actives instantiated → resource init → save data → sync → event
2. **Tick** (every tick): cooldowns -1 → (every 20 ticks) check conditions, tick passives, update resource → sync
3. **Remove**: remove attributes → passives.onRemove() → deactivate actives → clear data → sync → event

## Code Conventions

- **Language**: Java 17, no Kotlin
- **Naming**: PascalCase for classes, camelCase for methods (verb-first), UPPER_SNAKE_CASE for constants
- **Null safety**: `@Nullable` from org.jetbrains.annotations
- **Comments**: minimal, code is self-documenting. Do not add Javadoc unless requested
- **Logging**: SLF4J via `Archetype.LOGGER`
- **Mod ID**: `Archetype.MOD_ID` ("archetype"), never hardcode strings
- **ResourceLocation**: `new ResourceLocation(Archetype.MOD_ID, "name")`

## Extensibility (API)

```java
ArchetypeAPI.registerAbilityType(id, factory);     // Register new active ability type
ArchetypeAPI.registerPassiveType(id, factory);      // Register new passive ability type
ArchetypeAPI.registerConditionType(id, factory);    // Register new condition type
ArchetypeAPI.getPlayerClass(player);                // Get player's current class
ArchetypeAPI.assignClass(serverPlayer, classId);    // Assign a class to a player
```

## Releases

- Release notes must be written in **English**
- Attach only two JAR files: `archetype-<version>-fabric.jar` and `archetype-<version>-forge.jar`
- Do **not** attach dev-shadow or sources JARs
- Take JARs from GitHub Actions artifacts after the build completes

## Localization

Two languages: `en_us.json`, `ru_ru.json`. Key namespaces:
- `class.archetype.<id>.*` — class names and descriptions
- `ability.archetype.<id>.<ability>.*` — active abilities
- `passive.archetype.<id>.<passive>.*` — passive abilities
- `gui.archetype.*` — interface
- `commands.archetype.*` — commands

## Dependencies

Minecraft + Architectury API only. No external libraries. JSR305 — compileOnly.
