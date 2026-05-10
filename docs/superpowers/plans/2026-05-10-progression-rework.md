# Progression Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace opaque `{level, key}` progression with tagged `{level, ability, key}` entries, update GUI rendering to group by ability, and apply all tester-specified balance changes.

**Architecture:** Approach A — each milestone tagged with `ability`, multiple entries per level allowed, GUI groups by ability with section headers. Java abilities reworked inline (hardcoded level thresholds, matching existing code style).

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric API, Gson for JSON parsing.

---

## File Map

| File | Change |
|---|---|
| `common/.../core/PlayerClass.java:136` | `LevelMilestone` record: add `ability` field |
| `common/.../registry/ClassJsonParser.java:100-108` | Parse `ability` field in progression |
| `common/.../gui/ClassInfoScreen.java` | Group progression rendering by ability |
| `common/.../ability/active/ViDashAbility.java:82` | Charge thresholds: 20→15, 40→30 |
| `common/.../ability/active/ChaseTeleportAbility.java` | Remove self-damage, new damage+CD scaling, add resistance |
| `common/.../ability/active/AntigravityThrowAbility.java:34-40` | Damage: +1 at 10,20,30,40 + keep 60 |
| `common/.../ability/active/EvokerFangsAbility.java` | Remove tag-based resistance, add player resistance (Circle mode) |
| `common/.../ability/active/RandomProjectileAbility.java` | Fix arrow bug, new chances/damage |
| `common/.../ability/active/RandomTeleportAbility.java` | Remove egg, new chances, cooldown |
| `common/.../ability/active/FormShiftAbility.java` | Zombie health progression, wither per-form consumes_item |
| `common/.../data/archetype/archetype_classes/ram.json` | Damage bonuses, ability tags |
| `common/.../data/archetype/archetype_classes/vi.json` | Charge levels, ability tags |
| `common/.../data/archetype/archetype_classes/ru_yi.json` | Chase/antigravity rework, tags |
| `common/.../data/archetype/archetype_classes/summoner.json` | Fangs/CD/resistance params, tags |
| `common/.../data/archetype/archetype_classes/lin_qi.json` | Chance rework, remove attrs/passive, tags |
| `common/.../data/archetype/archetype_classes/morph.json` | All form updates, tags |
| `common/.../assets/archetype/lang/en_us.json` | New progression keys + header keys |
| `common/.../assets/archetype/lang/ru_ru.json` | Same |
| `common/.../assets/archetype/lang/de_de.json` + 5 others | Remove all `progression.archetype.*` keys |

---

## Task 1: LevelMilestone record + parser

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/core/PlayerClass.java:136`
- Modify: `common/src/main/java/com/mod/archetype/registry/ClassJsonParser.java:100-108`

- [ ] **Step 1: Update LevelMilestone record**

In `PlayerClass.java`, find and replace:
```java
public record LevelMilestone(int level, String descriptionKey) {}
```
With:
```java
public record LevelMilestone(int level, String ability, String descriptionKey) {}
```

- [ ] **Step 2: Update ClassJsonParser**

In `ClassJsonParser.java`, find the progression parsing block (~line 103-108):
```java
int level = obj.get("level").getAsInt();
String key = obj.get("key").getAsString();
progression.add(new PlayerClass.LevelMilestone(level, key));
```
Replace with:
```java
int level = obj.get("level").getAsInt();
String ability = requireString(obj, "ability", fileId, "progression[" + i + "]");
String key = obj.get("key").getAsString();
progression.add(new PlayerClass.LevelMilestone(level, ability, key));
```

- [ ] **Step 3: Fix all call-sites that construct LevelMilestone**

Search the whole project for `new PlayerClass.LevelMilestone(` or `new LevelMilestone(` — update any remaining 2-arg call sites. (There should be none after Step 2, but verify.)

```
grep -r "new PlayerClass.LevelMilestone\|new LevelMilestone" common/src
```

- [ ] **Step 4: Build to verify**

```
./gradlew :common:compileJava 2>&1 | tail -30
```
Expected: no errors (will fail at runtime until JSON files are updated, but compiles clean).

- [ ] **Step 5: Commit**

```
git add common/src/main/java/com/mod/archetype/core/PlayerClass.java
git add common/src/main/java/com/mod/archetype/registry/ClassJsonParser.java
git commit -m "feat: add ability field to LevelMilestone progression record"
```

---

## Task 2: ClassInfoScreen — grouped progression rendering

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/gui/ClassInfoScreen.java`

Context: `renderLevelTooltip` (around line 763) currently finds ONE `nextMilestone` and shows its description. With the new format, multiple entries can share the same level — they should all appear, grouped under their ability header.

- [ ] **Step 1: Rewrite the progression block in `renderLevelTooltip`**

Find this block (around lines 763-779):
```java
if (!progression.isEmpty()) {
    PlayerClass.LevelMilestone nextMilestone = null;
    for (PlayerClass.LevelMilestone milestone : progression) {
        if (milestone.level() > level) {
            nextMilestone = milestone;
            break;
        }
    }
    if (nextMilestone != null) {
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.archetype.next_level")
                .withStyle(Style.EMPTY.withColor(0xFFCC88)));
        lines.add(Component.translatable("gui.archetype.level_unlock", nextMilestone.level())
                .withStyle(Style.EMPTY.withColor(0x888888)));
        Component desc = Component.translatable(nextMilestone.descriptionKey());
        lines.add(desc.copy().withStyle(Style.EMPTY.withColor(0xAAAAAA)));
    }
}
```

Replace with:
```java
if (!progression.isEmpty()) {
    int nextLevel = -1;
    for (PlayerClass.LevelMilestone milestone : progression) {
        if (milestone.level() > level) {
            nextLevel = milestone.level();
            break;
        }
    }
    if (nextLevel >= 0) {
        lines.add(Component.empty());
        lines.add(Component.translatable("gui.archetype.next_level")
                .withStyle(Style.EMPTY.withColor(0xFFCC88)));
        lines.add(Component.translatable("gui.archetype.level_unlock", nextLevel)
                .withStyle(Style.EMPTY.withColor(0x888888)));
        String classPath = playerClass.getId().getPath();
        String lastAbility = null;
        for (PlayerClass.LevelMilestone milestone : progression) {
            if (milestone.level() != nextLevel) continue;
            if (!milestone.ability().equals(lastAbility)) {
                lines.add(Component.translatable(
                        "progression.archetype." + classPath + "." + milestone.ability() + ".header"
                ).withStyle(Style.EMPTY.withColor(0xCCCCFF)));
                lastAbility = milestone.ability();
            }
            lines.add(Component.translatable(milestone.descriptionKey())
                    .withStyle(Style.EMPTY.withColor(0xAAAAAA)));
        }
    }
}
```

- [ ] **Step 2: Find the full progression list render (if it exists)**

Search ClassInfoScreen for any other loop over `getProgression()`:
```
grep -n "getProgression\|LevelMilestone\|milestone\|progression" common/src/main/java/com/mod/archetype/gui/ClassInfoScreen.java
```

If there's a tab that shows all milestones as a flat list, update it to group by ability using the same pattern: track `lastAbility`, emit a header line when `ability` changes.

- [ ] **Step 3: Verify `playerClass.getId()` is accessible**

In ClassInfoScreen, check if `playerClass` has a `getId()` method. If not, add one to `PlayerClass.java`:
```java
public Identifier getId() { return id; }
```

- [ ] **Step 4: Build**

```
./gradlew :fabric:build 2>&1 | tail -40
```
Expected: builds (runtime will error on missing `ability` field in JSON until Task 10+, but compilation is clean).

- [ ] **Step 5: Commit**

```
git add common/src/main/java/com/mod/archetype/gui/ClassInfoScreen.java
git add common/src/main/java/com/mod/archetype/core/PlayerClass.java
git commit -m "feat: group progression tooltip by ability with section headers"
```

---

## Task 3: ViDashAbility — charge level thresholds

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/ViDashAbility.java:82`

- [ ] **Step 1: Update `computeMaxCharges`**

Find (line ~82):
```java
int extra = (classLevel >= 20 ? 1 : 0) + (classLevel >= 40 ? 1 : 0);
```
Replace with:
```java
int extra = (classLevel >= 15 ? 1 : 0) + (classLevel >= 30 ? 1 : 0);
```

- [ ] **Step 2: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/ViDashAbility.java
git commit -m "fix: Vi charge unlock at levels 15/30 (was 20/40)"
```

---

## Task 4: ChaseTeleportAbility — remove self-damage, new scaling, resistance

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/ChaseTeleportAbility.java`

New behavior:
- No self-damage ever
- Damage: base (from JSON `base_damage: 4`) + 1 per 10 levels up to 50 → total 9 at level 50
- Cooldown: base 200 ticks (10s) − 20 per level {10, 30, 50} → 7s at 50
- Resistance on activation: starts at level 20 (level 1, 2s), grows with 30 (+1s), 40 (+level 2, +1s)

- [ ] **Step 1: Rewrite the class**

Replace the entire file content:
```java
package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ChaseTeleportAbility extends AbstractActiveAbility {

    private final float baseDamage;

    public ChaseTeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.baseDamage = getFloat("base_damage", 4.0f);
    }

    private float getEffectiveDamage(int level) {
        float dmg = baseDamage;
        if (level >= 10) dmg += 1.0f;
        if (level >= 20) dmg += 1.0f;
        if (level >= 30) dmg += 1.0f;
        if (level >= 40) dmg += 1.0f;
        if (level >= 50) dmg += 1.0f;
        return dmg;
    }

    @Override
    public int getCooldownTicks(ServerPlayer player) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int cd = entry.cooldownTicks();
        if (level >= 10) cd -= 20;
        if (level >= 30) cd -= 20;
        if (level >= 50) cd -= 20;
        return Math.max(20, cd);
    }

    private void applyResistance(ServerPlayer player, int level) {
        if (level < 20) return;
        int amplifier = (level >= 40) ? 1 : 0;
        int durationTicks = 40; // 2s at level 20
        if (level >= 30) durationTicks += 20; // 3s
        if (level >= 40) durationTicks += 20; // 4s
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, amplifier, false, false, false));
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        LivingEntity target = player.getLastHurtMob();
        if (target == null || !target.isAlive()) {
            return ActivationResult.FAILED;
        }

        double distSq = player.distanceToSqr(target);
        if (distSq > 64 * 64) {
            return ActivationResult.FAILED;
        }

        Vec3 targetPos = target.position();
        Vec3 offset = player.position().subtract(targetPos).normalize();
        double teleX = targetPos.x + offset.x * 1.5;
        double teleY = targetPos.y;
        double teleZ = targetPos.z + offset.z * 1.5;
        player.teleportTo(teleX, teleY, teleZ);

        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        target.hurt(player.damageSources().playerAttack(player), getEffectiveDamage(level));
        applyResistance(player, level);

        return ActivationResult.SUCCESS;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "chase_teleport");
    }
}
```

- [ ] **Step 2: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/ChaseTeleportAbility.java
git commit -m "feat: Chase — remove self-damage, rework damage/CD/resistance scaling"
```

