# Archetype — Datapack Guide

This guide is for adding custom classes to Archetype via a Minecraft datapack — no Java required.

---

## Quick start

A class is one JSON file at:

```
data/<your_namespace>/archetype_classes/<class_id>.json
```

Minimal example:

```json
{
  "name": "class.mypack.warrior.name",
  "description": "class.mypack.warrior.description",
  "icon": "mypack:textures/gui/class/warrior.png",
  "color": "AA3322"
}
```

After placing the file, run `/reload` (or `/archetype reload`). The class becomes selectable.

The class is identified by `<namespace>:<filename>` — e.g. `mypack:warrior` for the file above.

---

## Schema

### Required

| Field         | Type   | Description |
|---------------|--------|-------------|
| `name`        | string | Localization key for the display name. |
| `description` | string | Localization key for the short description. |
| `icon`        | string | Identifier of a 32×32 PNG (`namespace:textures/...`). |
| `color`       | string | RGB hex without `#`, e.g. `"8800AA"`. Used for accents. |

### Optional

| Field                    | Type     | Default     | Description |
|--------------------------|----------|-------------|-------------|
| `category`               | enum     | `DAMAGE`    | `DAMAGE`, `MOBILITY`, `SUPPORT`, `UTILITY`. |
| `lore`                   | string[] | `[]`        | Lang keys, one per lore line. |
| `attributes`             | object[] | `[]`        | Always-on attribute modifiers. |
| `conditional_attributes` | object[] | `[]`        | Modifiers gated by a `condition`. |
| `passive_abilities`      | object[] | `[]`        | Ongoing effects. |
| `active_abilities`       | object[] | `[]`        | Triggered abilities, max 3 (one per slot). |
| `resource`               | object   | none        | Custom resource bar (mana / energy / etc.). |
| `size_modifier`          | float    | none        | Entity scale multiplier. |
| `incompatible_with`      | string[] | `[]`        | Class IDs that this one conflicts with. |
| `progression`            | object[] | `[]`        | Level milestones for the class screen. |
| `extra_ability_sections` | object[] | `[]`        | Sub-sections under an active ability in the dossier. |
| `ability_stats`          | object[] | `[]`        | Numeric stats shown in the dossier. |
| `commands`               | object[] | `[]`        | Vanilla commands run on lifecycle events. |

---

## Attributes

```json
"attributes": [
  { "attribute": "minecraft:max_health",     "operation": "ADDITION",       "value": -6.0 },
  { "attribute": "minecraft:attack_damage",  "operation": "MULTIPLY_BASE",  "value":  0.25 }
]
```

- `attribute`: any vanilla or modded attribute identifier.
- `operation`: `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL` (case-insensitive).
- `value`: double.

### Conditional attributes

Apply only while a condition holds:

```json
"conditional_attributes": [
  {
    "condition": { "type": "archetype:is_sneaking" },
    "modifiers": [
      { "attribute": "minecraft:movement_speed", "operation": "MULTIPLY_BASE", "value": -0.3 }
    ]
  }
]
```

---

## Active abilities

Each entry occupies one of three slots (`ability_1`, `ability_2`, `ability_3`). A slot may not be used twice in the same class.

```json
"active_abilities": [
  {
    "type": "archetype:dash",
    "slot": "ability_1",
    "cooldown": 100,
    "resource_cost": 0,
    "unlock_level": 0,
    "name": "ability.mypack.warrior.dash.name",
    "description": "ability.mypack.warrior.dash.description",
    "icon": "mypack:textures/gui/ability/dash.png",
    "item": "minecraft:feather",
    "params": { "dash_speed": 1.5, "damage": 4.0 }
  }
]
```

- `cooldown` — ticks (20 = 1s).
- `resource_cost` — units of the class's `resource`.
- `unlock_level` — class level required to use it.
- `item` — optional item ID; if set, holding the item triggers the ability.
- `params` — type-specific; see the **Built-in active types** table below.

### Built-in active types

All registered as `archetype:<id>`.

