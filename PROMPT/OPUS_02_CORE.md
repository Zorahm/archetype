# OPUS #2 — Ядро: ClassManager, Ability System, Conditions

## Роль

Ты — senior Minecraft mod developer. Пишешь ядро системы классов — интерфейсы, менеджеры, фабрики. Код должен быть расширяемым (новые типы через API), производительным (100+ игроков), и корректным (edge cases обработаны).

## Контекст из предыдущих шагов

Посмотри проект

## Задача

Реализуй три модуля в `common`:
1. **core/** — ClassManager (назначение/снятие/тик классов)
2. **ability/** — интерфейсы и фабрики способностей (пассивные + активные)
3. **condition/** — система условий с комбинаторами

Также: модель данных `PlayerClass` (описание класса, загруженное из JSON).

## Детальные требования

### Модуль core/

#### PlayerClass.java — описание класса (immutable, загружается из JSON)

```java
public final class PlayerClass {
    ResourceLocation id;
    String nameKey;           // ключ перевода
    String descriptionKey;
    ResourceLocation icon;    // путь к текстуре
    int color;                // HEX цвет
    ClassCategory category;   // DAMAGE, TANK, MOBILITY, UTILITY
    List<String> loreKeys;
    
    // Модификаторы
    List<AttributeModifierEntry> attributes;
    List<ConditionalAttributeEntry> conditionalAttributes;
    
    // Способности
    List<PassiveAbilityEntry> passiveAbilities;
    List<ActiveAbilityEntry> activeAbilities;
    
    // Ресурс (nullable)
    @Nullable ResourceDefinition resource;
    
    // Дополнительное
    @Nullable Float sizeModifier;
    List<ResourceLocation> incompatibleWith;
}
```

Вложенные record-ы:
- `AttributeModifierEntry(ResourceLocation attribute, AttributeModifier.Operation operation, double value)`
- `ConditionalAttributeEntry(ConditionDefinition condition, List<AttributeModifierEntry> modifiers)`
- `PassiveAbilityEntry(ResourceLocation type, JsonObject params, @Nullable ConditionDefinition activationCondition)`
- `ActiveAbilityEntry(ResourceLocation type, String slot, int cooldownTicks, int resourceCost, int unlockLevel, JsonObject params)`
- `ResourceDefinition(String typeKey, int maxValue, int startValue, float drainPerSecond, float regenPerSecond, int color, ResourceLocation icon)`
- `ConditionDefinition` — рекурсивная: `type` + `params` или `and/or/not` + `children`

#### ClassManager.java — управление классами игроков

Ответственности:
1. **Назначение класса** (`assignClass(ServerPlayer, ResourceLocation) → Result`)
   - Если есть старый класс — снять его полностью
   - Применить модификаторы атрибутов через ванильную систему `AttributeModifier`
   - Создать инстансы пассивных способностей через фабрику, вызвать `onApply(player)`
   - Создать инстансы активных способностей
   - Инициализировать ресурс (current = startValue)
   - Применить size modifier через `EntityDimensions` (если есть)
   - Синхронизировать с клиентом через NetworkHandler
   - Опубликовать `ClassAssignedEvent`

2. **Снятие класса** (`removeClass(ServerPlayer) → Result`)
   - Удалить ВСЕ модификаторы атрибутов (по UUID-паттерну `archetype:class_<attribute>`)
   - Вызвать `onRemove(player)` для каждой пассивки
   - Принудительно деактивировать активные способности (`forceDeactivate`)
   - Очистить кулдауны, ресурс, toggle-состояния
   - Вернуть размер модели
   - Синхронизировать, опубликовать `ClassRemovedEvent`

3. **Серверный тик** (`tickPlayer(ServerPlayer)`)
   Вызывается каждый тик для каждого онлайн-игрока с классом:
   
   | Действие | Частота | Реализация |
   |----------|---------|------------|
   | Уменьшить кулдауны | Каждый тик | `cooldowns.replaceAll((k, v) -> Math.max(0, v - 1))` |
   | Тикнуть длительные активные | Каждый тик | `if (ability.isActive()) ability.tickActive(player)` |
   | Проверить условные атрибуты | Каждый 20-й тик | Тест каждого условия, применить/снять модификаторы |
   | Тикнуть пассивки | Каждый 20-й тик | `passive.tick(player)` |
   | Обновить ресурс | Каждый 10-й тик | drain/regen с clamp(0, max) |
   | Синхронизация | Каждый 20-й тик | Отправить `SyncClassDataPacket` |
   
   Используй `tickCounter` (инкремент каждый тик) и проверку `tickCounter % N == 0`.

4. **Обработка событий** (методы, вызываемые из EventTranslator):
   - `onPlayerDeath(ServerPlayer)` — кулдауны сохраняются, ресурс → startValue
   - `onPlayerRespawn(ServerPlayer)` — переприменить атрибуты (MC сбрасывает при смерти), перезапустить пассивки
   - `onPlayerJoin(ServerPlayer)` — если класс есть → применить, синхронизировать; если нет и firstJoin → открыть витрину
   - `onPlayerAttack(ServerPlayer, Entity, DamageSource)` — делегировать пассивкам (`onPlayerAttack`)
   - `onPlayerHurt(ServerPlayer, DamageSource, float)` — делегировать пассивкам (`onPlayerHurt`)
   - `onPlayerEat(ServerPlayer, ItemStack)` — делегировать пассивкам (`onPlayerEat`)

5. **Кэширование**
   Для каждого онлайн-игрока с классом хранить `ActiveClassInstance`:
   ```java
   class ActiveClassInstance {
       PlayerClass classDefinition;
       List<PassiveAbility> activePassives;
       Map<String, ActiveAbility> activeAbilities; // slot → ability
       int tickCounter;
   }
   ```
   Map<UUID, ActiveClassInstance> — очищается при выходе игрока.

#### Edge cases (КРИТИЧНО):

- **Класс удалён из реестра при reload:** снять с игрока, логировать WARNING
- **assignClass во время активной toggle-способности:** forceDeactivate перед снятием
- **Игрок в креативе:** способности работают, но ресурс не тратится
- **Игрок мёртв:** не тикать
- **Два вызова assignClass подряд:** второй снимает первый корректно
- **Thread safety:** все операции только на серверном потоке, assert `!level.isClientSide`

---

### Модуль ability/

#### PassiveAbility.java — интерфейс пассивной способности

```java
public interface PassiveAbility {
    /** Вызывается при назначении класса */
    void onApply(ServerPlayer player);
    
    /** Вызывается раз в 20 тиков */
    void tick(ServerPlayer player);
    
    /** Вызывается при снятии класса */
    void onRemove(ServerPlayer player);
    
    // Хуки событий (default — пустые)
    default void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {}
    default void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {}
    default void onPlayerEat(ServerPlayer player, ItemStack food) {}
    
    /** Тип способности для сериализации */
    ResourceLocation getType();
    
    /** Является ли бонусом (true) или штрафом (false) — для GUI */
    boolean isPositive();
    
    /** Ключ перевода названия */
    String getNameKey();
    
    /** Ключ перевода описания */
    String getDescriptionKey();
}
```

#### ActiveAbility.java — интерфейс активной способности

```java
public interface ActiveAbility {
    /** Может ли сейчас активироваться */
    boolean canActivate(ServerPlayer player);
    
