# OPUS #1 — Scaffold проекта + Платформенные абстракции

## Роль

Ты — senior Minecraft mod developer с глубоким опытом Architectury, Forge и Fabric. Пишешь чистый, идиоматичный Java 17 код. Знаешь все подводные камни мультилоадерной разработки.

## Контекст

Мод **Archetype** — система классов для Minecraft 1.20.1+. Мод добавляет классы (Вампир, Голем, Фантом и т.д.) с уникальными способностями, пассивками, ресурсами. Классы определяются через JSON-датапаки. Архитектура мультилоадерная: один исходный код → сборки под Forge и Fabric через Architectury.

## Задача

Создай полный scaffold проекта: Gradle-конфигурацию, структуру пакетов и **все платформенные абстракции** (интерфейсы в common + заглушки в forge/fabric).

## Требования

### 1. Gradle и сборка

Файлы:
- `build.gradle` (корневой)
- `common/build.gradle`
- `forge/build.gradle`
- `fabric/build.gradle`
- `settings.gradle`
- `gradle.properties` (версии: MC 1.20.1, Architectury 9.x, Forge 47.x, Fabric Loader 0.14.x, Fabric API 0.88.x)

Зависимости:
- Architectury API (обязательная, оба лоадера)
- Fabric API (обязательная для fabric)
- Cardinal Components API (для fabric, хранение данных)
- Cloth Config (опциональная, экран настроек)
- Gson (встроена в MC)

### 2. Структура пакетов

```
archetype/
├── common/src/main/java/com/mod/archetype/
│   ├── Archetype.java                  ← константы, MOD_ID, логгер
│   ├── ArchetypeAPI.java               ← публичный API для других модов
│   ├── platform/
│   │   ├── PlatformHelper.java         ← интерфейс платформенных утилит
│   │   ├── PlayerDataAccess.java       ← интерфейс доступа к данным игрока
│   │   └── NetworkHandler.java         ← интерфейс сетевого слоя
│   ├── core/                           ← (заглушка, будет в Opus #2)
│   ├── ability/                        ← (заглушка)
│   ├── condition/                      ← (заглушка)
│   ├── data/                           ← (заглушка)
│   ├── registry/                       ← (заглушка)
│   ├── network/                        ← (заглушка)
│   ├── command/                        ← (заглушка)
│   ├── gui/                            ← (заглушка)
│   ├── config/                         ← (заглушка)
│   ├── item/                           ← (заглушка)
│   └── advancement/                    ← (заглушка)
│
├── common/src/main/resources/
│   ├── archetype.mixins.json           ← если нужны миксины
│   └── data/archetype/                 ← встроенные классы, рецепты
│
├── forge/src/main/java/com/mod/archetype/forge/
│   ├── ArchetypeForge.java             ← @Mod точка входа
│   ├── ForgePlatformHelper.java
│   ├── ForgePlayerDataAccess.java
│   ├── ForgeNetworkHandler.java
│   └── ForgeEventTranslator.java
│
├── fabric/src/main/java/com/mod/archetype/fabric/
│   ├── ArchetypeFabric.java            ← ModInitializer
│   ├── ArchetypeFabricClient.java      ← ClientModInitializer
│   ├── FabricPlatformHelper.java
│   ├── FabricPlayerDataAccess.java
│   ├── FabricNetworkHandler.java
│   └── FabricEventTranslator.java
│
├── fabric/src/main/resources/
│   └── fabric.mod.json
│
└── forge/src/main/resources/
    └── META-INF/mods.toml
```

### 3. Платформенные абстракции (КЛЮЧЕВОЕ)

**PlatformHelper.java** — утилиты платформы:
```java
public interface PlatformHelper {
    boolean isForge();
    boolean isFabric();
    boolean isClient();
    boolean isDedicatedServer();
    Path getConfigDir();
    // ServiceLoader для получения реализации
}
```