---

## Task 5: AntigravityThrowAbility — new damage scaling

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/AntigravityThrowAbility.java:34-40`

New: base damage comes from JSON (will be updated to 5.0). Code adds +1 at levels 10, 20, 30, 40, and keeps existing +1 at 60.

- [ ] **Step 1: Replace `getEffectiveDamage`**

Find (lines 34-39):
```java
private float getEffectiveDamage(int level) {
    float dmg = baseDamage;
    if (level >= 10) dmg += 1.0f;
    if (level >= 30) dmg += 1.0f;
    if (level >= 60) dmg += 1.0f;
    return dmg;
}
```
Replace with:
```java
private float getEffectiveDamage(int level) {
    float dmg = baseDamage;
    if (level >= 10) dmg += 1.0f;
    if (level >= 20) dmg += 1.0f;
    if (level >= 30) dmg += 1.0f;
    if (level >= 40) dmg += 1.0f;
    if (level >= 60) dmg += 1.0f;
    return dmg;
}
```

- [ ] **Step 2: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/AntigravityThrowAbility.java
git commit -m "feat: Antigravity damage +1 at levels 10/20/30/40 (keep 60)"
```

---

## Task 6: EvokerFangsAbility — remove tag resistance, add Circle player resistance

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/EvokerFangsAbility.java`

Current state: `getResistanceAmplifier()` returns a tag added to each fang entity. This mechanic is being replaced with direct player resistance on Circle (mode=3) activation only.

New resistance for Circle:
- Level 0: Resistance I (amp 0), 3s (60 ticks)
- Level 10: Resistance II (amp 1), 3s
- Level 20: Resistance II, 4s
- Level 30: Resistance III (amp 2), 4s
- Level 40: Resistance III, 5s
- Level 50: Resistance IV (amp 3), 5s
- Level 60: Resistance IV, 6s

- [ ] **Step 1: Rewrite the class**

Replace the full file:
```java
package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EvokerFangsAbility extends AbstractActiveAbility {

    private final int mode; // 1=line, 2=targeted, 3=around self
    private final int baseFangs;

    public EvokerFangsAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.mode = getInt("mode", 1);
        this.baseFangs = getInt("base_fangs", 3);
    }

    private int getFangCount(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int extra = level / 10;
        int total = baseFangs + extra;
        int maxFangs = getInt("max_fangs", mode == 3 ? 12 : 9);
        return Math.min(total, maxFangs);
    }

    private void applyCircleResistance(ServerPlayer player) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int amplifier = 0;
        if (level >= 10) amplifier = 1;
        if (level >= 30) amplifier = 2;
        if (level >= 50) amplifier = 3;
        int durationTicks = 60;
        if (level >= 20) durationTicks += 20;
        if (level >= 40) durationTicks += 20;
        if (level >= 60) durationTicks += 20;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, amplifier, false, true, true));
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        int count = getFangCount(player);
        switch (mode) {
            case 1 -> spawnFangsLine(player, count);
            case 2 -> spawnFangsTargeted(player, count);
            case 3 -> {
                spawnFangsAround(player, count);
                applyCircleResistance(player);
            }
            default -> spawnFangsLine(player, count);
        }
        return ActivationResult.SUCCESS;
    }

    private void spawnFangsLine(ServerPlayer player, int count) {
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
        Vec3 start = player.position().add(flatLook.scale(1.5));
        for (int i = 0; i < count; i++) {
            Vec3 pos = start.add(flatLook.scale(i * 1.2));
            BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(new EvokerFangs(player.level(), pos.x, ground.getY(), pos.z, 0, i * 2, player));
            }
        }
    }

    private void spawnFangsTargeted(ServerPlayer player, int count) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(20));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return;
        BlockPos targetPos = hit.getBlockPos().relative(hit.getDirection());
        double cx = targetPos.getX() + 0.5;
        double cy = targetPos.getY();
        double cz = targetPos.getZ() + 0.5;
        BlockPos centerGround = findGround(player, targetPos);
        if (centerGround != null) {
            player.level().addFreshEntity(new EvokerFangs(player.level(), cx, centerGround.getY(), cz, 0, 0, player));
        }
        if (count <= 1) return;
        int circleCount = count - 1;
        double angleStep = 2 * Math.PI / circleCount;
        double radius = 1.5;
        for (int i = 0; i < circleCount; i++) {
            double angle = angleStep * i;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            BlockPos blockPos = BlockPos.containing(x, cy, z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(new EvokerFangs(player.level(), x, ground.getY(), z, (float) angle, i + 1, player));
            }
        }
    }

    private void spawnFangsAround(ServerPlayer player, int count) {
        double angleStep = 2 * Math.PI / count;
        double radius = 2.0;
        for (int i = 0; i < count; i++) {
            double angle = angleStep * i;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            BlockPos blockPos = BlockPos.containing(x, player.getY(), z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(new EvokerFangs(player.level(), x, ground.getY(), z, (float) angle, i, player));
            }
        }
    }

    private BlockPos findGround(ServerPlayer player, BlockPos pos) {
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (!player.level().getBlockState(check).isAir()
                    && player.level().getBlockState(check.above()).isAir()) {
                return check.above();
            }
        }
        return pos;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "evoker_fangs");
    }
}
```

- [ ] **Step 2: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/EvokerFangsAbility.java
git commit -m "feat: Summoner Circle — player resistance on activation, remove tag-based mechanic"
```

