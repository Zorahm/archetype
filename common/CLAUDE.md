# common module

Платформо-независимое ядро. ~95% всего кода. Компилируется с fabric-loader (для маппингов).

## Точка входа

`Archetype.init()` — вызывается из fabric инициализатора. Регистрирует builtin условия, способности, advancement triggers, сеть.

## Добавление нового типа способности

1. Реализовать `ActiveAbility` или `PassiveAbility` интерфейс
2. Создать фабрику (`ActiveAbilityFactory` / `PassiveAbilityFactory`)
3. Зарегистрировать в `AbilityRegistry.registerBuiltins()` или через `ArchetypeAPI`
4. Добавить ключи локализации в оба lang-файла

## Добавление нового типа условия

1. Реализовать `Condition` интерфейс (метод `test(Player) → boolean`)
2. Создать `ConditionFactory`
3. Зарегистрировать в `ConditionRegistry.registerBuiltins()` или через `ArchetypeAPI`

## Добавление нового класса

Создать JSON в `resources/data/archetype/archetype_classes/`. Не требует Java-кода. Формат см. в существующих JSON (vi.json, ram.json, lin_qi.json, ru_yi.json).

## Сеть

Все пакеты в `network/`. Регистрация в `NetworkInit.register()`. Кодирование/декодирование через FriendlyByteBuf.

Направления:
- Client → Server: `ClassSelectPacket`, `AbilityUsePacket`, `AbilityReleasePacket`
- Server → Client: `OpenClassSelectionPacket`, `SyncClassDataPacket`, `ClassAssignResultPacket`, `PlayerClassSyncPacket`

## Ресурсы

- `assets/archetype/lang/` — локализация (en_us, ru_ru)
- `data/archetype/archetype_classes/` — встроенные классы
- `data/archetype/advancements/` — достижения
