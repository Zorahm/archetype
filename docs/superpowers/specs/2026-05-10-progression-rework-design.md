# Progression Rework Design
_Date: 2026-05-10_

## Goal

Replace the opaque `{level, key}` progression format with tagged entries (`{level, ability, key}`) so the JSON is self-documenting — you can see which ability changes at each level without opening a lang file.

---

## 1. JSON Format

### Before
```json
"progression": [
  { "level": 10, "key": "progression.archetype.ram.level_10" },
  { "level": 20, "key": "progression.archetype.ram.level_20" }
]
```

### After
```json
"progression": [
  { "level": 10, "ability": "rage",    "key": "progression.archetype.ram.rage.10" },
  { "level": 10, "ability": "feather", "key": "progression.archetype.ram.feather.10" },
  { "level": 20, "ability": "rage",    "key": "progression.archetype.ram.rage.20" }
]
```

Rules:
- `ability` field is required on every entry.
- Multiple entries at the same level are allowed (one per ability that changes).
- Key pattern: `progression.archetype.<class>.<ability>.<level>`
- Group header key: `progression.archetype.<class>.<ability>.header` (shown in GUI above each group)
- For Morph: `ability` = form id (`zombie`, `creeper`, `snowman`, `blaze`, `wither`)
- Languages: keep `en_us` + `ru_ru` only for progression keys; remove from `de_de`, `es_es`, `fr_fr`, `ja_jp`, `pt_br`, `zh_cn`.

---

## 2. Java Changes

### `PlayerClass.LevelMilestone`
```java
// Before
public record LevelMilestone(int level, String descriptionKey) {}

// After
public record LevelMilestone(int level, String ability, String descriptionKey) {}
```

### `ClassJsonParser`
Parse the required `ability` field from each progression entry:
```java
String ability = requireString(obj, "ability", fileId, "progression[" + i + "]");
progression.add(new LevelMilestone(level, ability, key));
```

### `ClassInfoScreen.renderLevelTooltip`
Current: finds the single next milestone, shows one description line.
New: finds all entries at the next level threshold, groups them by `ability`, shows ability header + description for each. Example tooltip:

```
Ур. 20
  [Ярость]  +2 урон, -1с КД
  [Прыжок]  +1 блок
```

The ability header is resolved via `Component.translatable("progression.archetype.<class>.<ability>.header")`.

### Progression tab (full list)
All milestones rendered grouped by ability, with a section header per ability, entries sorted by level within each group.

---

## 3. Data Changes Per Class

### RAM

**ability_stats — `rage_damage`:**
- Bonuses: `[{10,+1},{20,+1},{30,+1}]` → `[{10,+2},{20,+2},{30,+2}]`
- Result: base 6 + bonus 6 = **12 HP** (was 9)

**active_abilities params — `knockback_scale`:** unchanged.

**Progression entries:** retag all with `ability: "rage"` or `ability: "feather"` or `ability: "drain"` / `ability: "cooldown"` as appropriate. Rename keys.

---

### VI

**ability_stats — `charges`:**
- Bonuses: `[{20,+1},{40,+1}]` → `[{15,+1},{30,+1}]`

**Active ability params:** check if `charges` is read as static or level-indexed; if static, extend to level-indexed array (needs code investigation at implementation time).

**Progression entries:** retag with `ability: "dash"`. Remove any references to charge levels 20/40.

---

### RU_YI

#### Chase (ability_1)
**Remove:**
- `self_damage` param from active_ability params
- `chase_self_damage` stat from ability_stats
- Level 60 progression entry

**ability_stats — `chase_damage`:**
- Base: 4. Bonuses: `+1` at levels 10, 20, 30, 40, 50 → total 9

**ability_stats — `chase_cooldown`:**
- Base: 10s. Bonuses: `-1s` at levels 10, 30, 50 → total 7s

**New ability_stats — `chase_resistance_level`:**
- Base: 0 (no resistance). Bonuses: `+1` at 20, `+1` at 40 → final 2

**New ability_stats — `chase_resistance_duration`:**
- Base: 0s. Bonuses: `+2s` at 20, `+1s` at 30, `+1s` at 40 → final 4s

**Active ability params — new fields (level-indexed):**
```json
"resistance_level": [
  {"level": 0,  "value": 0},
  {"level": 20, "value": 1},
  {"level": 40, "value": 2}
],
"resistance_duration_ticks": [
  {"level": 0,  "value": 0},
  {"level": 20, "value": 40},
  {"level": 30, "value": 60},
  {"level": 40, "value": 80}
]
```

**Code — `ChaseTeleportAbility`:**
- Remove self-damage logic
- On ability use: apply Resistance effect for `resistance_duration_ticks` at `resistance_level` (both from params, level-indexed)

**Progression entries:** 10/20/30/40/50 tagged `ability: "chase"`.

#### Antigravity (ability_2)
**Active ability params:**
- `cooldown` (or base in JSON level 0 entry): 340 ticks → **300 ticks** (15s)
- `base_damage`: 7 → **5**

