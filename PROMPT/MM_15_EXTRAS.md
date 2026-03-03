# MM #15 — Адвансменты, Свиток Перерождения, Примеры JSON-классов

## Роль

Java-разработчик + контент-автор Minecraft модов. Реализуешь финальные элементы мода.

## Контекст

Посмотри проект

## Задача

Три блока:
1. Адвансменты (достижения) + кастомный триггер
2. Свиток Перерождения (предмет)
3. Примеры JSON-классов (3 полных класса)

---

## 1. Адвансменты

### Кастомный триггер

`advancement/ClassActionTrigger.java` — extends `SimpleCriterionTrigger<ClassActionTrigger.TriggerInstance>`:

```java
public class ClassActionTrigger extends SimpleCriterionTrigger<ClassActionTrigger.TriggerInstance> {
    public static final ResourceLocation ID = new ResourceLocation(Archetype.MOD_ID, "class_action");
    
    // TriggerInstance хранит условия:
    // - action_type: "select", "change", "level_up", "ability_use", "max_level", "try_all"
    // - class_id: конкретный ID класса или null (любой)
    // - ability_id: конкретная способность или null
    // - level: минимальный уровень или 0
    
    // trigger(ServerPlayer, String actionType, ResourceLocation classId, ResourceLocation abilityId, int level)
}
```

Регистрация: `CriteriaTriggers.register(new ClassActionTrigger())`

Вызовы триггера из ClassManager:
- `assignClass` → trigger("select", classId, null, 0) + если triedClasses.size() == allClasses.size() → trigger("try_all")
- `removeClass` + assignClass → trigger("change", newClassId, null, 0)
- `levelUp` → trigger("level_up", classId, null, newLevel) + если maxLevel → trigger("max_level")
- `useAbility` → trigger("ability_use", classId, abilityId, 0)

### JSON-файлы адвансментов

Расположение: `data/archetype/advancements/`

```
archetype/
  advancements/
    root.json                 ← "Начало пути" — выбрать первый класс
    rebirth.json              ← "Перерождение" — сменить класс
    master.json               ← "Мастер" — достичь макс. уровня
    try_all.json              ← "Испытать всё" — попробовать каждый класс
    first_blood.json          ← "Первая кровь" — использовать активную способность
    perfect_storm.json        ← "Идеальный шторм" — 3 способности за 10 секунд
    on_the_edge.json          ← "На пределе" — способность при ресурсе < 5%
```

Пример `root.json`:
```json
{
  "display": {
    "icon": { "item": "minecraft:nether_star" },
    "title": { "translate": "advancements.archetype.root.title" },
    "description": { "translate": "advancements.archetype.root.description" },
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true
  },
  "criteria": {
    "select_class": {
      "trigger": "archetype:class_action",
      "conditions": {
        "action_type": "select"
      }
    }
  }
}
```

Для "Идеальный шторм" (3 способности за 10 секунд) — требует кастомной логики:
- Трекать в PlayerClassData: `lastAbilityUseTimes` (List<Long>)
- При каждом ability_use: добавить текущий тик, удалить старше 200 тиков
- Если размер ≥ 3 → trigger("perfect_storm")

Для "На пределе" — проверка при ability_use: `resourceCurrent / resourceMax < 0.05`.

---

## 2. Свиток Перерождения

### RebirthScrollItem.java