---

## Task 7: RandomProjectileAbility — fix arrow, new chances + damage

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/RandomProjectileAbility.java`

Changes:
- Arrow damage: base 3 HP (use `baseDamage = 1.5` at shoot speed 2.0 → ~3 HP), +0.5 per 10 levels
- Snowball: base weight 30, reduce by 10 per tier only up to tier 3 (floor 0)
- Arrow: base weight 15, +5 per tier
- Potion: base weight 15, +5 per tier
- Arrow bug: investigate if `Arrow(level, player, stack, ItemStack.EMPTY)` fails in 1.21 — if so, try `Arrow(level, player, stack, null)`

- [ ] **Step 1: Replace `getArrowDamage`**

Find (lines 41-49):
```java
private double getArrowDamage(int classLevel) {
    double dmg = 0.5;
    if (classLevel >= 10) dmg += 0.5;
    if (classLevel >= 20) dmg += 0.5;
    if (classLevel >= 40) dmg += 0.5;
    if (classLevel >= 60) dmg += 0.5;
    return dmg;
}
```
Replace with:
```java
private double getArrowDamage(int classLevel) {
    // At shoot speed 2.0, baseDamage 1.5 ≈ 3 HP. Each 0.5 step ≈ +1 HP.
    double dmg = 1.5;
    if (classLevel >= 10) dmg += 0.5;
    if (classLevel >= 20) dmg += 0.5;
    if (classLevel >= 30) dmg += 0.5;
    if (classLevel >= 40) dmg += 0.5;
    if (classLevel >= 50) dmg += 0.5;
    return dmg;
}
```

- [ ] **Step 2: Replace chance calculation in `activate`**

Find (lines 63-70):
```java
int snowballChance = Math.max(0, 60 - levelTier * 10);
int arrowChance = 30 + levelTier * 5;
int potionChance = 10 + levelTier * 5;
int total = snowballChance + arrowChance + potionChance;
```
Replace with:
```java
// Snowball reduces only 3 tiers (→0 by level 30), arrow/potion grow each tier
int snowballChance = Math.max(0, 30 - Math.min(3, levelTier) * 10);
int arrowChance    = 15 + levelTier * 5;
int potionChance   = 15 + levelTier * 5;
int total = snowballChance + arrowChance + potionChance;
```

- [ ] **Step 3: Fix arrow spawn if needed**

In the arrow branch (around line 82), test if arrows appear in-game after other tasks are done. If not, try replacing:
```java
Arrow arrow = new Arrow(player.level(), player, new ItemStack(Items.ARROW), ItemStack.EMPTY);
```
With:
```java
Arrow arrow = new Arrow(player.level(), player, new ItemStack(Items.ARROW), null);
```
If still broken, add a debug log to confirm the branch is reached:
```java
Archetype.LOGGER.debug("RandomProjectile: spawning arrow at level {}", classLevel);
```

- [ ] **Step 4: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/RandomProjectileAbility.java
git commit -m "feat: Lin Qi Creation — new arrow damage, reworked spawn chances"
```

---

## Task 8: RandomTeleportAbility — remove egg, new chances

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/RandomTeleportAbility.java`

Changes:
- Remove egg branch entirely
- Snowball base: 70 → 50 (−20), grows with levels
- Pearl base: 25 → 50 (+25 absorbs egg's 5% + 20% extra), grows with levels
- Only 3 level tiers (cap at level 30)
- Cooldown reduction only at level 30 (remove level 60 reduction)

- [ ] **Step 1: Remove egg import**

Remove the line:
```java
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
```

- [ ] **Step 2: Rewrite `activate`**

Replace the entire `activate` method body with:
```java
@Override
public ActivationResult activate(ServerPlayer player) {
    if (!canActivate(player)) return ActivationResult.FAILED;

    int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
    // Only 3 tiers: levels 10, 20, 30
    int levelTier = Math.min(3, classLevel / 10);

    int snowballChance = Math.max(0, 50 - levelTier * 10);
    int pearlChance    = 50 + levelTier * 10;
    int total = snowballChance + pearlChance;

    int roll = random.nextInt(total);
    Vec3 look = player.getLookAngle();

    if (roll < snowballChance) {
        Snowball snowball = new Snowball(player.level(), player, new ItemStack(Items.SNOWBALL));
        snowball.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        snowball.shoot(look.x, look.y, look.z, 1.5f, 0f);
        player.level().addFreshEntity(snowball);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
    } else {
        ThrownEnderpearl pearl = new ThrownEnderpearl(player.level(), player, new ItemStack(Items.ENDER_PEARL));
        pearl.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        pearl.shoot(look.x, look.y, look.z, 1.5f, 0f);
        player.level().addFreshEntity(pearl);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
    }

    if (player.level() instanceof ServerLevel serverLevel) {
        serverLevel.sendParticles(ParticleTypes.WARPED_SPORE,
                player.getX(), player.getY(), player.getZ(),
                10, 0, 0, 0, 1.0);
    }
    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.VEX_HURT, SoundSource.AMBIENT, 100.0f, 1.2f);

    PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
    Identifier abilityId = Identifier.fromNamespaceAndPath("archetype", entry.slot());
    data.setCooldown(abilityId, computeCooldown(data.getClassLevel()));

    return ActivationResult.SUCCESS;
}
```

- [ ] **Step 3: Update `computeCooldown`**

Find:
```java
int reduction = (classLevel >= 30 ? 1 : 0) + (classLevel >= 60 ? 1 : 0);
return BASE_COOLDOWN_TICKS - (reduction * 40);
```
Replace:
```java
int reduction = (classLevel >= 30) ? 1 : 0;
return BASE_COOLDOWN_TICKS - (reduction * 40);
```

- [ ] **Step 4: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/RandomTeleportAbility.java
git commit -m "feat: Lin Qi Teleport — remove egg, new pearl/snowball chances"
```