**ability_stats — `antigravity_damage`:**
- Base: 5. Bonuses: `+1` at 10, 20, 30, 40 and keep existing 60 bonus → total 10 at level 60

**ability_stats — `antigravity_cooldown`:**
- Base: **15s**. Bonuses: adjust if needed to match new base.

**ability_stats — `antigravity_slowness`:** unchanged.

**Progression entries:** retag with `ability: "antigravity"`.

---

### Summoner

**Progression text:** remove all damage mentions from `en_us`/`ru_ru` progression keys; logic in code unchanged.

**ability_1 (Line, mode=1):**
- `base_fangs`: 2 → **5**
- ability_stats `fangs` (line): base 2 → 5

**ability_2 (Target, mode=2):**
- `base_fangs`: 2 → **4**
- `cooldown`: 100 → **120 ticks** (6s, was 5s)
- ability_stats `fangs` (target): base 2 → 4
- ability_stats `cooldown_target`: base 5s → 6s

**ability_3 (Circle, mode=3):**
- `cooldown`: 100 → **200 ticks** (10s, was 5s)
- ability_stats `cooldown_around`: base 5s → 10s

**New Circle params — resistance (level-indexed):**
```json
"resistance_amplifier": [
  {"level": 0,  "value": 0},
  {"level": 10, "value": 1},
  {"level": 30, "value": 2},
  {"level": 50, "value": 3}
],
"resistance_duration_ticks": [
  {"level": 0,  "value": 60},
  {"level": 20, "value": 80},
  {"level": 40, "value": 100},
  {"level": 60, "value": 120}
]
```
_(resistance level 1→2→3→4 maps to amplifier 0→1→2→3; duration 3s→4s→5s→6s)_

**New ability_stats for Circle:**
- `resistance_level`: base 1, bonuses at 10(+1), 30(+1), 50(+1)
- `resistance_duration`: base 3s, bonuses at 20(+1s), 40(+1s), 60(+1s)

**Code — `EvokerFangsAbility` (mode=3):**
- On activation: apply Resistance effect using level-indexed params above.

**Progression entries:** retag `ability: "devour_line"`, `"devour_target"`, `"devour_around"`.

---

### Lin Qi

#### Creation (ability_1 — random_projectile)

**ability_stats:**
- `arrow_damage`: base 1 → **3**
- `snowball_chance`: base 60 → **30**, bonuses -10% only at 10, 20, 30 (remove 40, 50 entries)
- `arrow_chance`: base 30 → **15**
- `potion_chance`: base 10 → **15**
- Check if pearl chance appears in creation stats — if so, remove (it belongs to teleport only)

**Code — `RandomProjectileAbility`:**
- Investigate and fix bug: arrows not spawning, only potions

#### Teleport (ability_2 — random_teleport)

