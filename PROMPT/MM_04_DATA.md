# MM #4 — PlayerClassData и сериализация

## Роль

Java-разработчик Minecraft модов. Пишешь модуль хранения данных игрока.

## Контекст

Посмотри проект

Мод **Archetype** — система классов для MC 1.20.1. Данные каждого игрока хранятся в `PlayerClassData` и сериализуются в NBT (сохранение) и `FriendlyByteBuf` (сеть).

## Задача

Реализуй `data/PlayerClassData.java` — полный класс данных игрока.

## Поля

```java
public class PlayerClassData {
    @Nullable ResourceLocation currentClassId;     // ID текущего класса
    long classAssignedTime;                         // тик-время назначения
    int classLevel;                                 // 1–maxLevel из конфига
    int classExperience;                            // опыт до следующего уровня
    float resourceCurrent;                          // текущее значение ресурса
    Map<ResourceLocation, Integer> cooldowns;       // ability_id → оставшиеся тики
    Set<Integer> activeConditionalSets;             // индексы активных условных наборов
    Map<ResourceLocation, Boolean> toggleStates;    // toggle_ability_id → вкл/выкл
    long lastClassChangeTime;                       // для кулдауна смены класса
    Set<ResourceLocation> triedClasses;             // классы, которые игрок пробовал (для адвансментов)
}
```

## Методы

### NBT сериализация (для сохранения мира)

```java
CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    if (currentClassId != null) tag.putString("ClassId", currentClassId.toString());
    tag.putLong("AssignedTime", classAssignedTime);
    tag.putInt("Level", classLevel);
    tag.putInt("Experience", classExperience);
    tag.putFloat("Resource", resourceCurrent);
    tag.putLong("LastChangeTime", lastClassChangeTime);
    
    // Cooldowns → CompoundTag
    CompoundTag cdTag = new CompoundTag();
    cooldowns.forEach((id, ticks) -> cdTag.putInt(id.toString(), ticks));
    tag.put("Cooldowns", cdTag);
    
    // ToggleStates → CompoundTag
    CompoundTag toggleTag = new CompoundTag();
    toggleStates.forEach((id, state) -> toggleTag.putBoolean(id.toString(), state));
    tag.put("Toggles", toggleTag);
    
    // TriedClasses → ListTag of StringTag
    ListTag triedTag = new ListTag();
    triedClasses.forEach(id -> triedTag.add(StringTag.valueOf(id.toString())));
    tag.put("TriedClasses", triedTag);
    
    // ActiveConditionalSets → IntArrayTag
    tag.putIntArray("ActiveCondSets", activeConditionalSets.stream().mapToInt(i->i).toArray());
    
    return tag;
}
```

```java
static PlayerClassData load(CompoundTag tag) { ... }
// Обратная операция. Все поля опциональны — если отсутствуют, используй значения по умолчанию.
// Парсинг ResourceLocation обернуть в try-catch (может быть невалидный при обновлении мода).
```

### Сетевая сериализация (для SyncClassDataPacket)

Только динамические данные, НЕ описание класса:
```java
void writeSyncData(FriendlyByteBuf buf) {
    buf.writeBoolean(currentClassId != null);
    if (currentClassId != null) {
        buf.writeResourceLocation(currentClassId);
        buf.writeVarInt(classLevel);
        buf.writeVarInt(classExperience);
        buf.writeFloat(resourceCurrent);
        // ... cooldowns, toggles
    }
}

static PlayerClassData readSyncData(FriendlyByteBuf buf) { ... }
```

### Утилиты

```java
boolean hasClass();
boolean isOnCooldown(ResourceLocation abilityId);
int getRemainingCooldown(ResourceLocation abilityId);
void setCooldown(ResourceLocation abilityId, int ticks);
void tickCooldowns();  // уменьшить все на 1, удалить нулевые
void addExperience(int amount, int maxLevel, int[] expTable);
boolean canChangeClass(long currentTime, long cooldownTicks);
void reset();  // полный сброс всех данных
PlayerClassData copy();  // глубокая копия
```

### Таблица опыта

```java
// Формула: expForLevel(n) = baseExp * n^1.5
// Примерные значения при baseExp = 100:
// Lvl 2: 100,  Lvl 3: 245,  Lvl 5: 559,  Lvl 10: 1581, Lvl 20: 4472
static int experienceForLevel(int level, int baseExp) {
    return (int)(baseExp * Math.pow(level - 1, 1.5));
}
```

## Формат вывода

Один файл `PlayerClassData.java`, полностью, с Javadoc на публичных методах.

## Чего НЕ делать

- Не привязывай к Capability/Attachment — это платформенный код
- Не добавляй логику ClassManager — только данные и сериализация