---

## Task 9: FormShiftAbility — zombie health progression + wither consumes_item

**Files:**
- Modify: `common/src/main/java/com/mod/archetype/ability/active/FormShiftAbility.java`

Two changes:
1. **Zombie health bonus at level 50**: `getEffectiveHealthModifier` hardcodes `return 0` for zombie. Add progression-based health modifier support.
2. **Wither skeleton item not consumed**: `activate()` always calls `held.shrink(1)`. Add per-form `consumesItem` field.

- [ ] **Step 1: Add `consumesItem` to `FormDefinition`**

In the `FormDefinition` inner class, add a field after `final JsonArray progression;`:
```java
final boolean consumesItem;
```

In `FormDefinition(JsonObject json)` constructor, add after `this.progression = ...`:
```java
this.consumesItem = !json.has("consumes_item") || json.get("consumes_item").getAsBoolean();
```

- [ ] **Step 2: Use `consumesItem` in `activate()`**

Find in `activate()`:
```java
held.shrink(1);
```
Replace with:
```java
if (matchedForm.consumesItem) {
    held.shrink(1);
}
```

- [ ] **Step 3: Add `getEffectiveHealthModifier` progression support for zombie**

Find `getEffectiveHealthModifier` in `FormDefinition` (around line 483):
```java
float getEffectiveHealthModifier(int level) {
    if ("zombie".equals(formId)) {
        return 0;
    }
    return maxHealthModifier;
}
```
Replace with:
```java
float getEffectiveHealthModifier(int level) {
    if ("zombie".equals(formId)) {
        float bonus = 0;
        for (JsonElement elem : progression) {
            JsonObject p = elem.getAsJsonObject();
            if (p.has("level") && p.has("health_modifier") && level >= p.get("level").getAsInt()) {
                bonus = p.get("health_modifier").getAsFloat();
            }
        }
        return bonus;
    }
    return maxHealthModifier;
}
```

- [ ] **Step 4: Build and commit**

```
./gradlew :common:compileJava 2>&1 | tail -10
git add common/src/main/java/com/mod/archetype/ability/active/FormShiftAbility.java
git commit -m "feat: Morph — zombie health at lv50, wither skeleton item not consumed"
```

---

## Task 10: RAM JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/ram.json`

- [ ] **Step 1: Update `ability_stats` → `rage_damage` bonuses**

Change all three `"value": 1` entries under `rage_damage` bonuses to `"value": 2`:
```json
{"name": "gui.archetype.ram.stat.rage_damage", "base": 6.0, "format": "int", "bonuses": [
  {"level": 10, "value": 2},
  {"level": 20, "value": 2},
  {"level": 30, "value": 2}
]}
```

- [ ] **Step 2: Replace `progression` array**

```json
"progression": [
  {"level": 5,  "ability": "rage",    "key": "progression.archetype.ram.rage.5"},
  {"level": 10, "ability": "rage",    "key": "progression.archetype.ram.rage.10"},
  {"level": 10, "ability": "feather", "key": "progression.archetype.ram.feather.10"},
  {"level": 15, "ability": "feather", "key": "progression.archetype.ram.feather.15"},
  {"level": 20, "ability": "rage",    "key": "progression.archetype.ram.rage.20"},
  {"level": 20, "ability": "feather", "key": "progression.archetype.ram.feather.20"},
  {"level": 25, "ability": "rage",    "key": "progression.archetype.ram.rage.25"},
  {"level": 30, "ability": "rage",    "key": "progression.archetype.ram.rage.30"},
  {"level": 40, "ability": "rage",    "key": "progression.archetype.ram.rage.40"},
  {"level": 50, "ability": "rage",    "key": "progression.archetype.ram.rage.50"},
  {"level": 50, "ability": "feather", "key": "progression.archetype.ram.feather.50"}
]
```

- [ ] **Step 3: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/ram.json
git commit -m "data: RAM — rage damage +2 per milestone, progression ability tags"
```

---

## Task 11: VI JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/vi.json`

- [ ] **Step 1: Update `ability_stats` → `charges` bonuses**

```json
{"name": "gui.archetype.vi.stat.charges", "base": 1, "format": "int", "bonuses": [
  {"level": 15, "value": 1},
  {"level": 30, "value": 1}
]}
```

- [ ] **Step 2: Replace `progression` array**

```json
"progression": [
  {"level": 10, "ability": "dash", "key": "progression.archetype.vi.dash.10"},
  {"level": 15, "ability": "dash", "key": "progression.archetype.vi.dash.15"},
  {"level": 20, "ability": "dash", "key": "progression.archetype.vi.dash.20"},
  {"level": 30, "ability": "dash", "key": "progression.archetype.vi.dash.30"},
  {"level": 40, "ability": "dash", "key": "progression.archetype.vi.dash.40"},
  {"level": 50, "ability": "dash", "key": "progression.archetype.vi.dash.50"}
]
```

- [ ] **Step 3: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/vi.json
git commit -m "data: Vi — charges unlock at 15/30, progression ability tags"
```

---

## Task 12: RU_YI JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/ru_yi.json`

- [ ] **Step 1: Remove `self_damage` from Chase params**

In `active_abilities[0].params`, remove `"self_damage": 3.0`.

- [ ] **Step 2: Update Chase ability_stats**

Replace the Chase stat block:
```json
{"name": "gui.archetype.ru_yi.stat.header_chase", "base": 0, "format": "header", "bonuses": []},
{"name": "gui.archetype.ru_yi.stat.chase_damage", "base": 4, "format": "int", "bonuses": [
  {"level": 10, "value": 1},
  {"level": 20, "value": 1},
  {"level": 30, "value": 1},
  {"level": 40, "value": 1},
  {"level": 50, "value": 1}
]},
{"name": "gui.archetype.ru_yi.stat.chase_cooldown", "base": 10, "format": "seconds", "bonuses": [
  {"level": 10, "value": -1},
  {"level": 30, "value": -1},
  {"level": 50, "value": -1}
]},
{"name": "gui.archetype.ru_yi.stat.chase_resistance_level", "base": 0, "format": "int", "bonuses": [
  {"level": 20, "value": 1},
  {"level": 40, "value": 1}
]},
{"name": "gui.archetype.ru_yi.stat.chase_resistance_duration", "base": 0, "format": "seconds", "bonuses": [
  {"level": 20, "value": 2},
  {"level": 30, "value": 1},
  {"level": 40, "value": 1}
]}
```
(Remove the old `chase_self_damage` stat entirely.)

- [ ] **Step 3: Update Antigravity params and stats**

In `active_abilities[1]`:
- `"cooldown": 340` → `"cooldown": 300`
- In `params`: `"base_damage": 7.0` → `"base_damage": 5.0`

