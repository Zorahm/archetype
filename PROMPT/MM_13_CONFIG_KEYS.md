# MM #13 — Конфигурация + Keybinds

## Роль

Java-разработчик. Реализуешь систему конфигурации и привязки клавиш.

## Контекст

Посмотри проект

## Задача

Реализуй:
1. `config/ServerConfig.java` — серверный конфиг
2. `config/ClientConfig.java` — клиентский конфиг
3. `config/ConfigManager.java` — загрузка/сохранение
4. `keybind/ArchetypeKeybinds.java` — регистрация и обработка клавиш (common-часть)

---

## ServerConfig.java

YAML или JSON файл. Хранится в `config/archetype-server.json`. Загружается при старте сервера.

```java
public class ServerConfig {
    // Общее
    public boolean showSelectionOnFirstJoin = true;
    public boolean allowFreeClassChange = false;
    public int classChangeCooldownTicks = 72000; // 1 час
    public boolean resetLevelOnClassChange = false;
    public int maxClassLevel = 20;
    
    // Баланс
    public float globalCooldownMultiplier = 1.0f;
    public float globalAbilityDamageMultiplier = 1.0f;
    public float classExpFromMobKill = 1.0f;
    public float classExpFromOreBreak = 1.0f;
    
    // Ограничения
    public List<String> disabledClasses = new ArrayList<>();
    public List<String> disabledDimensions = new ArrayList<>();
    public List<String> disabledAbilityTypes = new ArrayList<>();
    
    // Права
    public int opLevelForCommands = 2;
    public boolean allowPlayerSelfSelect = true;
}
```

## ClientConfig.java

Хранится в `config/archetype-client.json`. У каждого клиента свой.

```java
public class ClientConfig {
    // Отображение
    public String abilityBarPosition = "hotbar_right"; // hotbar_right, hotbar_left, top_center
    public boolean showResourceBar = true;
    public boolean showClassNameplate = true;
    public float hudScale = 1.0f;
    
    // Анимации
    public boolean enableGuiAnimations = true;
    public float animationSpeed = 1.0f;
    
    // Звуки
    public float abilitySoundVolume = 1.0f;
    public float guiSoundVolume = 0.5f;
}
```

## ConfigManager.java

Простая загрузка/сохранение через Gson:

```java
public class ConfigManager {
    private static ServerConfig serverConfig;
    private static ClientConfig clientConfig;
    
    public static void loadServerConfig(Path configDir);
    public static void loadClientConfig(Path configDir);
    public static void saveServerConfig(Path configDir);
    public static void saveClientConfig(Path configDir);
    
    public static ServerConfig server() { return serverConfig; }
    public static ClientConfig client() { return clientConfig; }
    
    // Горячая перезагрузка (вызывается из /archetype reload)
    public static void reloadServer(Path configDir);
}
```

Загрузка:
1. Если файл существует → прочитать, Gson.fromJson
2. Если не существует → создать дефолтный, сохранить
3. Если JSON невалидный → лог ошибки, использовать дефолтный, НЕ перезаписывать (чтобы юзер мог починить)

Gson настройки: `setPrettyPrinting()`, `serializeNulls()`. Комментарии в JSON не поддерживаются — добавить README.

---

## ArchetypeKeybinds.java

Common-часть обработки клавиш. Регистрация — в платформенных модулях.

```java
public class ArchetypeKeybinds {
    // Определения (не KeyMapping — они платформозависимы, но описания)
    public static final String ABILITY_1 = "key.archetype.ability_1";
    public static final String ABILITY_2 = "key.archetype.ability_2";
    public static final String ABILITY_3 = "key.archetype.ability_3";
    public static final String CLASS_INFO = "key.archetype.class_info";
    public static final String CATEGORY = "key.categories.archetype";
    
    // Дефолтные клавиши
    public static final int DEFAULT_ABILITY_1 = GLFW.GLFW_KEY_R;
    public static final int DEFAULT_ABILITY_2 = GLFW.GLFW_KEY_V;
    public static final int DEFAULT_ABILITY_3 = GLFW.GLFW_KEY_G;
    public static final int DEFAULT_CLASS_INFO = GLFW.GLFW_KEY_J;
    
    /**
     * Вызывается каждый клиентский тик из платформенного EventTranslator.
     * Проверяет нажатия и отправляет пакеты.
     */
    public static void tickKeybinds(KeyMapping ability1, KeyMapping ability2, KeyMapping ability3, KeyMapping classInfo) {
        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // не обрабатывать в GUI
        
        // Ability keys: consumeClick() → отправить AbilityUsePacket
        if (ability1.consumeClick()) {
            sendAbilityUse("ability_1");
        }
        // ... ability_2, ability_3
        
        // Class info: consumeClick() → открыть ClassInfoScreen
        if (classInfo.consumeClick()) {
            mc.setScreen(new ClassInfoScreen());
        }
        
        // Для заряжаемых: проверять isDown() → если была зажата и отпущена → AbilityReleasePacket
    }
    
    private static void sendAbilityUse(String slot) {
        // Клиентская предпроверка (не авторитетная, для UX):
        // - есть ли класс
        // - не на кулдауне ли (клиентский трекер)
        // - хватает ли ресурса (клиентские данные)
        // Если всё ок → NetworkHandler.sendToServer(new AbilityUsePacket(slot))
    }
}
```

### Обработка заряжаемых способностей

```java
// Трекер для charged abilities:
private static String chargingSlot = null;
private static int chargeTicks = 0;

// В tick:
if (chargingSlot != null) {
    if (getKeyForSlot(chargingSlot).isDown()) {
        chargeTicks++;
    } else {
        // Кнопка отпущена
        NetworkHandler.sendToServer(new AbilityReleasePacket(chargingSlot));
        chargingSlot = null;
        chargeTicks = 0;
    }
}
```

## Формат вывода

1. `config/ServerConfig.java`
2. `config/ClientConfig.java`
3. `config/ConfigManager.java`
4. `keybind/ArchetypeKeybinds.java`
