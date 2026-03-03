# MM #5 — ClassRegistry и JSON-парсинг

## Роль

Java-разработчик. Пишешь систему загрузки JSON-определений классов для Minecraft мода.

## Контекст

Посмотри проект

Мод **Archetype** загружает классы из JSON-файлов в `data/<namespace>/archetype_classes/`. Файлы могут находиться в самом моде, в датапаках, в других модах.

## Задача

Реализуй `registry/ClassRegistry.java` — загрузка, валидация и хранение определений классов.

## Требования

### Загрузка

1. Сканировать `data/*/archetype_classes/*.json` через `ResourceManager` (ванильный `SimpleJsonResourceReloadListener` или `ResourceManagerReloadListener`)
2. Парсить каждый файл через `Gson` или ручной `JsonObject` → поля `PlayerClass`
3. Валидировать каждое поле
4. При ошибке в файле: **логировать ошибку** с указанием файла и поля, **пропустить файл**, НЕ крашить
5. Сохранить валидные классы в `Map<ResourceLocation, PlayerClass>`

### Парсинг JSON

Полная структура файла:

```json
{
  "id": "archetype:vampire",
  "name": "class.archetype.vampire.name",
  "description": "class.archetype.vampire.description",
  "icon": "archetype:textures/gui/class_icons/vampire.png",
  "color": "#8B0000",
  "category": "damage",
  "lore": [
    "class.archetype.vampire.lore.1",
    "class.archetype.vampire.lore.2"
  ],
  
  "attributes": [
    {
      "attribute": "minecraft:generic.max_health",
      "operation": "addition",
      "value": -4.0
    },
    {
      "attribute": "minecraft:generic.attack_damage",
      "operation": "addition",
      "value": 1.0
    },
    {
      "attribute": "minecraft:generic.movement_speed",
      "operation": "multiply_base",
      "value": 0.15
    }
  ],
  
  "conditional_attributes": [
    {
      "condition": {
        "type": "archetype:time_of_day",
        "range": [13000, 23000]
      },
      "modifiers": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "operation": "addition",
          "value": 2.0
        }
      ]
    }
  ],
  
  "passive_abilities": [
    {
      "type": "archetype:sun_damage",
      "positive": false,
      "name": "passive.archetype.vampire.sun_damage.name",
      "description": "passive.archetype.vampire.sun_damage.desc",
      "params": {
        "damage_per_second": 1.0,
        "ignore_if_helmet": true,
        "set_fire_ticks": 60
      }
    },
    {
      "type": "archetype:night_vision",
      "positive": true,
      "name": "passive.archetype.vampire.night_vision.name",
      "description": "passive.archetype.vampire.night_vision.desc",
      "params": {
        "amplifier": 0
      },
      "condition": {
        "type": "archetype:time_of_day",
        "range": [13000, 23000]
      }
    }
  ],
  
  "active_abilities": [
    {
      "type": "archetype:blood_drain",
      "slot": "ability_1",
      "cooldown": 100,
      "resource_cost": 0,
      "unlock_level": 1,
      "name": "ability.archetype.vampire.blood_drain.name",
      "description": "ability.archetype.vampire.blood_drain.desc",
      "icon": "archetype:textures/gui/abilities/blood_drain.png",
      "params": {
        "damage_per_tick": 1.0,
        "heal_percent": 0.5,
        "duration_ticks": 40,
        "range": 4.0
      }
    }
  ],
  
  "resource": {
    "type": "resource.archetype.blood",
    "max": 100,
    "start": 100,
    "drain_per_second": 0.1,
    "regen_per_second": 0.0,
    "color": "#8B0000",
    "icon": "archetype:textures/gui/resource/blood.png"
  },
  
  "size_modifier": null,
  "incompatible_with": []
}
```

### Парсер

Класс `ClassJsonParser`:

```java
public class ClassJsonParser {
    /**
     * Парсит JSON в PlayerClass. Бросает ClassParseException при ошибке.
     */
    public static PlayerClass parse(ResourceLocation fileId, JsonObject json) throws ClassParseException;
    
    // Вспомогательные:
    private static List<AttributeModifierEntry> parseAttributes(JsonArray arr);
    private static List<ConditionalAttributeEntry> parseConditionalAttributes(JsonArray arr);
    private static List<PassiveAbilityEntry> parsePassives(JsonArray arr);
    private static List<ActiveAbilityEntry> parseActives(JsonArray arr);
    private static ResourceDefinition parseResource(JsonObject obj);
    private static ConditionDefinition parseCondition(JsonObject obj);  // рекурсивный
    private static int parseColor(String hex);  // "#8B0000" → int
}
```

### Валидация

Для каждого поля:
- Обязательные поля (`id`, `name`, `description`, `icon`, `color`) — проверить наличие
- `color` — валидный HEX (с или без `#`)
- `category` — одно из: `damage`, `tank`, `mobility`, `utility` (или отсутствует)
- `attributes[].attribute` — валидный ResourceLocation (синтаксис, не проверять реестр)
- `attributes[].operation` — одно из: `addition`, `multiply_base`, `multiply_total`
- `active_abilities[].slot` — одно из: `ability_1`, `ability_2`, `ability_3`
- `active_abilities[].cooldown` — ≥ 0
- `active_abilities[].unlock_level` — ≥ 0 (0 = сразу доступна)
- `resource.max` — > 0
- Типы пассивок и активок — проверить что зарегистрированы в `AbilityRegistry`
- Типы условий — проверить что зарегистрированы в `ConditionRegistry`

### ClassRegistry.java

```java
public class ClassRegistry extends SimpleJsonResourceReloadListener {
    private Map<ResourceLocation, PlayerClass> classes = Map.of();
    
    // Lifecycle
    void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler);
    
    // Доступ
    Optional<PlayerClass> get(ResourceLocation id);
    Collection<PlayerClass> getAll();
    boolean exists(ResourceLocation id);
    Set<ResourceLocation> getAllIds();
    
    // Команда /archetype reload
    void reload(ResourceManager manager);
}
```

`apply()`:
1. Создать новый `Map<ResourceLocation, PlayerClass>`
2. Для каждого JSON: try → parse → put. Catch → log error, continue.
3. Логировать: `"Loaded {} classes ({} failed)"`, count, errorCount
4. Атомарно заменить `this.classes = Collections.unmodifiableMap(newMap)`

### ClassParseException

```java
public class ClassParseException extends Exception {
    private final ResourceLocation fileId;
    private final String field;
    // getMessage() → "Error parsing class 'archetype:vampire' at field 'attributes[2].operation': ..."
}
```

## Формат вывода

Файлы:
1. `ClassParseException.java`
2. `ClassJsonParser.java`
3. `ClassRegistry.java`

## Чего НЕ делать

- Не парси способности и условия рекурсивно — только создай `ConditionDefinition`/`PassiveAbilityEntry`/`ActiveAbilityEntry` с raw `JsonObject params`
- Не реализуй конкретные типы — парсер не знает что внутри `params`
- Не крашь сервер при невалидном JSON — только лог + пропуск