Update antigravity stats:
```json
{"name": "gui.archetype.ru_yi.stat.header_antigravity", "base": 0, "format": "header", "bonuses": []},
{"name": "gui.archetype.ru_yi.stat.antigravity_damage", "base": 5, "format": "int", "bonuses": [
  {"level": 10, "value": 1},
  {"level": 20, "value": 1},
  {"level": 30, "value": 1},
  {"level": 40, "value": 1},
  {"level": 60, "value": 1}
]},
{"name": "gui.archetype.ru_yi.stat.antigravity_cooldown", "base": 15, "format": "seconds", "bonuses": [
  {"level": 10, "value": -1},
  {"level": 20, "value": -1},
  {"level": 30, "value": -1},
  {"level": 40, "value": -1},
  {"level": 50, "value": -1},
  {"level": 60, "value": -1}
]},
{"name": "gui.archetype.ru_yi.stat.antigravity_slowness", "base": 3, "format": "int", "bonuses": [
  {"level": 10, "value": -1},
  {"level": 30, "value": -1},
  {"level": 50, "value": -1}
]}
```

- [ ] **Step 4: Replace `progression` array**

```json
"progression": [
  {"level": 10, "ability": "chase",       "key": "progression.archetype.ru_yi.chase.10"},
  {"level": 10, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.10"},
  {"level": 20, "ability": "chase",       "key": "progression.archetype.ru_yi.chase.20"},
  {"level": 20, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.20"},
  {"level": 30, "ability": "chase",       "key": "progression.archetype.ru_yi.chase.30"},
  {"level": 30, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.30"},
  {"level": 40, "ability": "chase",       "key": "progression.archetype.ru_yi.chase.40"},
  {"level": 40, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.40"},
  {"level": 50, "ability": "chase",       "key": "progression.archetype.ru_yi.chase.50"},
  {"level": 50, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.50"},
  {"level": 60, "ability": "antigravity", "key": "progression.archetype.ru_yi.antigravity.60"}
]
```

- [ ] **Step 5: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/ru_yi.json
git commit -m "data: RU_YI — chase rework, antigravity base 5/15s, progression tags"
```

---

## Task 13: Summoner JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/summoner.json`

- [ ] **Step 1: Update ability_1 (Line, mode=1)**

- `params.base_fangs`: 2 → 5
- `ability_stats` fangs (line section): `"base": 2` → `"base": 5`

- [ ] **Step 2: Update ability_2 (Target, mode=2)**

- `params.base_fangs`: 2 → 4
- `cooldown`: 100 → 120
- `ability_stats` fangs (target section): `"base": 2` → `"base": 4`
- `ability_stats` cooldown_target: `"base": 5` → `"base": 6`

- [ ] **Step 3: Update ability_3 (Circle, mode=3)**

- `cooldown`: 100 → 200
- `ability_stats` cooldown_around: `"base": 5` → `"base": 10`

Add new Circle stats after `cooldown_around`:
```json
{"name": "gui.archetype.summoner.stat.circle_resistance_level",    "base": 1, "format": "int",     "bonuses": [{"level": 10, "value": 1}, {"level": 30, "value": 1}, {"level": 50, "value": 1}]},
{"name": "gui.archetype.summoner.stat.circle_resistance_duration", "base": 3, "format": "seconds", "bonuses": [{"level": 20, "value": 1}, {"level": 40, "value": 1}, {"level": 60, "value": 1}]}
```