| Type | Purpose |
|------|---------|
| `timed_buff` | Apply potion effects to self for a duration. |
| `area_attack` | Damage entities in a radius. |
| `targeted_effect` | Apply an effect to a single targeted entity. |
| `projectile` | Fire a vanilla projectile. |
| `dash` | Forward burst of velocity, optional damage / fire trail. |
| `teleport` | Short teleport along look vector. |
| `morph` | Transform player visual model. |
| `toggle` | Stateful on/off ability. |
| `summon` | Spawn an entity. |
| `self_heal` | Restore health. |
| `charged` | Hold-to-charge ability. |
| `blood_drain` | Damage target and heal self. |
| `vi_dash` | Multi-charge dash with elemental variants. |
| `rage_dash` | Dash whose strength scales with missing health. |
| `random_projectile` | Picks a random projectile from a list. |
| `random_teleport` | Teleport to a random nearby location. |
| `evoker_fangs` | Spawn evoker-fang lines. |
| `chase_teleport` | Teleport behind the looked-at entity. |
| `antigravity_throw` | Lift entities upward in an area. |
| `form_shift` | Switch between morph forms. |
| `form_cancel` | Drop the current morph. |

For the exact `params` each type accepts, read the matching class under [common/src/main/java/com/mod/archetype/ability/active/](common/src/main/java/com/mod/archetype/ability/active/) — the constructor and `readParams()` show every key.

---

## Passive abilities

```json
"passive_abilities": [
  {
    "type": "archetype:lifesteal",
    "params": { "fraction": 0.1 },
    "positive": true,
    "name": "passive.mypack.warrior.lifesteal.name",
    "description": "passive.mypack.warrior.lifesteal.description"
  },
  {
    "type": "archetype:sun_damage",
    "condition": { "type": "archetype:under_open_sky" },
    "positive": false,
    "hidden": false,
    "name": "passive.mypack.warrior.sun_burn.name",
    "description": "passive.mypack.warrior.sun_burn.description"
  }
]
```

- `condition` — optional gate; passive only ticks when the condition is true.
- `positive` — affects the colour shown in the dossier.
- `hidden` — exclude from the UI.

### Built-in passive types

All `archetype:<id>`:

`sun_damage`, `night_vision`, `food_restriction`, `natural_regeneration_disabled`, `undead_type`,
`water_vulnerability`, `totem_offhand_damage`, `sink_in_water`, `no_fall_damage`, `fire_immunity`,
`custom_diet`, `mob_neutral`, `magnetic_pull`, `thorns_passive`, `lifesteal`,
`wall_climb`, `slow_fall`, `jump_boost`, `breath_underwater`, `effect_immunity`,
`no_sneak`, `fire_attack`, `destroy_holy_items`, `elytra_fragility`, `shield_block`,
`tool_fragility`, `random_enchant`, `arrow_transmute`, `potion_create`, `villager_rejection`,
`water_food_damage`, `potion_block`, `formless_debuff`, `xp_attribute_scaling`.

For parameters, see [common/src/main/java/com/mod/archetype/ability/passive/](common/src/main/java/com/mod/archetype/ability/passive/).

---

## Conditions

Used by `conditional_attributes` and by `passive_abilities[].condition`.

```json
{ "type": "archetype:health_below_percent", "percent": 0.3 }
```

### Built-in condition types

All `archetype:<id>`:

| Type | Params |
|------|--------|
| `time_of_day` | `"range": [start, end]` (day-time ticks 0–24000) |
| `health_below_percent` | `"percent": 0.0–1.0` |
| `health_above_percent` | `"percent": 0.0–1.0` |
| `in_water` | — |
| `underwater` | — |
| `under_open_sky` | — |
| `in_dimension` | `"dimension": "minecraft:overworld"` |
| `in_biome_tag` | `"tag": "minecraft:is_forest"` |
| `is_sneaking` | — |
| `is_sprinting` | — |
| `on_fire` | — |
| `has_item` | `"item": "minecraft:diamond"`, `"slot": "mainhand" \| "offhand" \| "armor_head" \| "armor_chest" \| "armor_legs" \| "armor_feet" \| "any"` |

### Combinators

`and`, `or`, `not` — both bare and `archetype:`-prefixed forms are accepted.

