# MM #7 — Реализация всех шаблонов активных способностей

## Роль

Java-разработчик Minecraft модов. Реализуешь шаблоны активных способностей.

## Контекст

Посмотри проект

Каждая активная способность:
- Создаётся фабрикой из `ActiveAbilityEntry` (содержит slot, cooldown, resourceCost, unlockLevel, JsonObject params)
- `canActivate` → проверяет условия. `activate` → выполняет действие.
- Длительные: `tickActive` каждый тик, пока `isActive()`.
- Toggle: `activate` переключает состояние.
- Заряжаемые: `activate` начинает зарядку, `onRelease` завершает.

## Задача

Реализуй ВСЕ шаблоны (каждый — класс extends `AbstractActiveAbility`).

## Шаблоны

### 1. TimedBuffAbility (`archetype:timed_buff`)
**Механика:** нажал → получил эффекты на N секунд.
**activate:** `active = true`, сохранить `remainingTicks = duration`
**tickActive:** уменьшить `remainingTicks`, если 0 → `active = false`
**onApply эффекты:** применить список `MobEffectInstance` из params
**onExpire:** снять эффекты
**Параметры:**
- `duration_ticks` (int, 200) — длительность
- `effects` (array) — каждый: `{"effect": "minecraft:resistance", "amplifier": 1}`
- `self_attributes` (array, optional) — временные модификаторы атрибутов
- `particles` (string, optional) — тип партиклов

### 2. AreaAttackAbility (`archetype:area_attack`)
**Механика:** нажал → урон всем вокруг в радиусе.
**activate:** найти все `LivingEntity` в `radius` через `AABB` + `level.getEntitiesOfClass()`, исключить себя, нанести `damage` каждому, применить `effects` на каждого, knockback
**Мгновенная** (tickActive не вызывается).
**Параметры:**
- `damage` (float, 6.0)
- `radius` (float, 5.0)
- `knockback_strength` (float, 1.0)
- `effects` (array, optional) — эффекты на целей
- `self_effects` (array, optional) — эффекты на себя
- `damage_type` (string, "minecraft:player_attack")

### 3. TargetedEffectAbility (`archetype:targeted_effect`)
**Механика:** нажал → эффект на сущность, на которую смотришь.
**activate:** raycast `player.pick(range, 0, false)` или `ProjectileUtil.getEntityHitResult()`. Если попал в LivingEntity → применить эффекты.
**Параметры:**
- `range` (float, 10.0)
- `effects` (array) — эффекты на цель
- `damage` (float, 0) — прямой урон (если > 0)
- `requires_line_of_sight` (bool, true)

### 4. ProjectileAbility (`archetype:projectile`)
**Механика:** нажал → летит снаряд.
**activate:** создать кастомную entity `ArchetypeProjectile`, задать направление, скорость, урон, эффекты. `level.addFreshEntity(projectile)`.
**Примечание:** `ArchetypeProjectile extends AbstractHurtingProjectile` или `ThrowableProjectile`. Универсальный снаряд: внешний вид (партикл или текстура), урон, эффекты, гравитация — всё из params.
**Параметры:**
- `speed` (float, 1.5)
- `damage` (float, 8.0)
- `gravity` (bool, false) — false = летит прямо, true = арка
- `effects_on_hit` (array, optional)
- `explosion_radius` (float, 0) — если > 0, взрыв при попадании (без разрушения блоков)
- `particle` (string, "minecraft:flame")
- `pierce` (int, 0) — сколько целей пробивает

### 5. DashAbility (`archetype:dash`)
**Механика:** рывок вперёд.
**activate:** `Vec3 look = player.getLookAngle()`, `player.setDeltaMovement(look.scale(dashSpeed))`, `player.hurtMarked = true` (форсировать синхронизацию), отменить урон от падения на `no_fall_ticks`.
**Параметры:**
- `dash_speed` (float, 3.0)
- `no_fall_ticks` (int, 40) — иммунитет к урону от падения после рывка
- `damage_on_hit` (float, 0) — если > 0, нанести урон первому встреченному мобу
- `trail_particle` (string, optional)

### 6. TeleportAbility (`archetype:teleport`)
**Механика:** телепортация к точке прицела.
**activate:** raycast блок `player.pick(range, 0, false)`. Если попал в блок → найти безопасную позицию (2 блока воздуха) → `player.teleportTo(x, y, z)`. Если не попал — fail.
**Параметры:**
- `range` (float, 30.0)
- `require_safe_landing` (bool, true) — проверять 2 блока воздуха
- `enderpearl_damage` (bool, false) — нанести 1 hp урона при телепорте

