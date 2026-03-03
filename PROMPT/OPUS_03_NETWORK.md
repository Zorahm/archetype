# OPUS #3 — Сетевой протокол и синхронизация

## Роль

Ты — senior Minecraft mod developer. Проектируешь сетевой протокол мода. Приоритеты: безопасность (вся валидация на сервере), минимальный трафик, корректная синхронизация.

## Контекст из предыдущих шагов

Посмотри проект

## Задача

Реализуй модуль `network/` в common:
1. Все пакеты (классы с сериализацией/десериализацией через `FriendlyByteBuf`)
2. Серверные обработчики с полной валидацией
3. Клиентские обработчики
4. Класс `NetworkInit` для регистрации всех пакетов через `NetworkHandler`

## Пакеты: клиент → сервер

### 1. ClassSelectPacket

| Поле | Тип | Размер |
|------|-----|--------|
| classId | ResourceLocation | ~30 байт |
| viaItem | boolean | 1 байт |

**Сериализация:**
```java
void encode(FriendlyByteBuf buf) {
    buf.writeResourceLocation(classId);
    buf.writeBoolean(viaItem);
}
static ClassSelectPacket decode(FriendlyByteBuf buf) {
    return new ClassSelectPacket(buf.readResourceLocation(), buf.readBoolean());
}
```

**Серверная валидация (ВСЕ проверки, именно в этом порядке):**
1. `classId` не null, длина строки ≤ 256 символов → иначе `INVALID_PACKET`
2. Класс существует в реестре → иначе `CLASS_NOT_FOUND`
3. Класс не в списке `disabledClasses` конфига → иначе `CLASS_DISABLED`
4. Если у игрока уже есть класс: проверить кулдаун смены (`classChangeCooldownTicks`) → иначе `CHANGE_ON_COOLDOWN`
5. Если `viaItem == true`: проверить что ItemStack в MainHand — это Свиток Перерождения → иначе `ITEM_NOT_FOUND`
6. Если у игрока уже есть класс: проверить `incompatibleWith` → иначе `INCOMPATIBLE_CLASS`
7. Если `allowPlayerSelfSelect == false` в конфиге и это не первый выбор → иначе `NOT_ALLOWED`

**При успехе:**
- `ClassManager.assignClass(player, classId)`
- Если `viaItem`: уменьшить стак на 1 → `player.getMainHandItem().shrink(1)`
- Отправить `ClassAssignResultPacket(true, null)` игроку
- Отправить `PlayerClassSyncPacket(player.getUUID(), classId)` всем трекающим

**При ошибке:**
- Отправить `ClassAssignResultPacket(false, reasonKey)`
- Логировать `WARN`: `"Player {} failed class select {}: {}", player.getName(), classId, reason`

### 2. AbilityUsePacket

| Поле | Тип | Размер |
|------|-----|--------|
| slotName | String (max 32) | ~10 байт |

**Серверная валидация:**
1. `slotName` не пустой и длина ≤ 32 → иначе дроп пакета, лог WARN
2. У игрока назначен класс → иначе дроп
3. В этом слоте есть способность → иначе дроп
4. Способность не на кулдауне (серверный трекер) → иначе дроп (не exploit — может быть рассинхрон)
5. Хватает ресурса → иначе дроп
6. Уровень класса ≥ `unlockLevel` → иначе дроп
7. `ability.canActivate(player)` → true → иначе дроп
8. `ArchetypeEvents.ABILITY_PRE_USE.invoker().onPreUse(player, ability)` → не `CANCEL`

**При успехе:**
- `ActivationResult result = ability.activate(player)`
- Если `result == SUCCESS`:
  - Вычесть ресурс: `data.resourceCurrent -= ability.getResourceCost()` (если не креатив)
  - Установить кулдаун: `data.cooldowns.put(abilityId, ability.getCooldownTicks())`
  - Опубликовать `ArchetypeEvents.ABILITY_USED`
  - Синхронизировать немедленно (не ждать 20-тиковый цикл)

### 3. AbilityReleasePacket

| Поле | Тип | Размер |
|------|-----|--------|
| slotName | String (max 32) | ~10 байт |

**Серверная валидация:**
1. Слот валиден, класс назначен
2. Способность в этом слоте — заряжаемого типа (`charged`)
3. Способность сейчас активна (идёт зарядка)

