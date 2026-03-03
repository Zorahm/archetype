# MM #8 — Реализация всех типов условий

## Роль

Java-разработчик. Реализуешь конкретные типы условий для системы классов.

## Контекст

Посмотри проект

Условия используются в:
- Условных атрибутах (`conditional_attributes`) — проверяются раз в 20 тиков
- Пассивных способностях — условие активации (например, ночное зрение только ночью)
- Активных способностях — `canActivate` может включать кастомное условие

## Задача

Реализуй ВСЕ встроенные типы условий.

## Типы

### 1. TimeOfDayCondition (`archetype:time_of_day`)
- `test`: `level.getDayTime() % 24000` в диапазоне `[from, to]`. Если `from > to` → переход через полночь (13000–23000 = ночь).
- **Params:** `range` (int[], [from, to])

### 2. HealthBelowPercentCondition (`archetype:health_below_percent`)
- `test`: `player.getHealth() / player.getMaxHealth() < percent`
- **Params:** `percent` (float, 0.3)

### 3. HealthAbovePercentCondition (`archetype:health_above_percent`)
- `test`: `player.getHealth() / player.getMaxHealth() > percent`
- **Params:** `percent` (float, 0.8)

### 4. InWaterCondition (`archetype:in_water`)
- `test`: `player.isInWater()`
- **Params:** нет

### 5. UnderwaterCondition (`archetype:underwater`)
- `test`: `player.isUnderWater()` (голова под водой)
- **Params:** нет

### 6. UnderOpenSkyCondition (`archetype:under_open_sky`)
- `test`: `player.level().canSeeSky(player.blockPosition())`
- **Params:** нет

### 7. InDimensionCondition (`archetype:in_dimension`)
- `test`: `player.level().dimension().location().equals(dimensionId)`
- **Params:** `dimension` (string, "minecraft:the_nether")

### 8. InBiomeTagCondition (`archetype:in_biome_tag`)
- `test`: получить биом позиции → проверить через `biome.is(tagKey)`
- **Params:** `tag` (string, "minecraft:is_ocean")

### 9. IsSneakingCondition (`archetype:is_sneaking`)
- `test`: `player.isShiftKeyDown()`
- **Params:** нет

### 10. IsSprintingCondition (`archetype:is_sprinting`)
- `test`: `player.isSprinting()`
- **Params:** нет

### 11. OnFireCondition (`archetype:on_fire`)
- `test`: `player.isOnFire()`
- **Params:** нет

### 12. HasItemCondition (`archetype:has_item`)
- `test`: проверить наличие предмета в указанном слоте или в любом слоте инвентаря
- Если `slot` = "mainhand" → `player.getMainHandItem()`. "offhand" → `player.getOffhandItem()`. "armor_head/chest/legs/feet". "any" → весь инвентарь.
- **Params:** `item` (string, "minecraft:diamond_sword"), `slot` (string, "any")

## Общие правила

- Каждый класс: `condition/types/<TypeName>Condition.java`
- Реализует `Condition`
- Конструктор: `(JsonObject params)` — читает параметры
- `getType()` → `new ResourceLocation("archetype", "xxx")`
- Все проверки null-safe

## Формат вывода

12 файлов + обновлённый `ConditionRegistry.registerBuiltins()` с регистрацией всех фабрик.