### 7. MorphAbility (`archetype:morph`)
**Механика:** превращение. Длительная способность.
**activate:** `active = true`, изменить размер через entity dimensions, скорость, запретить/разрешить полёт, применить визуальные эффекты. Сохранить `remainingTicks`.
**tickActive:** уменьшить `remainingTicks`, применить morph-специфичные эффекты (полёт, дыхание). Если тикер дошёл до 0 → деактивировать.
**forceDeactivate:** вернуть размер, скорость, снять полёт.
**Параметры:**
- `duration_ticks` (int, 400)
- `size_width` (float, 0.6) — ширина хитбокса
- `size_height` (float, 0.6) — высота хитбокса
- `can_fly` (bool, false)
- `flight_speed` (float, 0.05)
- `can_attack` (bool, false) — можно ли атаковать в форме
- `speed_modifier` (float, 0) — изменение скорости
- `drain_per_second` (float, 0) — расход ресурса в секунду (дополнительно к базовому cost)

### 8. ToggleAbility (`archetype:toggle`)
**Механика:** вкл/выкл.
**activate:** если `active` → деактивировать (снять эффекты, модификаторы). Если не `active` → активировать (применить).
**tickActive:** тикает пока включена. Проверять ресурс — если `drain_per_second > 0`, вычитать. Если ресурс кончился → автоматически деактивировать.
**Параметры:**
- `effects` (array) — эффекты пока включена
- `attributes` (array, optional) — модификаторы атрибутов пока включена
- `drain_per_second` (float, 0) — расход ресурса в секунду
- `particle` (string, optional)

### 9. SummonAbility (`archetype:summon`)
**Механика:** призвание существа.
**activate:** `EntityType.byString(entityType)` → создать entity → `level.addFreshEntity()`. Позиция: перед игроком. Добавить в `owner` через persistent data. Настроить AI если моб. Сохранить UUID в данные.
**Ограничение:** максимум `max_summons` существ. Если лимит — убить старейшее.
**Параметры:**
- `entity_type` (string, "minecraft:wolf") — тип сущности
- `max_summons` (int, 1)
- `duration_ticks` (int, 1200) — 0 = перманентные
- `health` (float, 20.0)
- `damage` (float, 4.0)

### 10. SelfHealAbility (`archetype:self_heal`)
**Механика:** мгновенное лечение.
**activate:** `player.heal(amount)`. Если `percent` → лечить % от maxHealth.
**Параметры:**
- `amount` (float, 0) — абсолютное лечение
- `percent` (float, 0.3) — лечение в % от макс HP (используется если amount == 0)
- `remove_effects` (string[], []) — снять негативные эффекты

### 11. ChargedAbility (`archetype:charged`)
**Механика:** зажал → копится → отпустил → эффект.
**activate:** `active = true`, `chargeLevel = 0`.
**tickActive:** `chargeLevel++`, cap at `max_charge_ticks`. Визуальная индикация (партиклы усиливаются).
**onRelease(player, chargeLevel):** рассчитать `chargePercent = chargeLevel / max_charge_ticks`. Нанести `baseDamage * (1 + chargePercent * damageMultiplier)` в радиусе или по цели. `active = false`.
**Параметры:**
- `max_charge_ticks` (int, 60) — 3 секунды макс зарядки
- `base_damage` (float, 4.0)
- `damage_multiplier` (float, 3.0) — урон при полном заряде: base * (1 + multiplier)
- `radius` (float, 0) — если > 0, area damage
- `range` (float, 5.0) — если radius == 0, targeted

### 12. BloodDrainAbility (`archetype:blood_drain`)
**Механика:** длительное высасывание HP из цели.
**activate:** raycast → найти цель → если LivingEntity → `active = true`, сохранить UUID цели, `remainingTicks = duration`.
**tickActive:** если цель жива и в `range` → `target.hurt(damage_per_tick)`, `player.heal(damage_per_tick * heal_percent)`, восполнить ресурс `resource_gain_per_tick`. Если цель умерла/далеко → деактивировать.
**Параметры:**
- `damage_per_tick` (float, 1.0)
- `heal_percent` (float, 0.5)
- `resource_gain_per_tick` (float, 2.0)
- `duration_ticks` (int, 40)
- `range` (float, 4.0)
- `undead_immune` (bool, true) — не работает на нежить

## Вспомогательное

### ArchetypeProjectile.java
Универсальный снаряд для `ProjectileAbility`:
- extends `AbstractHurtingProjectile`
- Хранит: `float damage`, `List<MobEffectInstance> effects`, `float explosionRadius`, `int pierce`, `ParticleOptions particle`
- `onHitEntity` → нанести урон, применить эффекты
- `onHitBlock` → если explosionRadius > 0 → взрыв (без разрушения)
- Рендер: партикл + trail

## Общие правила

- Каждый класс: `ability/active/<TypeName>Ability.java`
- Все extends `AbstractActiveAbility`
- Конструктор: `public XxxAbility(ActiveAbilityEntry entry) { super(entry); }`
- Параметры читай из `entry.params()` в final-поля конструктора
- Звуки: `player.level().playSound(null, player, soundEvent, SoundSource.PLAYERS, 1.0f, 1.0f)` — placeholder SoundEvent, можно из params
- Партиклы: `((ServerLevel)level).sendParticles(...)` — серверная отправка

## Формат вывода

13 файлов (12 шаблонов + ArchetypeProjectile), каждый полностью.
Обновлённый `AbilityRegistry.registerBuiltins()` с регистрацией всех фабрик.