**При успехе:**
- Считать `chargeLevel` из внутреннего состояния способности (сколько тиков заряжалась)
- `ability.onRelease(player, chargeLevel)`
- Установить кулдаун, вычесть ресурс, синхронизировать

---

## Пакеты: сервер → клиент

### 4. OpenClassSelectionPacket

| Поле | Тип | Размер |
|------|-----|--------|
| mode | byte | 1 байт |

`mode`: 0 = первый выбор, 1 = перерождение

**Клиентский обработчик:**
- Открыть `ClassSelectionScreen` с соответствующим режимом
- Выполнять на клиентском потоке: `Minecraft.getInstance().execute(() -> ...)`

### 5. SyncClassDataPacket

| Поле | Тип | Размер |
|------|-----|--------|
| hasClass | boolean | 1 байт |
| classId | ResourceLocation | ~30 байт (если hasClass) |
| level | int | 4 байта |
| experience | int | 4 байта |
| resourceCurrent | float | 4 байта |
| resourceMax | float | 4 байта |
| cooldownCount | int | 4 байта |
| cooldowns[] | (ResourceLocation + int remaining + int max)[] | ~40 байт × N |
| toggleCount | int | 4 байта |
| toggleStates[] | (ResourceLocation + boolean)[] | ~30 байт × N |

Итого: ~100–200 байт. Отправляется раз в 20 тиков + при изменении.

**Сериализация:**
```java
void encode(FriendlyByteBuf buf) {
    buf.writeBoolean(hasClass);
    if (hasClass) {
        buf.writeResourceLocation(classId);
        buf.writeVarInt(level);
        buf.writeVarInt(experience);
        buf.writeFloat(resourceCurrent);
        buf.writeFloat(resourceMax);
        
        buf.writeVarInt(cooldowns.size());
        cooldowns.forEach((id, entry) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(entry.remaining());
            buf.writeVarInt(entry.max());
        });
        
        buf.writeVarInt(toggleStates.size());
        toggleStates.forEach((id, state) -> {
            buf.writeResourceLocation(id);
            buf.writeBoolean(state);
        });
    }
}
```

**Клиентский обработчик:**
- Обновить `ClientClassData` — клиентское зеркало данных (без серверных ссылок)
- НЕ хранить ссылки на серверные объекты
- Используется GUI и HUD для отображения

### 6. ClassAssignResultPacket

| Поле | Тип | Размер |
|------|-----|--------|
| success | boolean | 1 байт |
| failReasonKey | String (nullable) | 0–50 байт |

**Клиентский обработчик:**
- Если `success`: закрыть экран выбора, показать toast-уведомление, проиграть звук
- Если `!success`: показать сообщение об ошибке на экране выбора (перевести `failReasonKey`)

### 7. PlayerClassSyncPacket

| Поле | Тип | Размер |
|------|-----|--------|
| playerUUID | UUID | 16 байт |
| hasClass | boolean | 1 байт |
| classId | ResourceLocation | ~30 байт (если hasClass) |

Отправляется **другим игрокам** в зоне трекинга. Используется для:
- Отображения класса над головой (nameplate)
- Клиентских эффектов (партиклы, визуал)

**Клиентский обработчик:**
- Обновить `ClientOtherPlayersData.put(playerUUID, classId)`
- Кэш очищается при выходе игрока из зоны видимости

---

## Клиентские данные

### ClientClassData.java

Клиентское зеркало данных текущего игрока. Синглтон, обновляется из `SyncClassDataPacket`.

```java
public class ClientClassData {
    private static final ClientClassData INSTANCE = new ClientClassData();
    
    private boolean hasClass;
    private @Nullable ResourceLocation classId;
    private int level;
    private int experience;
    private float resourceCurrent;
    private float resourceMax;
    private Map<ResourceLocation, CooldownInfo> cooldowns = new HashMap<>();
    private Map<ResourceLocation, Boolean> toggleStates = new HashMap<>();
    
    // Getters (thread-safe: обновляется только на render thread)
    // Метод update(SyncClassDataPacket) — вызывается из клиентского обработчика
    // Метод clear() — при дисконнекте
    
    public record CooldownInfo(int remaining, int maxTicks) {
        public float getProgress() { return maxTicks > 0 ? (float) remaining / maxTicks : 0f; }
    }
}
```

