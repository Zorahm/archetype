# MM #6 — Реализация всех пассивных способностей

## Роль

Java-разработчик Minecraft модов. Реализуешь конкретные типы пассивных способностей.

## Контекст

Посмотри проект

Каждая пассивная способность:
- Создаётся фабрикой из `JsonObject params`
- Имеет `onApply(player)`, `tick(player)`, `onRemove(player)` + хуки событий
- `tick()` вызывается раз в 20 тиков (раз в секунду)
- Параметры читаются из JSON через хелперы `getFloat("key", default)` и т.д.

## Задача

Реализуй ВСЕ встроенные типы пассивных способностей (каждый — отдельный класс, extends `AbstractPassiveAbility`).

## Типы для реализации

### 1. SunDamagePassive (`archetype:sun_damage`)
- `tick`: если игрок под открытым небом (`level.canSeeSky(pos)`) и время дня 0–12000 и нет шлема (если `ignore_if_helmet`) → `player.hurt(damageSources.onFire(), damagePerSecond)`, поджечь на `set_fire_ticks`
- Параметры: `damage_per_second` (float), `ignore_if_helmet` (bool, true), `ignore_if_underground` (bool, true), `set_fire_ticks` (int, 60)

### 2. NightVisionPassive (`archetype:night_vision`)
- `tick`: если условие выполнено (или всегда) → применить `MobEffects.NIGHT_VISION` на 400 тиков с `amplifier`
- `onRemove`: снять эффект
- Эффект задаётся с `ambient=true, showParticles=false, showIcon=false` (скрытый)
- Параметры: `amplifier` (int, 0)

### 3. FoodRestrictionPassive (`archetype:food_restriction`)
- `onPlayerEat`: если еда не в `allowed_foods` и не имеет тег `tag_allowed` → наложить негативные эффекты (`MobEffects.POISON` на 5 сек, `MobEffects.HUNGER` на 10 сек)
- Параметры: `allowed_foods` (string[], []), `tag_allowed` (string, nullable), `poison_duration` (int, 100), `hunger_duration` (int, 200)

### 4. NaturalRegenDisabledPassive (`archetype:natural_regeneration_disabled`)
- `tick`: если у игрока есть `MobEffects.REGENERATION` от natural regen → снять
- Альтернативный подход: выставить gamerule? Нет — это на весь сервер. Лучше: `onPlayerHurt` → ставить тег, `tick` → если тег и HP < maxHP → не лечить. Но надёжнее через `LivingHealEvent` (Forge) / mixin (Fabric).
- **Реализация**: проверять в tick, если `player.getFoodData().getFoodLevel() >= 18` и HP < max → `player.hurt(...)` на 0 чтобы сбросить regen timer. Или проще: давать скрытый `MobEffects.HUNGER` (но мешает), лучше — оставить TODO для хука через EventTranslator.
- Параметры: нет

### 5. UndeadTypePassive (`archetype:undead_type`)
- `onPlayerHurt`: если урон от `DamageTypes.MAGIC` из зелья лечения → умножить (зелья лечения наносят урон)
- Enchantment Smite: увеличить получаемый урон при наличии Smite на оружии атакующего
- Параметры: `smite_vulnerability` (bool, true), `healing_potions_damage` (bool, true), `damage_potions_heal` (bool, true)

### 6. WaterVulnerabilityPassive (`archetype:water_vulnerability`)
- `tick`: если `player.isInWater()` → урон. Если `player.isInWaterRainOrBubble()` и `rain_damage` → урон от дождя
- Параметры: `damage_per_second` (float), `rain_damage` (bool, false), `rain_damage_per_second` (float, 0.5)

### 7. SinkInWaterPassive (`archetype:sink_in_water`)
- `tick`: если `player.isInWater()` → `player.setDeltaMovement(motion.x, -0.04, motion.z)` (тонет)
- Опционально: скорость ходьбы по дну → применить attribute modifier на `MOVEMENT_SPEED` когда в воде
- Параметры: `cannot_swim` (bool, true), `walk_on_bottom` (bool, true), `walk_speed_underwater` (float, 0.8)