    /** Активировать. Возвращает результат */
    ActivationResult activate(ServerPlayer player);
    
    /** Тик активной/длительной способности (каждый тик, пока isActive) */
    void tickActive(ServerPlayer player);
    
    /** Принудительная деактивация */
    void forceDeactivate(ServerPlayer player);
    
    /** Сейчас активна? (для длительных/toggle) */
    boolean isActive();
    
    /** Для заряжаемых: отпускание кнопки */
    default void onRelease(ServerPlayer player, int chargeLevel) {}
    
    /** Тип способности */
    ResourceLocation getType();
    
    /** Слот привязки (ability_1, ability_2, ability_3) */
    String getSlot();
    
    /** Кулдаун в тиках */
    int getCooldownTicks();
    
    /** Стоимость ресурса */
    int getResourceCost();
    
    /** Уровень разблокировки (0 = сразу) */
    int getUnlockLevel();
    
    /** Метаданные для GUI */
    String getNameKey();
    String getDescriptionKey();
    ResourceLocation getIcon();
}

public enum ActivationResult {
    SUCCESS,
    ON_COOLDOWN,
    NOT_ENOUGH_RESOURCE,
    LEVEL_TOO_LOW,
    CONDITION_NOT_MET,
    ALREADY_ACTIVE,
    FAILED
}
```

#### AbilityRegistry.java — фабрика способностей

```java
public class AbilityRegistry {
    private final Map<ResourceLocation, PassiveAbilityFactory> passiveFactories;
    private final Map<ResourceLocation, ActiveAbilityFactory> activeFactories;
    
    void registerPassive(ResourceLocation type, PassiveAbilityFactory factory);
    void registerActive(ResourceLocation type, ActiveAbilityFactory factory);
    
    PassiveAbility createPassive(PassiveAbilityEntry entry);
    ActiveAbility createActive(ActiveAbilityEntry entry);
    
    /** Регистрация встроенных типов в init() */
    void registerBuiltins();
}

