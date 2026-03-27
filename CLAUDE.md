# Archetype — Minecraft Class System Mod

## Обзор

Мод системы классов для Minecraft 1.20.1. Мультилоадерный (Forge + Fabric) через Architectury. Java 17, Official Mojang Mappings.

**Принцип:** баланс через компромисс — каждый бонус компенсирован штрафом.

## Структура проекта

```
archetype/
├── common/    # ~95% кода. Ядро, способности, условия, сеть, GUI, команды
├── fabric/    # Fabric-слой: Data Attachments, Fabric Networking API, события
├── forge/     # Forge-слой: Capabilities, SimpleChannel, события
```

Пакет: `com.mod.archetype` (common), `com.mod.archetype.fabric`, `com.mod.archetype.forge`.

## Сборка

```bash
./gradlew build
```

Артефакты: `fabric/build/libs/*.jar`, `forge/build/libs/*.jar`.

Shadow JAR включает common в каждый платформенный модуль.

## Архитектурные решения

- **Платформа-абстракция через ServiceLoader**: `NetworkHandler`, `PlayerDataAccess`, `PlatformHelper` — интерфейсы в `platform/`, реализации в forge/fabric модулях
- **Singleton + приватный конструктор**: `ClassManager.getInstance()`, `AbilityRegistry.getInstance()`, `ConditionRegistry.getInstance()`
- **Factory-паттерн**: `PassiveAbilityFactory`, `ActiveAbilityFactory`, `ConditionFactory` — создают экземпляры из JSON-параметров
- **Immutable records**: `PlayerClass`, `AttributeModifierEntry`, `ConditionalAttributeEntry`, `ResourceDefinition`, `ConditionDefinition`
- **JSON-датапаки**: классы загружаются из `data/<namespace>/archetype_classes/*.json` через `ClassRegistry` (extends `SimpleJsonResourceReloadListener`)

## Ключевые модули (common)

| Пакет | Суть |
|-------|------|
| `core/` | `ClassManager` — жизненный цикл (assign/tick/remove). `PlayerClass` — определение класса. `ActiveClassInstance` — кэш инстанцированных способностей |
| `ability/` | 25 пассивных + 14 активных типов. Интерфейсы `PassiveAbility`, `ActiveAbility`. Реестр `AbilityRegistry` |
| `condition/` | 12 типов условий + комбинаторы (And/Or/Not). `ConditionRegistry` |
| `data/` | `PlayerClassData` — состояние игрока: класс, уровень, опыт, ресурс, кулдауны, тоглы. NBT-сериализация |
| `network/` | 8 пакетов (ClassSelect, AbilityUse/Release, OpenClassSelection, SyncClassData, ClassAssignResult, PlayerClassSync). Серверная валидация |
| `registry/` | `ClassRegistry` — загрузка JSON-классов из датапаков |
| `gui/` | `ClassSelectionScreen`, `ClassDetailScreen`, `ClassInfoScreen`, `AbilityHudOverlay` |
| `command/` | `/archetype set/remove/get/list/reload` |
| `item/` | `RebirthScrollItem` — предмет смены класса |

## Жизненный цикл класса

1. **Assign**: атрибуты → passives.onApply() → actives создаются → ресурс init → данные → синхронизация → событие
2. **Tick** (каждый тик): кулдауны -1 → (каждые 20 тиков) проверка условий, tick пассивок, обновление ресурса → синк
3. **Remove**: атрибуты снять → passives.onRemove() → actives деактивировать → данные очистить → синк → событие

## Конвенции кода

- **Язык**: Java 17, без Kotlin
- **Нейминг**: PascalCase для классов, camelCase для методов (verb-first), UPPER_SNAKE_CASE для констант
- **Null-safety**: `@Nullable` из org.jetbrains.annotations
- **Комментарии**: минимум, код самодокументирующийся. Не добавлять Javadoc без запроса
- **Логирование**: SLF4J через `Archetype.LOGGER`
- **Mod ID**: `Archetype.MOD_ID` ("archetype"), не хардкодить строки
- **ResourceLocation**: `new ResourceLocation(Archetype.MOD_ID, "name")`

## Расширяемость (API)

```java
ArchetypeAPI.registerAbilityType(id, factory);     // Новый тип активной способности
ArchetypeAPI.registerPassiveType(id, factory);      // Новый тип пассивной способности
ArchetypeAPI.registerConditionType(id, factory);    // Новый тип условия
ArchetypeAPI.getPlayerClass(player);                // Получить текущий класс
ArchetypeAPI.assignClass(serverPlayer, classId);    // Назначить класс
```

## Релизы

- Текст релиза — на **английском языке**
- Прикреплять только два JAR-файла: `archetype-<version>-fabric.jar` и `archetype-<version>-forge.jar`
- Dev-shadow и sources JAR к релизу **не прикладывать**
- JAR-файлы брать из GitHub Actions артефактов после завершения билда

## Локализация

Два языка: `en_us.json`, `ru_ru.json`. Ключи по неймспейсам:
- `class.archetype.<id>.*` — названия/описания классов
- `ability.archetype.<id>.<ability>.*` — способности
- `passive.archetype.<id>.<passive>.*` — пассивки
- `gui.archetype.*` — интерфейс
- `commands.archetype.*` — команды

## Зависимости

Только Minecraft + Architectury API. Никаких внешних библиотек. JSR305 — compileOnly.