```java
public class RebirthScrollItem extends Item {
    public RebirthScrollItem() {
        super(new Item.Properties()
            .rarity(Rarity.EPIC)         // фиолетовое название
            .stacksTo(1)                 // не стакается
            .fireResistant()             // не горит
        );
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Серверная сторона:
            // 1. Проверить кулдаун смены класса
            // 2. Отправить OpenClassSelectionPacket(mode=1) — режим перерождения
            // Предмет НЕ тратится здесь — тратится при подтверждении выбора (в ClassSelectHandler)
            
            PlayerClassData data = /* получить через PlayerDataAccess */;
            if (!data.hasClass()) {
                // Нет класса — нельзя использовать (перерождение бессмысленно)
                player.displayClientMessage(Component.translatable("item.archetype.rebirth_scroll.no_class"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            
            if (!data.canChangeClass(level.getGameTime(), ConfigManager.server().classChangeCooldownTicks)) {
                player.displayClientMessage(Component.translatable("item.archetype.rebirth_scroll.cooldown"), true);
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            
            NetworkHandler.sendToPlayer(serverPlayer, new OpenClassSelectionPacket((byte) 1));
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // зачарованный блеск
    }
    
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.archetype.rebirth_scroll.tooltip")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
```

### Регистрация предмета

```java
// В Archetype.java или отдельном ItemRegistry.java:
public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Archetype.MOD_ID, Registries.ITEM);
public static final RegistrySupplier<Item> REBIRTH_SCROLL = ITEMS.register("rebirth_scroll", RebirthScrollItem::new);
```

### Рецепт крафта

`data/archetype/recipes/rebirth_scroll.json`:
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    " N ",
    "EPE",
    " N "
  ],
  "key": {
    "N": { "item": "minecraft:nether_star" },
    "E": { "item": "minecraft:ender_eye" },
    "P": { "item": "minecraft:paper" }
  },
  "result": {
    "item": "archetype:rebirth_scroll",
    "count": 1
  }
}
```

---

## 3. Примеры JSON-классов

### Вампир (archetype:vampire)

Файл: `data/archetype/archetype_classes/vampire.json`

Полный JSON с:
- Атрибуты: HP −4, Attack +1, Speed +15%
- Условные: ночью Attack +2, Speed +10%
- Пассивки: sun_damage, night_vision, food_restriction (только сырое мясо), undead_type, lifesteal (20%)
- Активные: blood_drain (slot 1, CD 5s), morph bat (slot 2, CD 30s, fly, small, no attack), targeted_effect hypnosis (slot 3, CD 60s, unlock lvl 10, slowness + weakness)
- Ресурс: Blood, max 100, start 100, drain 0.1/s, regen 0, color #8B0000

### Голем (archetype:golem)

- Атрибуты: HP +10, Attack +2, Speed −20%, Knockback Resistance +0.5
- Пассивки: sink_in_water, no_fall_damage, fire_immunity, natural_regen_disabled
- Активные: timed_buff Stone Skin (slot 1, CD 20s, Resistance 2 на 10s), area_attack Ground Slam (slot 2, CD 15s, 8 dmg, radius 4, knockback), toggle Fortification (slot 3, Resistance 3 + Slowness 2, drain ресурса)
- Ресурс: Endurance, max 80, start 80, drain 0, regen 2/s, color #808080

### Фантом (archetype:phantom)

- Атрибуты: HP −6, Speed +30%
- Пассивки: no_fall_damage, slow_fall, wall_climb
- Активные: dash Shadow Step (slot 1, CD 8s, speed 4, 20 no_fall_ticks), teleport Phase Shift (slot 2, CD 25s, range 20), toggle Ethereal Form (slot 3, partial invisibility + reduced damage taken, drain)
- Ресурс: Essence, max 60, start 60, drain 0.2/s, regen 0.5/s, color #9B59B6

### Файлы переводов

`assets/archetype/lang/en_us.json` — все ключи для 3 классов + UI + команды + предметы.

`assets/archetype/lang/ru_ru.json` — русская локализация.

## Формат вывода

1. `advancement/ClassActionTrigger.java`
2. 7 JSON-файлов адвансментов
3. `item/RebirthScrollItem.java`
4. Регистрация предмета (фрагмент)
5. `recipes/rebirth_scroll.json`
6. `archetype_classes/vampire.json`
7. `archetype_classes/golem.json`
8. `archetype_classes/phantom.json`
9. `lang/en_us.json`
10. `lang/ru_ru.json`