### ClientOtherPlayersData.java

Кэш классов других игроков.

```java
public class ClientOtherPlayersData {
    private static final Map<UUID, ResourceLocation> playerClasses = new ConcurrentHashMap<>();
    
    public static void update(UUID playerId, @Nullable ResourceLocation classId);
    public static @Nullable ResourceLocation getClass(UUID playerId);
    public static void remove(UUID playerId);
    public static void clear(); // при дисконнекте
}
```

---

## NetworkInit.java

Регистрирует все пакеты через абстрактный `NetworkHandler`.

```java
public class NetworkInit {
    public static void register(NetworkHandler handler) {
        // Клиент → Сервер
        handler.registerServerReceiver(
            id("class_select"), ClassSelectPacket.class,
            ClassSelectPacket::decode, ClassSelectHandler::handle
        );
        handler.registerServerReceiver(
            id("ability_use"), AbilityUsePacket.class,
            AbilityUsePacket::decode, AbilityUseHandler::handle
        );
        handler.registerServerReceiver(
            id("ability_release"), AbilityReleasePacket.class,
            AbilityReleasePacket::decode, AbilityReleaseHandler::handle
        );
        
        // Сервер → Клиент
        handler.registerClientReceiver(
            id("open_selection"), OpenClassSelectionPacket.class,
            OpenClassSelectionPacket::decode, OpenSelectionClientHandler::handle
        );
        handler.registerClientReceiver(
            id("sync_data"), SyncClassDataPacket.class,
            SyncClassDataPacket::decode, SyncDataClientHandler::handle
        );
        handler.registerClientReceiver(
            id("assign_result"), ClassAssignResultPacket.class,
            ClassAssignResultPacket::decode, AssignResultClientHandler::handle
        );
        handler.registerClientReceiver(
            id("player_class_sync"), PlayerClassSyncPacket.class,
            PlayerClassSyncPacket::decode, PlayerClassSyncClientHandler::handle
        );
    }
    
    private static ResourceLocation id(String path) {
        return new ResourceLocation(Archetype.MOD_ID, path);
    }
}
```

---

## Обработчики (handlers)

Серверные обработчики — отдельные классы со static-методом `handle(ServerPlayer player, T packet)`.
Клиентские обработчики — отдельные классы со static-методом `handle(T packet)`.

Каждый серверный обработчик:
1. Логирует получение пакета на TRACE уровне
2. Выполняет валидацию
3. При ошибке валидации — логирует WARN с деталями
4. При успехе — выполняет действие

Каждый клиентский обработчик:
1. Оборачивает логику в `Minecraft.getInstance().execute(() -> { ... })`
2. Обновляет клиентские данные

## Формат вывода

Каждый файл полностью. Порядок:
1. Все пакеты: `ClassSelectPacket`, `AbilityUsePacket`, `AbilityReleasePacket`, `OpenClassSelectionPacket`, `SyncClassDataPacket`, `ClassAssignResultPacket`, `PlayerClassSyncPacket`
2. Серверные обработчики: `ClassSelectHandler`, `AbilityUseHandler`, `AbilityReleaseHandler`
3. Клиентские обработчики: `OpenSelectionClientHandler`, `SyncDataClientHandler`, `AssignResultClientHandler`, `PlayerClassSyncClientHandler`
4. `ClientClassData`, `ClientOtherPlayersData`
5. `NetworkInit`

## Edge cases

- **FriendlyByteBuf overflow**: все строки читать с maxLength, ResourceLocation читать через readResourceLocation (встроенная защита)
- **Клиент отправляет невалидный пакет**: логировать WARN, НЕ кикать (может быть рассинхрон)
- **Дисконнект во время обработки**: все операции идемпотентны
- **Клиентский обработчик вызван до инициализации GUI**: null-check перед доступом к Screen
- **Concurrent sync**: `SyncClassDataPacket` заменяет данные целиком (last-write-wins), не мержит

## Чего НЕ делать

- Не реализуй `ForgeNetworkHandler` / `FabricNetworkHandler` — это платформенные модули
- Не пиши GUI-экраны — только клиентские обработчики, которые их открывают
- Не реализуй `ClassManager` внутри обработчиков — вызывай его методы