**PlayerDataAccess.java** — хранение данных игрока:
```java
public interface PlayerDataAccess {
    PlayerClassData getClassData(Player player);
    void setClassData(Player player, PlayerClassData data);
    // Forge: через Capability
    // Fabric: через Cardinal Components / Data Attachment
}
```

**NetworkHandler.java** — сетевой слой:
```java
public interface NetworkHandler {
    void init();  // регистрация пакетов
    <T> void sendToPlayer(ServerPlayer player, T packet);
    <T> void sendToServer(T packet);
    <T> void sendToTracking(Entity entity, T packet);
    <T> void registerServerReceiver(ResourceLocation id, Class<T> packetClass, 
                                     PacketDecoder<T> decoder, ServerPacketHandler<T> handler);
    <T> void registerClientReceiver(ResourceLocation id, Class<T> packetClass,
                                     PacketDecoder<T> decoder, ClientPacketHandler<T> handler);
}
```

**EventTranslator** — маппинг платформенных событий на общие обработчики (абстракцию не нужно, каждый лоадер напрямую вызывает общие методы).

### 4. ServiceLoader паттерн

Для получения реализаций в common используй `ServiceLoader` (стандартный подход Architectury):

```java
// В common:
PlatformHelper helper = ServiceLoader.load(PlatformHelper.class)
    .findFirst().orElseThrow();
```

С файлами:
- `forge/src/main/resources/META-INF/services/com.mod.archetype.platform.PlatformHelper`
- `fabric/src/main/resources/META-INF/services/com.mod.archetype.platform.PlatformHelper`

Аналогично для `PlayerDataAccess` и `NetworkHandler`.

### 5. ArchetypeAPI.java

Публичный фасад для других модов:

```java
public final class ArchetypeAPI {
    // Получение данных
    PlayerClass getPlayerClass(Player player);
    boolean hasClass(Player player, ResourceLocation classId);
    int getClassLevel(Player player);
    
    // Манипуляция (серверная сторона)
    void assignClass(ServerPlayer player, ResourceLocation classId);
    void removeClass(ServerPlayer player);
    
    // Расширяемость
    void registerAbilityType(ResourceLocation id, AbilityFactory<?> factory);
    void registerConditionType(ResourceLocation id, ConditionFactory factory);
    void registerPassiveType(ResourceLocation id, PassiveFactory factory);
    
    // Реестр
    Collection<PlayerClass> getAllClasses();
    Optional<PlayerClass> getClass(ResourceLocation id);
}
```

Реализация делегирует внутренним менеджерам. Методы с `@Nullable` и Javadoc.

### 6. Точки входа

**Archetype.java** (common):
- `public static final String MOD_ID = "archetype";`
- `public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);`
- Метод `init()` — вызывается из обеих точек входа
- Метод `initClient()` — клиентская инициализация

**ArchetypeForge.java:**
- `@Mod(Archetype.MOD_ID)`, конструктор вызывает `Archetype.init()`
- Подписка на события через `MinecraftForge.EVENT_BUS`

**ArchetypeFabric.java:**
- `implements ModInitializer`, `onInitialize()` → `Archetype.init()`

## Формат вывода

Выдай каждый файл полностью. Формат:

```
=== path/to/File.java ===
(полный код файла)
```

Порядок:
1. gradle.properties, settings.gradle, build.gradle (root)
2. common/build.gradle, forge/build.gradle, fabric/build.gradle
3. Archetype.java, ArchetypeAPI.java
4. Все интерфейсы из platform/
5. Forge точка входа + ServiceLoader файлы
6. Fabric точка входа + ServiceLoader файлы
7. fabric.mod.json, mods.toml

## Чего НЕ делать

- Не реализуй внутренние модули (core, ability и т.д.) — только пакеты-заглушки с package-info.java
- Не пиши GUI, команды, способности — это другие промпты
- Не добавляй TODO-комментарии к заглушкам — просто пустые пакеты
- Не используй Architectury @ExpectPlatform если можно обойтись ServiceLoader