### 8. NoFallDamagePassive (`archetype:no_fall_damage`)
- `onPlayerHurt`: если `source.is(DamageTypes.FALL)` → отменить (return через cancellation или выставить 0)
- Лучше: через EventTranslator хук `onLivingHurt` с cancel. Оставить метод `shouldCancelDamage(source) → bool`, ClassManager проверяет его.
- Параметры: нет

### 9. FireImmunityPassive (`archetype:fire_immunity`)
- `tick`: `player.clearFire()`, давать `MobEffects.FIRE_RESISTANCE` скрытый
- `onPlayerHurt`: если `source.is(DamageTypeTags.IS_FIRE)` → cancel
- Параметры: нет

### 10. CustomDietPassive (`archetype:custom_diet`)
- Заменяет еду: обычная еда не работает, работают только указанные предметы
- `onPlayerEat`: если предмет в `food_items` → восстановить `food_value` и `saturation`. Если нет → применить негативный эффект.
- Параметры: `food_items` (string[]), `food_value` (int, 6), `saturation` (float, 0.6)

### 11. MobNeutralPassive (`archetype:mob_neutral`)
- `tick`: для каждого моба вокруг (радиус 16) из `mob_types` → если таргетит игрока → снять таргет
- Параметры: `mob_types` (string[], ["minecraft:zombie", "minecraft:skeleton"...]), `radius` (float, 16), `always_friendly` (bool, false)

### 12. MagneticPullPassive (`archetype:magnetic_pull`)
- `tick`: все `ItemEntity` в радиусе → притянуть к игроку (`item.setDeltaMovement(dirToPlayer * speed)`)
- Параметры: `item_pull_radius` (float, 8.0), `item_pull_speed` (float, 0.05)

### 13. ThornsPassive (`archetype:thorns_passive`)
- `onPlayerHurt`: если атакующий — LivingEntity → нанести ему `amount * reflect_percent` урона
- Параметры: `reflect_percent` (float, 0.15), `reflect_type` (string, "thorns" — тип урона)

### 14. LifestealPassive (`archetype:lifesteal`)
- `onPlayerAttack`: если цель — LivingEntity → `player.heal(damageDealt * percent_of_damage)`
- Параметры: `percent_of_damage` (float, 0.2), `only_melee` (bool, true)

### 15. WallClimbPassive (`archetype:wall_climb`)
- `tick`: если игрок прижат к стене (horizontalCollision) и зажат Shift → `player.setDeltaMovement(mx, climbSpeed, mz)`, отменить падение
- Параметры: `climb_speed` (float, 0.2)

### 16. SlowFallPassive (`archetype:slow_fall`)
- `tick`: если `player.getDeltaMovement().y < 0` → `setDeltaMovement(mx, max(my, -fallSpeed), mz)`
- Параметры: `fall_speed` (float, 0.05)

### 17. JumpBoostPassive (`archetype:jump_boost`)
- `onApply`: добавить attribute modifier на `ForgeMod.JUMP_BOOST` или скрытый эффект `MobEffects.JUMP`
- `onRemove`: убрать
- Параметры: `multiplier` (float, 0.5)

### 18. BreathUnderwaterPassive (`archetype:breath_underwater`)
- `tick`: `player.setAirSupply(player.getMaxAirSupply())`
- Параметры: нет

### 19. EffectImmunityPassive (`archetype:effect_immunity`)
- `tick`: для каждого эффекта из `effects` → если у игрока есть → снять
- Параметры: `effects` (string[], ["minecraft:poison", "minecraft:wither"])

## Общие правила

- Каждый класс в отдельном файле: `ability/passive/<TypeName>Passive.java`
- Все extends `AbstractPassiveAbility`
- Конструктор: `public XxxPassive(JsonObject params) { super(params); }`
- Считывай параметры в конструкторе в final-поля
- `getType()` → `new ResourceLocation("archetype", "xxx")`
- `isPositive()` — hardcoded true/false в зависимости от типа
- Не делай проверку на `level.isClientSide` — пассивки тикают только на сервере

## Формат вывода

19 файлов, каждый полностью. Порядок: по номерам выше.

Также: обновлённый `AbilityRegistry.registerBuiltins()` с регистрацией всех фабрик:
```java
registerPassive(new ResourceLocation("archetype", "sun_damage"), SunDamagePassive::new);
// ...
```
