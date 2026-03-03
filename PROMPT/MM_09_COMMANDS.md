# MM #9 — Команды (Brigadier)

## Роль

Java-разработчик. Реализуешь систему команд через Brigadier API.

## Контекст

Посмотри проект

## Задача

Реализуй модуль `command/` — все команды мода + кастомный ArgumentType.

## Команды

```
/archetype set <player> <class> [force:bool]
/archetype remove <player>
/archetype get <player>
/archetype list
/archetype select [player]
/archetype reload
/archetype ability cooldown <player> <ability_id> <ticks>
/archetype ability trigger <player> <ability_id>
/archetype ability reset <player>
/archetype resource <player> set <amount>
/archetype resource <player> add <amount>
/archetype level <player> set <level>
/archetype level <player> add <amount>
```

## Права доступа

| Команда | Уровень | Кто может |
|---------|---------|-----------|
| `list`, `get` | 0 | Все игроки |
| `select` (для себя) | 0 | Если `allowPlayerSelfSelect` в конфиге |
| `select` (другого) | 2 | Операторы |
| `set`, `remove`, `reload`, `ability`, `resource`, `level` | 2 | Операторы |

## Файлы

### ArchetypeCommand.java

Главный класс, регистрирует дерево команд:

```java
public class ArchetypeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("archetype");
        
        root.then(/* set */);
        root.then(/* remove */);
        root.then(/* get */);
        root.then(/* list */);
        root.then(/* select */);
        root.then(/* reload */);
        root.then(/* ability */);
        root.then(/* resource */);
        root.then(/* level */);
        
        dispatcher.register(root);
    }
}
```

### ClassIdArgument.java

Кастомный ArgumentType для автодополнения ID классов:

```java
public class ClassIdArgument implements ArgumentType<ResourceLocation> {
    // parse: ResourceLocation.read(reader)
    // listSuggestions: из ClassRegistry.getAllIds(), фильтр по уже введённому тексту
    // getExamples: ["archetype:vampire", "archetype:golem"]
}
```

Регистрация: через `ArgumentTypeInfos` (Forge/Fabric различается — оставить хелпер).

### Реализация каждой команды

**set:**
```java
// /archetype set <player> <class> [force:bool]
.then(Commands.literal("set")
    .requires(src -> src.hasPermission(2))
    .then(Commands.argument("player", EntityArgument.player())
        .then(Commands.argument("class", new ClassIdArgument())
            .executes(ctx -> executeSet(ctx, false))
            .then(Commands.argument("force", BoolArgumentType.bool())
                .executes(ctx -> executeSet(ctx, BoolArgumentType.getBool(ctx, "force")))
            )
        )
    )
)
```
Логика `executeSet`: `ClassManager.assignClass(player, classId)`. Если `force` — игнорировать кулдаун и incompatible. Сообщение в чат: `"Set class of %s to %s"`.

**remove:** `ClassManager.removeClass(player)`. Сообщение: `"Removed class from %s"`.

**get:** Показать: имя класса, уровень, ресурс. `"Player %s: class=%s, level=%d, resource=%.0f/%.0f"`.

**list:** Перечислить все классы из реестра. Формат: `"Available classes (%d): vampire, golem, phantom, ..."`

**select:** Если `player` указан → отправить `OpenClassSelectionPacket` этому игроку. Если нет → отправить отправителю.

**reload:** `ClassRegistry.reload()`. Сообщение с количеством загруженных классов.

**ability cooldown:** `data.setCooldown(abilityId, ticks)`. 0 = сбросить.

**ability trigger:** `ClassManager.triggerAbility(player, abilityId)` — программно активировать.

**ability reset:** Сбросить все кулдауны, деактивировать все toggle.

**resource set:** `data.resourceCurrent = amount`, clamp(0, max).

**resource add:** `data.resourceCurrent = clamp(current + amount, 0, max)`. Может быть отрицательным (вычитание).

**level set:** `data.classLevel = clamp(level, 1, maxLevel)`.

**level add:** аналогично.

### Обратная связь (feedback)

Все команды:
- При успехе: `source.sendSuccess(() -> Component.translatable("commands.archetype.xxx.success", args), true)`
- При ошибке: бросают `CommandSyntaxException` с читаемым сообщением
- `broadcastToOps = true` для модифицирующих команд

### Tab-completion

- `<player>` → `EntityArgument.player()` (встроенное автодополнение)
- `<class>` → `ClassIdArgument` с suggestions из реестра
- `<ability_id>` → suggestions из способностей текущего класса игрока
- `[force]` → `BoolArgumentType.bool()`

## Формат вывода

1. `ClassIdArgument.java`
2. `ArchetypeCommand.java` (полное дерево + все execute-методы)

Весь код в одном файле `ArchetypeCommand` допустим (команды компактны). Если > 300 строк — разбить по подклассам.