@FunctionalInterface
public interface PassiveAbilityFactory {
    PassiveAbility create(JsonObject params);
}

@FunctionalInterface
public interface ActiveAbilityFactory {
    ActiveAbility create(ActiveAbilityEntry entry);
}
```

Встроенные типы НЕ реализовывать (будут в MiniMax-промптах). Только зарегистрировать маппинг `ResourceLocation → Factory` в `registerBuiltins()`. Пока — пустые заглушки.

#### Базовые классы-помощники

`AbstractPassiveAbility` — абстрактный класс с общей логикой:
- Хранит `JsonObject params`
- Методы `getInt(key, default)`, `getFloat(key, default)`, `getString(key, default)`, `getBool(key, default)` для чтения параметров из JSON
- Default-реализации `onApply`, `onRemove` — пустые

`AbstractActiveAbility` — абстрактный класс:
- Хранит `ActiveAbilityEntry entry`, `boolean active`
- Реализует `getSlot()`, `getCooldownTicks()`, `getResourceCost()`, `getUnlockLevel()`
- `canActivate` — проверяет кулдаун, ресурс, уровень, условия
- JSON-хелперы аналогично

---

### Модуль condition/

#### Condition.java

```java
public interface Condition {
    boolean test(Player player);
    ResourceLocation getType();
}
```

#### ConditionRegistry.java

```java
public class ConditionRegistry {
    private final Map<ResourceLocation, ConditionFactory> factories;
    
    void register(ResourceLocation type, ConditionFactory factory);
    Condition create(ConditionDefinition definition);
    void registerBuiltins();
}

@FunctionalInterface
public interface ConditionFactory {
    Condition create(JsonObject params);
}
```

#### Комбинаторы (РЕАЛИЗОВАТЬ ПОЛНОСТЬЮ)

```java
class AndCondition implements Condition {
    List<Condition> children;
    boolean test(Player p) { return children.stream().allMatch(c -> c.test(p)); }
}

class OrCondition implements Condition { ... }
class NotCondition implements Condition { ... }
```

Парсинг из `ConditionDefinition`:
- Если `type == "and"` → рекурсивно создать children, вернуть `AndCondition`
- Если `type == "or"` → `OrCondition`
- Если `type == "not"` → `NotCondition` с одним child
- Иначе → `factories.get(type).create(params)`

Встроенные типы условий НЕ реализовывать (будут в MiniMax). Только комбинаторы.

---

### События для других модов

```java
// Architectury Events
public class ArchetypeEvents {
    public static final Event<ClassAssigned> CLASS_ASSIGNED = EventFactory.createLoop(ClassAssigned.class);
    public static final Event<ClassRemoved> CLASS_REMOVED = EventFactory.createLoop(ClassRemoved.class);
    public static final Event<AbilityUsed> ABILITY_USED = EventFactory.createLoop(AbilityUsed.class);
    public static final Event<AbilityPreUse> ABILITY_PRE_USE = EventFactory.createCancellable(AbilityPreUse.class);
    public static final Event<ClassLevelUp> CLASS_LEVEL_UP = EventFactory.createLoop(ClassLevelUp.class);
    
    public interface ClassAssigned { void onAssigned(ServerPlayer player, PlayerClass newClass); }
    public interface ClassRemoved { void onRemoved(ServerPlayer player, PlayerClass oldClass); }
    public interface AbilityUsed { void onUsed(ServerPlayer player, ActiveAbility ability); }
    public interface AbilityPreUse { 
        EventResult onPreUse(ServerPlayer player, ActiveAbility ability); 
    }
    public interface ClassLevelUp { void onLevelUp(ServerPlayer player, int newLevel); }
}
```

## Формат вывода

Каждый файл полностью. Порядок:
1. `PlayerClass.java` и все вложенные record-ы
2. `ClassCategory.java` (enum)
3. `Condition.java`, `ConditionRegistry.java`, комбинаторы
4. `PassiveAbility.java`, `AbstractPassiveAbility.java`
5. `ActiveAbility.java`, `ActivationResult.java`, `AbstractActiveAbility.java`
6. `AbilityRegistry.java`
7. `ActiveClassInstance.java`
8. `ClassManager.java`
9. `ArchetypeEvents.java`
10. Обновлённый `ArchetypeAPI.java` (делегирует ClassManager и реестрам)

## Чего НЕ делать

- Не реализуй конкретные типы способностей (sun_damage, timed_buff и т.д.)
- Не реализуй конкретные типы условий (time_of_day, in_water и т.д.)
- Не пиши JSON-парсинг ClassRegistry — это отдельный промпт
- Не пиши GUI, команды, сеть
- Фабрики способностей регистрируют только ID → заглушка-factory