```json
{
  "type": "and",
  "conditions": [
    { "type": "archetype:in_water" },
    { "type": "not", "condition": { "type": "archetype:is_sneaking" } }
  ]
}
```

---

## Resource

Define a custom resource bar (mana, energy, blood, etc.):

```json
"resource": {
  "type": "resource.mypack.mana",
  "max": 100,
  "start": 100,
  "drain_per_second": 0.0,
  "regen_per_second": 5.0,
  "color": "44AAFF",
  "icon": "mypack:textures/gui/resource/mana.png"
}
```

Active abilities consume the resource via their `resource_cost`.

---

## Lifecycle commands

Run vanilla commands on class lifecycle events:

```json
"commands": [
  { "trigger": "on_assign",    "command": "say {player} became a warrior" },
  { "trigger": "on_level_up",  "command": "playsound minecraft:entity.player.levelup player @s" },
  { "trigger": "on_tick",      "command": "effect give @s minecraft:speed 2 0 true", "interval": 40 }
]
```

- Triggers: `on_assign`, `on_remove`, `on_tick`, `on_death`, `on_respawn`, `on_level_up`.
- `interval` (ticks) applies only to `on_tick` — default 20.
- Placeholders: `{player}` → name, `{level}` → class level (`on_level_up` only).
- Commands run with permission level 4 with the player as `@s`.

---

## Localization

Datapacks themselves cannot ship `lang/` files — that is a resource pack feature. Two options:

1. **Bundle a resource pack** alongside the datapack (recommended for distribution).
2. **Reuse existing keys** from Archetype's own lang files where it fits.

If a key has no translation, Minecraft displays the raw key — useful for catching typos.

Key conventions used by built-in classes (no enforcement, just a pattern to mirror):

- `class.<namespace>.<class_id>.name` / `.description` / `.lore_<n>`
- `ability.<namespace>.<class_id>.<ability_id>.name` / `.description`
- `passive.<namespace>.<class_id>.<passive_id>.name` / `.description`
- `progression.<namespace>.<class_id>.level_<n>`
- `resource.<namespace>.<resource_id>`

---

## Validation & errors

Class JSONs are validated at load time. Problems are reported in two tiers:

**Hard errors — class is rejected:**
- Missing required fields (`name`, `description`, `icon`, `color`).
- Invalid attribute `operation`, slot name, hex color, category, or trigger name.
- Two active abilities sharing the same `slot`.
- `resource.max <= 0`, `cooldown < 0`, `unlock_level < 0`, `commands[].interval <= 0`.

```
[archetype] Error parsing class 'mypack:warrior' at field 'active_abilities[1].slot': Slot 'ability_1' is already used by another active ability in this class
```

**Warnings — class still loads, but the offending entry is skipped at runtime:**
- Unknown `passive_abilities[].type` / `active_abilities[].type`.
- Unknown `condition.type` (the condition evaluates to `false`).

```
[archetype] Class 'mypack:warrior' field 'active_abilities[0].type': unknown active ability type 'archetype:teleporter' — this entry will be skipped at runtime.
```

The lenient handling for unknown types lets a companion mod register them later (e.g. on first server tick). After editing JSON, run `/archetype reload`.

---

## Adding new ability or condition *types* from Java

This requires a companion Fabric mod (datapacks alone cannot register Java factories).

```java
ArchetypeAPI.registerAbilityType(
    Identifier.fromNamespaceAndPath("mypack", "my_ability"),
    MyAbility::new);

ArchetypeAPI.registerPassiveType(
    Identifier.fromNamespaceAndPath("mypack", "my_passive"),
    MyPassive::new);

ArchetypeAPI.registerConditionType(
    Identifier.fromNamespaceAndPath("mypack", "my_condition"),
    MyCondition::new);
```

Call these from your mod's `init()` — before the world loads — so the parser sees them when datapacks reload.

---

## Reference: complete class file

See [common/src/main/resources/data/archetype/archetype_classes/](common/src/main/resources/data/archetype/archetype_classes/) for full real-world examples (`vi.json`, `ram.json`, `lin_qi.json`, `ru_yi.json`, `summoner.json`, `morph.json`).