Remove all three `damage` stat entries (the ones with bonuses at 20 and 40 — don't change logic, just remove the display stats that mention damage progression).

- [ ] **Step 4: Replace `progression` array**

```json
"progression": [
  {"level": 10, "ability": "devour_line",   "key": "progression.archetype.summoner.devour_line.10"},
  {"level": 10, "ability": "devour_target", "key": "progression.archetype.summoner.devour_target.10"},
  {"level": 10, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.10"},
  {"level": 20, "ability": "devour_line",   "key": "progression.archetype.summoner.devour_line.20"},
  {"level": 20, "ability": "devour_target", "key": "progression.archetype.summoner.devour_target.20"},
  {"level": 20, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.20"},
  {"level": 30, "ability": "devour_line",   "key": "progression.archetype.summoner.devour_line.30"},
  {"level": 30, "ability": "devour_target", "key": "progression.archetype.summoner.devour_target.30"},
  {"level": 30, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.30"},
  {"level": 40, "ability": "devour_line",   "key": "progression.archetype.summoner.devour_line.40"},
  {"level": 40, "ability": "devour_target", "key": "progression.archetype.summoner.devour_target.40"},
  {"level": 40, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.40"},
  {"level": 50, "ability": "devour_line",   "key": "progression.archetype.summoner.devour_line.50"},
  {"level": 50, "ability": "devour_target", "key": "progression.archetype.summoner.devour_target.50"},
  {"level": 50, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.50"},
  {"level": 60, "ability": "devour_around", "key": "progression.archetype.summoner.devour_around.60"}
]
```

- [ ] **Step 5: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/summoner.json
git commit -m "data: Summoner — fangs 5/4, cooldowns, Circle resistance display, progression tags"
```

---

## Task 14: Lin Qi JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/lin_qi.json`

- [ ] **Step 1: Remove negative base attributes**

Delete these three entries from the `attributes` array (keep `scale: -0.1`):
```json
{"attribute": "minecraft:max_health",    "operation": "ADDITION",       "value": -6.0},
{"attribute": "minecraft:attack_speed",  "operation": "MULTIPLY_BASE",  "value": -0.05},
{"attribute": "minecraft:attack_damage", "operation": "MULTIPLY_BASE",  "value": -0.15}
```

- [ ] **Step 2: Remove growth passive**

Delete the entire `xp_attribute_scaling` entry from `passive_abilities`.

- [ ] **Step 3: Update Creation ability_stats**

```json
{"name": "gui.archetype.lin_qi.stat.arrow_damage",   "base": 3,  "format": "int",     "bonuses": [
  {"level": 10, "value": 1}, {"level": 20, "value": 1},
  {"level": 30, "value": 1}, {"level": 40, "value": 1}
]},
{"name": "gui.archetype.lin_qi.stat.creation_cooldown", "base": 4, "format": "seconds", "bonuses": [
  {"level": 30, "value": -1}
]},
{"name": "gui.archetype.lin_qi.stat.arrow_chance",   "base": 15, "format": "percent", "bonuses": [
  {"level": 10, "value": 5}, {"level": 20, "value": 5},
  {"level": 30, "value": 5}, {"level": 40, "value": 5}, {"level": 50, "value": 5}
]},
{"name": "gui.archetype.lin_qi.stat.potion_chance",  "base": 15, "format": "percent", "bonuses": [
  {"level": 10, "value": 5}, {"level": 20, "value": 5},
  {"level": 30, "value": 5}, {"level": 40, "value": 5}, {"level": 50, "value": 5}
]},
{"name": "gui.archetype.lin_qi.stat.snowball_chance","base": 30, "format": "percent", "bonuses": [
  {"level": 10, "value": -10}, {"level": 20, "value": -10}, {"level": 30, "value": -10}
]}
```

- [ ] **Step 4: Update Teleport ability_stats**

```json
{"name": "gui.archetype.lin_qi.stat.teleport_cooldown", "base": 9, "format": "seconds", "bonuses": [
  {"level": 30, "value": -2}
]},
{"name": "gui.archetype.lin_qi.stat.pearl_chance", "base": 50, "format": "percent", "bonuses": [
  {"level": 10, "value": 10}, {"level": 20, "value": 10}, {"level": 30, "value": 10}
]},
{"name": "gui.archetype.lin_qi.stat.snowball_chance", "base": 50, "format": "percent", "bonuses": [
  {"level": 10, "value": -10}, {"level": 20, "value": -10}, {"level": 30, "value": -10}
]}
```
(Remove any egg chance stat if present.)

- [ ] **Step 5: Replace `progression` (remove levels 40, 50, 60 from teleport; tag all)**

```json
"progression": [
  {"level": 10, "ability": "creation", "key": "progression.archetype.lin_qi.creation.10"},
  {"level": 10, "ability": "teleport", "key": "progression.archetype.lin_qi.teleport.10"},
  {"level": 20, "ability": "creation", "key": "progression.archetype.lin_qi.creation.20"},
  {"level": 20, "ability": "teleport", "key": "progression.archetype.lin_qi.teleport.20"},
  {"level": 30, "ability": "creation", "key": "progression.archetype.lin_qi.creation.30"},
  {"level": 30, "ability": "teleport", "key": "progression.archetype.lin_qi.teleport.30"},
  {"level": 40, "ability": "creation", "key": "progression.archetype.lin_qi.creation.40"},
  {"level": 50, "ability": "creation", "key": "progression.archetype.lin_qi.creation.50"}
]
```

- [ ] **Step 6: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/lin_qi.json
git commit -m "data: Lin Qi — remove negative attrs/growth, new chances, teleport levels capped at 30"
```

---

## Task 15: Morph JSON

**Files:**
- Modify: `common/src/main/resources/data/archetype/archetype_classes/morph.json`

- [ ] **Step 1: Zombie form**

In `forms[zombie]`:
- `night_attack_damage_modifier`: 0.0 (unchanged — progression adds it)
- Replace `progression` array:
```json
"progression": [
  {"level": 10, "night_vision": true,         "night_attack_damage": 0.10},
  {"level": 20, "night_attack_damage": 0.25},
  {"level": 30, "remove_sun_damage": true},
  {"level": 40, "night_attack_speed": 0.20},
  {"level": 50, "health_modifier": 4},
  {"level": 60, "night_attack_damage": 0.35}
]
```

Update `ability_stats` zombie block:
```json
{"name": "gui.archetype.morph.stat.zombie_header",       "base": 0, "format": "header",  "bonuses": []},
{"name": "gui.archetype.morph.stat.zombie_night_damage",  "base": 0, "format": "percent", "bonuses": [
  {"level": 10, "value": 10}, {"level": 20, "value": 15},
  {"level": 40, "value": 0},  {"level": 50, "value": 0}, {"level": 60, "value": 10}
]},
{"name": "gui.archetype.morph.stat.zombie_night_speed",   "base": 0, "format": "percent", "bonuses": [
  {"level": 40, "value": 20}
]},
{"name": "gui.archetype.morph.stat.zombie_health_bonus",  "base": 0, "format": "int",     "bonuses": [
  {"level": 50, "value": 4}
]},
{"name": "gui.archetype.morph.stat.zombie_night_vision",  "base": 0, "format": "boolean", "bonuses": [{"level": 10, "value": 1}]},
{"name": "gui.archetype.morph.stat.zombie_sun_burn",      "base": 1, "format": "boolean", "bonuses": [{"level": 30, "value": -1}]}
```

Note: `getEffectiveNightAttackDamage` sets the value to whatever the last matching progression entry says (it's absolute, not cumulative). The progression array above sets cumulative totals directly in each entry's `night_attack_damage` field.

- [ ] **Step 2: Creeper form**

- `on_hit_damage`: 1 → 3
- Replace `progression`:
```json
"progression": [
  {"level": 15, "on_hit_radius": 1},
  {"level": 30, "on_hit_damage": 7}
]
```
_(base 3 + 4 bonus at level 30 = 7 total)_

Update ability_stats creeper:
```json
{"name": "gui.archetype.morph.stat.creeper_radius", "base": 0, "format": "int", "bonuses": [{"level": 15, "value": 1}]},
{"name": "gui.archetype.morph.stat.creeper_damage",  "base": 3, "format": "int", "bonuses": [{"level": 30, "value": 4}]}
```

- [ ] **Step 3: Snowman form**

- `on_hit_effect_amplifier`: 0 → 2 (slowness level 3)
- `on_hit_effect_duration`: 20 → 60 (3s)
- Replace `progression`:
```json
"progression": [
  {"level_interval": 10, "on_hit_effect_duration_growth": 20}
]
```
_(+1s per 10 levels, code already supports this format in `getEffectiveOnHitEffectDuration`)_

Update ability_stats snowman:
```json
{"name": "gui.archetype.morph.stat.snowman_effect_level",    "base": 3, "format": "int",     "bonuses": []},
{"name": "gui.archetype.morph.stat.snowman_effect_duration", "base": 3, "format": "seconds", "bonuses": [
  {"level": 10, "value": 1}, {"level": 20, "value": 1}, {"level": 30, "value": 1},
  {"level": 40, "value": 1}, {"level": 50, "value": 1}, {"level": 60, "value": 1}
]}
```

- [ ] **Step 4: Blaze form**

- `on_hit_fire_duration`: 20 → 60
- Remove `fire_damage_multiplier` field entirely (was not implemented in code)
- `fall_damage_multiplier`: 2.0 → 0.5 (field kept for future implementation; note below)
- Replace `progression`:
```json
"progression": [
  {"level": 10, "on_hit_fire_duration": 80},
  {"level": 30, "on_hit_fire_duration": 100},
  {"level": 50, "on_hit_fire_duration": 120}
]
```

Update ability_stats blaze:
```json
{"name": "gui.archetype.morph.stat.blaze_fire_duration", "base": 3, "format": "seconds", "bonuses": [
  {"level": 10, "value": 1}, {"level": 30, "value": 1}, {"level": 50, "value": 1}
]}
```

> **Note:** `fall_damage_multiplier` is in JSON but not yet read by any code. Fire immunity (0.5x fall) implementation requires adding a damage handler in `ClassManager`/`FabricEventTranslator` — this is out of scope for this task. Remove `fire_damage_multiplier` from JSON; the 0.5x fall damage is deferred.

- [ ] **Step 5: Wither skeleton form**

- Add `"consumes_item": false` field to the wither_skeleton form object
- Remove `max_health_modifier: -4`
- `on_hit_effect_duration`: 40 → 80 (4s)
- `on_hit_effect_amplifier`: 0 → 1 (wither level 2)
- Replace `progression`:
```json
"progression": [
  {"level": 10, "on_hit_effect_amplifier": 2},
  {"level": 20, "on_hit_effect_duration": 100},
  {"level": 30, "on_hit_effect_amplifier": 3},
  {"level": 40, "on_hit_effect_duration": 120}
]
```

Update ability_stats wither:
```json
{"name": "gui.archetype.morph.stat.wither_effect_duration", "base": 4, "format": "seconds", "bonuses": [
  {"level": 20, "value": 1}, {"level": 40, "value": 1}
]},
{"name": "gui.archetype.morph.stat.wither_effect_level", "base": 2, "format": "int", "bonuses": [
  {"level": 10, "value": 1}, {"level": 30, "value": 1}
]}
```

- [ ] **Step 6: Replace top-level `progression` array (Morph)**

```json
"progression": [
  {"level": 10, "ability": "zombie",         "key": "progression.archetype.morph.zombie.10"},
  {"level": 10, "ability": "snowman",        "key": "progression.archetype.morph.snowman.10"},
  {"level": 10, "ability": "blaze",          "key": "progression.archetype.morph.blaze.10"},
  {"level": 10, "ability": "wither",         "key": "progression.archetype.morph.wither.10"},
  {"level": 15, "ability": "creeper",        "key": "progression.archetype.morph.creeper.15"},
  {"level": 20, "ability": "zombie",         "key": "progression.archetype.morph.zombie.20"},
  {"level": 20, "ability": "snowman",        "key": "progression.archetype.morph.snowman.20"},
  {"level": 20, "ability": "wither",         "key": "progression.archetype.morph.wither.20"},
  {"level": 30, "ability": "zombie",         "key": "progression.archetype.morph.zombie.30"},
  {"level": 30, "ability": "snowman",        "key": "progression.archetype.morph.snowman.30"},
  {"level": 30, "ability": "blaze",          "key": "progression.archetype.morph.blaze.30"},
  {"level": 30, "ability": "creeper",        "key": "progression.archetype.morph.creeper.30"},
  {"level": 30, "ability": "wither",         "key": "progression.archetype.morph.wither.30"},
  {"level": 40, "ability": "zombie",         "key": "progression.archetype.morph.zombie.40"},
  {"level": 40, "ability": "snowman",        "key": "progression.archetype.morph.snowman.40"},
  {"level": 40, "ability": "wither",         "key": "progression.archetype.morph.wither.40"},
  {"level": 50, "ability": "zombie",         "key": "progression.archetype.morph.zombie.50"},
  {"level": 50, "ability": "snowman",        "key": "progression.archetype.morph.snowman.50"},
  {"level": 50, "ability": "blaze",          "key": "progression.archetype.morph.blaze.50"},
  {"level": 60, "ability": "zombie",         "key": "progression.archetype.morph.zombie.60"},
  {"level": 60, "ability": "snowman",        "key": "progression.archetype.morph.snowman.60"}
]
```

- [ ] **Step 7: Commit**

```
git add common/src/main/resources/data/archetype/archetype_classes/morph.json
git commit -m "data: Morph — zombie/creeper/snowman/blaze/wither form rework, progression tags"
```

---

## Task 16: en_us.json — new progression keys

**Files:**
- Modify: `common/src/main/resources/assets/archetype/lang/en_us.json`

- [ ] **Step 1: Remove all old progression keys**

Delete every key matching `"progression.archetype.*"` (all old `level_N` style keys for all 6 classes).

- [ ] **Step 2: Add header keys**

```json
"progression.archetype.ram.rage.header": "Rage",
"progression.archetype.ram.feather.header": "Feather Jump",
"progression.archetype.vi.dash.header": "Dash",
"progression.archetype.ru_yi.chase.header": "Chase",
"progression.archetype.ru_yi.antigravity.header": "Antigravity",
"progression.archetype.summoner.devour_line.header": "Devour: Line",
"progression.archetype.summoner.devour_target.header": "Devour: Target",
"progression.archetype.summoner.devour_around.header": "Devour: Circle",
"progression.archetype.lin_qi.creation.header": "Creation",
"progression.archetype.lin_qi.teleport.header": "Teleport",
"progression.archetype.morph.zombie.header": "Zombie",
"progression.archetype.morph.creeper.header": "Creeper",
"progression.archetype.morph.snowman.header": "Snowman",
"progression.archetype.morph.blaze.header": "Blaze",
"progression.archetype.morph.wither.header": "Wither Skeleton"
```

- [ ] **Step 3: Add RAM milestone keys**

```json
"progression.archetype.ram.rage.5":    "Drain -1/s",
"progression.archetype.ram.rage.10":   "Damage +2, CD -1s, drain -1/s",
"progression.archetype.ram.feather.10":"Jump +1 block",
"progression.archetype.ram.feather.15":"Jump +1 block, drain -1/s",
"progression.archetype.ram.rage.20":   "Damage +2, CD -1s, drain -1/s",
"progression.archetype.ram.feather.20":"Jump +1 block",
"progression.archetype.ram.rage.25":   "Max health +2, rage cost 30→20",
"progression.archetype.ram.rage.30":   "Damage +2, CD -1s",
"progression.archetype.ram.rage.40":   "CD -1s, drain -1/s",
"progression.archetype.ram.rage.50":   "CD -1s",
"progression.archetype.ram.feather.50":"Jump +1 block"
```

- [ ] **Step 4: Add VI milestone keys**

```json
"progression.archetype.vi.dash.10": "Damage +1, +1s effects",
"progression.archetype.vi.dash.15": "+1 charge, resistance -1s CD",
"progression.archetype.vi.dash.20": "+1s effects, +1 effect level, fire trail",
"progression.archetype.vi.dash.30": "Damage +1, +1s effects, +1 resistance, -1s CD, +1 charge",
"progression.archetype.vi.dash.40": "Damage +1",
"progression.archetype.vi.dash.50": "-1s CD"
```

- [ ] **Step 5: Add RU_YI milestone keys**

```json
"progression.archetype.ru_yi.chase.10":       "Damage +1, CD -1s",
"progression.archetype.ru_yi.antigravity.10": "Damage +1, CD -1s, -1 slowness level",
"progression.archetype.ru_yi.chase.20":       "Damage +1, resistance I (2s)",
"progression.archetype.ru_yi.antigravity.20": "Damage +1, CD -1s",
"progression.archetype.ru_yi.chase.30":       "Damage +1, CD -1s, resistance +1s",
"progression.archetype.ru_yi.antigravity.30": "Damage +1, CD -1s, -1 slowness level",
"progression.archetype.ru_yi.chase.40":       "Damage +1, resistance II, +1s",
"progression.archetype.ru_yi.antigravity.40": "Damage +1, CD -1s",
"progression.archetype.ru_yi.chase.50":       "Damage +1, CD -1s",
"progression.archetype.ru_yi.antigravity.50": "CD -1s, slowness removed",
"progression.archetype.ru_yi.antigravity.60": "Damage +1, CD -1s"
```

- [ ] **Step 6: Add Summoner milestone keys**

```json
"progression.archetype.summoner.devour_line.10":   "+1 fang",
"progression.archetype.summoner.devour_target.10": "+1 fang",
"progression.archetype.summoner.devour_around.10": "+1 fang, resistance II",
"progression.archetype.summoner.devour_line.20":   "+1 fang",
"progression.archetype.summoner.devour_target.20": "+1 fang",
"progression.archetype.summoner.devour_around.20": "+1 fang, resistance +1s",
"progression.archetype.summoner.devour_line.30":   "+1 fang",
"progression.archetype.summoner.devour_target.30": "+1 fang",
"progression.archetype.summoner.devour_around.30": "+1 fang, resistance III",
"progression.archetype.summoner.devour_line.40":   "+1 fang",
"progression.archetype.summoner.devour_target.40": "+1 fang",
"progression.archetype.summoner.devour_around.40": "+1 fang, resistance +1s",
"progression.archetype.summoner.devour_line.50":   "+1 fang",
"progression.archetype.summoner.devour_target.50": "+1 fang",
"progression.archetype.summoner.devour_around.50": "+1 fang, resistance IV",
"progression.archetype.summoner.devour_around.60": "Resistance +1s"
```

- [ ] **Step 7: Add Lin Qi milestone keys**

```json
"progression.archetype.lin_qi.creation.10": "Snowball -10%%, arrow/potion +5%%, arrow damage +1",
"progression.archetype.lin_qi.teleport.10": "Snowball -10%%, pearl +10%%",
"progression.archetype.lin_qi.creation.20": "Snowball -10%%, arrow/potion +5%%, arrow damage +1",
"progression.archetype.lin_qi.teleport.20": "Snowball -10%%, pearl +10%%",
"progression.archetype.lin_qi.creation.30": "Snowball -10%%, arrow/potion +5%%, CD -1s, arrow damage +1",
"progression.archetype.lin_qi.teleport.30": "Snowball -10%%, pearl +10%%, CD -2s",
"progression.archetype.lin_qi.creation.40": "Arrow/potion +5%%, arrow damage +1",
"progression.archetype.lin_qi.creation.50": "Arrow/potion +5%%"
```

- [ ] **Step 8: Add Morph milestone keys**

```json
"progression.archetype.morph.zombie.10":  "Damage +10%% at night/caves, night vision",
"progression.archetype.morph.snowman.10": "+1s slowness duration",
"progression.archetype.morph.blaze.10":   "+1s fire on hit",
"progression.archetype.morph.wither.10":  "+1 wither level",
"progression.archetype.morph.creeper.15": "+1 explosion radius",
"progression.archetype.morph.zombie.20":  "Damage +15%% at night/caves (total 25%%)",
"progression.archetype.morph.snowman.20": "+1s slowness duration",
"progression.archetype.morph.wither.20":  "+1s wither duration",
"progression.archetype.morph.zombie.30":  "Sun immunity",
"progression.archetype.morph.snowman.30": "+1s slowness duration",
"progression.archetype.morph.blaze.30":   "+1s fire on hit",
"progression.archetype.morph.creeper.30": "+4 explosion damage",
"progression.archetype.morph.wither.30":  "+1 wither level",
"progression.archetype.morph.zombie.40":  "+20%% attack speed at night/caves",
"progression.archetype.morph.snowman.40": "+1s slowness duration",
"progression.archetype.morph.wither.40":  "+1s wither duration",
"progression.archetype.morph.zombie.50":  "+4 max health",
"progression.archetype.morph.snowman.50": "+1s slowness duration",
"progression.archetype.morph.blaze.50":   "+1s fire on hit",
"progression.archetype.morph.zombie.60":  "Damage +10%% at night/caves (total 35%%)",
"progression.archetype.morph.snowman.60": "+1s slowness duration"
```

- [ ] **Step 9: Build and commit**

```
./gradlew :fabric:build 2>&1 | tail -40
git add common/src/main/resources/assets/archetype/lang/en_us.json
git commit -m "lang: en_us — new progression keys with ability tags and headers"
```

---

## Task 17: ru_ru.json — new progression keys

**Files:**
- Modify: `common/src/main/resources/assets/archetype/lang/ru_ru.json`

- [ ] **Step 1: Remove all old progression keys**

Delete every key matching `"progression.archetype.*"`.

- [ ] **Step 2: Add all keys from Task 16 translated to Russian**

Headers:
```json
"progression.archetype.ram.rage.header":              "Ярость",
"progression.archetype.ram.feather.header":           "Перо прыжка",
"progression.archetype.vi.dash.header":               "Рывок",
"progression.archetype.ru_yi.chase.header":           "Преследование",
"progression.archetype.ru_yi.antigravity.header":     "Антигравитация",
"progression.archetype.summoner.devour_line.header":  "Пожирание: Линия",
"progression.archetype.summoner.devour_target.header":"Пожирание: Цель",
"progression.archetype.summoner.devour_around.header":"Пожирание: Круг",
"progression.archetype.lin_qi.creation.header":       "Сотворение",
"progression.archetype.lin_qi.teleport.header":       "Телепортация",
"progression.archetype.morph.zombie.header":          "Зомби",
"progression.archetype.morph.creeper.header":         "Криппер",
"progression.archetype.morph.snowman.header":         "Снеговик",
"progression.archetype.morph.blaze.header":           "Ифрит",
"progression.archetype.morph.wither.header":          "Визер-скелет"
```

Milestones: translate each key from Task 16 Steps 3–8 into Russian. Use the same key names, Russian text. (Provide translations for all keys listed in Task 16 — exact text is content work, but keys must match exactly.)

- [ ] **Step 3: Build and commit**

```
./gradlew :fabric:build 2>&1 | tail -40
git add common/src/main/resources/assets/archetype/lang/ru_ru.json
git commit -m "lang: ru_ru — new progression keys with Russian translations"
```

---

## Task 18: Remove progression from other lang files

**Files:**
- Modify: `common/src/main/resources/assets/archetype/lang/de_de.json`
- Modify: `common/src/main/resources/assets/archetype/lang/es_es.json`
- Modify: `common/src/main/resources/assets/archetype/lang/fr_fr.json`
- Modify: `common/src/main/resources/assets/archetype/lang/ja_jp.json`
- Modify: `common/src/main/resources/assets/archetype/lang/pt_br.json`
- Modify: `common/src/main/resources/assets/archetype/lang/zh_cn.json`

- [ ] **Step 1: Remove progression keys from each file**

In each of the 6 files, delete every key that starts with `"progression.archetype."`.

- [ ] **Step 2: Build and commit**

```
./gradlew :fabric:build 2>&1 | tail -40
git add common/src/main/resources/assets/archetype/lang/
git commit -m "lang: remove progression keys from non-en/ru languages"
```

---

## Self-Review Checklist

- [x] **Task 1** covers LevelMilestone record + parser
- [x] **Task 2** covers GUI grouped rendering
- [x] **Tasks 3–9** cover all Java ability changes
- [x] **Tasks 10–15** cover all 6 class JSON files
- [x] **Tasks 16–18** cover lang files
- [x] VI charges: code (Task 3) + JSON (Task 11) both updated
- [x] Chase self-damage: removed in code (Task 4) + JSON stat removed (Task 12)
- [x] EvokerFangs: old tag resistance removed, Circle resistance added (Task 6) + Summoner JSON has new cooldowns (Task 13)
- [x] Snowman: `level_interval` + `on_hit_effect_duration_growth` already supported in code — JSON-only change (Task 15)
- [x] Zombie caves: already implemented via `!canSeeSky` — JSON-only change (Task 15)
- [x] Blaze fire immunity: already in `tickBlazeForm` — JSON removes `fire_damage_multiplier` (Task 15)
- [x] Wither consumes_item: code (Task 9) + JSON (Task 15)
- [x] Lin Qi negative attrs: JSON-only removal (Task 14)
- [x] Antigravity base CD 340→300: JSON (Task 12), base_damage 7→5: JSON (Task 12), damage code (Task 5)