**ability_stats:**
- Remove egg spawn chance stat entirely (if present)
- `snowball_chance`: base -20% (and shift bonuses accordingly)
- `pearl_chance`: base +20% (absorb egg's percentage)
- Remove ability_stats entries for levels 40, 50, 60

**Progression:** remove level 40, 50, 60 entries entirely.

**Code — `RandomTeleportAbility`:**
- Remove egg spawn logic; redistribute probability to ender pearl

#### Growth passive (xp_attribute_scaling)

**Current behavior:** attributes are taken away at class assign; restored gradually up to level 60.
**New behavior:** attributes are NOT taken away at assign; passive gives no penalty at level 0.

**JSON — `lin_qi.json` base attributes:** remove the four negative attribute entries (`max_health -6`, `attack_speed -0.05`, `attack_damage -0.15`, `scale -0.1`). Lin Qi starts with full base stats.

**Code/passive — `xp_attribute_scaling`:** the growth passive exists only to restore what was taken. Once the penalties are removed, the passive can be removed from lin_qi entirely (or kept to give small late-game bonuses — decide at implementation time).

**Progression entries:** retag `ability: "creation"`, `"teleport"`.

---

### Morph

#### Zombie form

**JSON changes in `forms[zombie]`:**
- `night_attack_damage_modifier` progression rework:
  - Level 10: +0.10 (night damage)
  - Level 20: +0.15 (cumulative 0.25)
  - Level 30: sun immunity (move from level 50)
  - Level 40: `night_attack_speed: 0.20`
  - Level 50: `max_health_modifier: +4` (new field)
  - Level 60: `night_attack_damage: 0.10` (cumulative 0.35)
- Remove `remove_sun_damage` at level 50; move to level 30

**Code — `FormShiftAbility` zombie:**
- Night/cave detection: condition is `isNight || !player.level().canSeeSky(player.blockPosition())`
- Support `max_health_modifier` at level 50 (progressive health bonus)

**ability_stats:** update all zombie stats to match new progression.

#### Creeper form

**JSON changes in `forms[creeper]`:**
- `on_hit_damage`: 1 → **3** (base +2 HP)
- Level 15: `on_hit_radius: 1` (was level 30)
- Level 30: `on_hit_damage: +4` (was level 60: +2, doubled and moved)

**ability_stats:**
- `creeper_damage`: base 1 → 3; bonus at 30 → +4 (was at 60 +1)
- `creeper_radius`: bonus at 15 (was 30)

#### Snowman form

**JSON changes in `forms[snowman]`:**
- `on_hit_effect_amplifier`: 0 → **2** (slowness level 3)
- `on_hit_effect_duration`: 20 → **60 ticks** (3s)
- Progression: replace `level_interval` amplifier growth with duration growth:
  ```json
  "progression": [
    {"level": 10, "on_hit_effect_duration_growth": 20},
    {"level": 20, "on_hit_effect_duration_growth": 20},
    {"level": 30, "on_hit_effect_duration_growth": 20},
    {"level": 40, "on_hit_effect_duration_growth": 20},
    {"level": 50, "on_hit_effect_duration_growth": 20},
    {"level": 60, "on_hit_effect_duration_growth": 20}
  ]
  ```
  Final duration at level 60: 3s + 6s = **9s**.

**Code — `FormShiftAbility`:** add support for `on_hit_effect_duration_growth` progression field (cumulative duration bonus per milestone).

**ability_stats:**
- `snowman_effect_level`: base 3 (was 1), no bonuses
- `snowman_effect_duration`: base 3s, bonuses +1s at 10,20,30,40,50,60

#### Blaze form

**JSON changes in `forms[blaze]`:**
- `on_hit_fire_duration`: 20 → **60 ticks** (3s base). Progression values shift +40 each: 40→80, 60→100, 80→120 (= 4s, 5s, 6s at levels 10, 30, 50)
- Remove `fire_damage_multiplier: 2.0`
- Add `fire_immune: true`
- `fall_damage_multiplier`: 2.0 → **0.5**

**Code — `FormShiftAbility` blaze:**
- Support `fire_immune: true` form field (grant fire resistance effect while in form, or cancel fire damage)
- Support `fall_damage_multiplier < 1.0` (already exists, just verify)

**ability_stats:** update `blaze_fire_duration` base to 3s, bonuses at 10/30/50 for +1s each.

#### Wither Skeleton form

**JSON changes in `forms[wither_skeleton]`:**
- Add `consumes_item: false` override at form level (item is NOT consumed on use)
- Remove `max_health_modifier: -4`
- `on_hit_effect_duration`: 40 → **80 ticks** (4s)
- `on_hit_effect_amplifier`: 0 → **1** (wither level 2)
- New progression (replace old entirely):
  ```json
  "progression": [
    {"level": 10, "on_hit_effect_amplifier": 2},
    {"level": 20, "on_hit_effect_duration": 100},
    {"level": 30, "on_hit_effect_amplifier": 3},
    {"level": 40, "on_hit_effect_duration": 120}
  ]
  ```
  _(wither 2→3→4; duration 4s→5s→6s)_

**Code — `FormShiftAbility`:**
- Support per-form `consumes_item` override (currently global on top-level params)

**ability_stats:**
- `wither_effect_level`: base 2, bonuses at 10(+1), 30(+1) → final 4
- `wither_effect_duration`: base 4s, bonuses at 20(+1s), 40(+1s) → final 6s

---

## 4. Localization

| File | Action |
|---|---|
| `en_us.json` | Rename all `progression.archetype.*` keys to new pattern; add `*.header` keys; update text per tester spec |
| `ru_ru.json` | Same |
| `de_de`, `es_es`, `fr_fr`, `ja_jp`, `pt_br`, `zh_cn` | Remove all `progression.archetype.*` entries |

---

## 5. Code Changes Summary

| File | Change |
|---|---|
| `PlayerClass.java` | `LevelMilestone`: add `ability` field |
| `ClassJsonParser.java` | Parse `ability` field in progression |
| `ClassInfoScreen.java` | Group progression rendering by `ability` |
| `ChaseTeleportAbility.java` | Remove self-damage; add resistance effect |
| `EvokerFangsAbility.java` | mode=3: add resistance on activation |
| `RandomProjectileAbility.java` | Fix arrow bug; update chance params |
| `RandomTeleportAbility.java` | Remove egg spawn; update pearl/snowball chances |
| `lin_qi.json` base attributes | Remove negative entries for health/atk_speed/atk_damage (keep scale); remove growth passive entry |
| `FormShiftAbility.java` | Zombie caves, snowman duration_growth, blaze fire_immune, wither per-form consumes_item |

---

## 6. Implementation Phases

1. **Architecture** — `LevelMilestone` + parser + GUI rendering
2. **JSON retag** — add `ability` field + rename keys in all 6 class files + update lang files
3. **Data-only class changes** — RAM, VI, RU_YI antigravity (no new code needed)
4. **Code changes** — Chase resistance, EvokerFangs resistance, Lin Qi bugs, FormShiftAbility extensions
5. **Morph forms** — JSON data + form-specific code extensions
