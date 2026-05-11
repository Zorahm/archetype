# Создание своих классов — Archetype

Этот гайд для тех, кто хочет добавить собственный класс без написания кода.

---

## Быстрый старт

1. Запусти Minecraft хотя бы один раз с модом — он сам создаст папку с примерами
2. Найди папку: `.minecraft/config/archetype/classes/`
3. Скопируй любой готовый файл (например `vi.json`) и переименуй: `мой_класс.json`
4. Отредактируй содержимое
5. В игре введи `/archetype reload` — класс появится сразу, без перезапуска

> Имя файла = ID класса. `vampire.json` → ID будет `archetype:vampire`

---

## Структура файла

```json
{
  "name": "Название класса",
  "description": "Короткое описание для GUI",
  "icon": "archetype:textures/gui/class/vi.png",
  "color": "8800AA",
  "category": "DAMAGE",
  "lore": ["Строка флейвор-текста"],
  "size_modifier": 1.0,
  "attributes": [ ... ],
  "passive_abilities": [ ... ],
  "active_abilities": [ ... ],
  "ability_stats": [ ... ],
  "progression": [ ... ]
}
```

---

## Поля верхнего уровня

| Поле | Тип | Описание |
|------|-----|----------|
| `name` | строка | Название класса в GUI |
| `description` | строка | Описание (1–2 предложения) |
| `icon` | строка | Иконка. Можно использовать иконки из мода (список ниже) |
| `color` | строка | HEX-цвет рамки карточки, без `#`. Например `CC2200` |
| `category` | строка | Роль: `DAMAGE`, `TANK`, `MOBILITY`, `UTILITY` |
| `lore` | массив строк | Атмосферный текст на экране выбора |
| `size_modifier` | число | Масштаб хитбокса. `1.0` = норма, `1.25` = крупнее, `0.8` = меньше |

---

## Атрибуты

Изменяют характеристики игрока при смене класса.

```json
"attributes": [
  {
    "attribute": "minecraft:generic.max_health",
    "operation": "ADDITION",
    "value": -4.0
  },
  {
    "attribute": "minecraft:generic.movement_speed",
    "operation": "MULTIPLY_BASE",
    "value": 0.2
  }
]
```

### Доступные атрибуты

| ID атрибута | Что меняет |
|-------------|-----------|
| `minecraft:generic.max_health` | Максимальное здоровье (в хп: 1 сердце = 2 единицы) |
| `minecraft:generic.attack_damage` | Урон в ближнем бою |
| `minecraft:generic.attack_speed` | Скорость удара |
| `minecraft:generic.movement_speed` | Скорость передвижения |
| `minecraft:generic.armor` | Броня |
| `minecraft:generic.armor_toughness` | Стойкость брони |
| `minecraft:generic.knockback_resistance` | Сопротивление отбросу (0.0–1.0) |
| `minecraft:generic.luck` | Удача |
| `minecraft:player.block_interaction_range` | Дальность взаимодействия с блоками |
| `minecraft:player.entity_interaction_range` | Дальность атаки |

### Операции

| Операция | Смысл | Пример с `value: 0.2` |
|----------|-------|-----------------------|
| `ADDITION` | Прибавить/убавить фиксированное число | `max_health` +0.2 хп |
| `MULTIPLY_BASE` | Умножить базовое значение | движение +20% |
| `MULTIPLY_TOTAL` | Умножить итоговое значение (после всех бонусов) | итоговый урон +20% |

> **Совет:** 1 сердце = 2 единицы `max_health`.
> Хочешь дать +3 сердца → `"value": 6.0`

---

## Пассивные способности

Работают постоянно, пока у игрока этот класс.

```json
"passive_abilities": [
  {
    "type": "archetype:lifesteal",
    "params": {
      "percent_of_damage": 0.15,
      "only_melee": true
    },
    "positive": true,
    "name": "Кражей крови",
    "description": "Восстанавливает 15% урона атакой"
  }
]
```

Поле `positive: true` — бонус (зелёная иконка в GUI), `false` — штраф (красная).

---

### `archetype:lifesteal` — Вампиризм
Восстанавливает здоровье от атак.
```json
"params": {
  "percent_of_damage": 0.2,
  "only_melee": true
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `percent_of_damage` | `0.2` | Доля от урона, идущая в лечение (0.0–1.0) |
| `only_melee` | `true` | `true` = только ближний бой, `false` = любой урон |

---

### `archetype:thorns_passive` — Шипы
Отражает часть урона обратно атакующему.
```json
"params": {
  "reflect_percent": 0.2
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `reflect_percent` | `0.15` | Доля отражаемого урона (0.0–1.0) |

---

### `archetype:jump_boost` — Усиленный прыжок
Постоянный буф прыжка.
```json
"params": {
  "multiplier": 1.0
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `multiplier` | `0.5` | Сила прыжка: `0.5` = Jump Boost I, `1.0` = II, `1.5` = III |

---

### `archetype:slow_fall` — Замедленное падение
Игрок падает медленно (как с пером невесомости). Параметров нет.

---

### `archetype:no_fall_damage` — Нет урона от падения
Игрок не получает урона от падения. Параметров нет.

---

### `archetype:fire_immunity` — Иммунитет к огню
Игрок не горит и не получает урона от огня/лавы. Параметров нет.

---

### `archetype:water_vulnerability` — Уязвимость к воде
Вода наносит урон игроку. Параметров нет.

---

### `archetype:sink_in_water` — Тонет в воде
Игрок не может плавать — сразу идёт ко дну. Параметров нет.

---

### `archetype:breath_underwater` — Дыхание под водой
Бесконечный воздух под водой. Параметров нет.

---

### `archetype:night_vision` — Ночное зрение
Постоянное ночное зрение. Параметров нет.

---

### `archetype:sun_damage` — Урон от солнца
Игрок горит под открытым дневным небом (как зомби). Параметров нет.

---

### `archetype:undead_type` — Нежить
Игрок считается нежитью: исцеление наносит урон, яд лечит. Параметров нет.

---

### `archetype:natural_regeneration_disabled` — Без регена
Отключает естественное восстановление здоровья от еды. Параметров нет.

---

### `archetype:wall_climb` — Лазание по стенам
Игрок может карабкаться по вертикальным поверхностям. Параметров нет.

---

### `archetype:mob_neutral` — Нейтралитет с мобами
Мобы не агрятся на игрока первыми.
```json
"params": {
  "mode": "whitelist",
  "mob_types": ["minecraft:zombie", "minecraft:skeleton"],
  "radius": 16.0
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `mode` | `"whitelist"` | `"whitelist"` — нейтральны только указанные мобы; `"blacklist"` — нейтральны все, **кроме** указанных |
| `mob_types` | `[]` | Список ID существ. Пустой список при `whitelist` = все мобы нейтральны; пустой при `blacklist` = никто не нейтрален |
| `radius` | `16.0` | Радиус проверки в блоках |

**Примеры:**
```json
// Все мобы нейтральны (старое поведение)
"params": {}

// Только зомби и скелеты не атакуют
"params": { "mode": "whitelist", "mob_types": ["minecraft:zombie", "minecraft:skeleton"] }

// Все нейтральны, кроме эндерменов и криперов
"params": { "mode": "blacklist", "mob_types": ["minecraft:enderman", "minecraft:creeper"] }
```

---

### `archetype:magnetic_pull` — Магнитное притяжение
Подбирает предметы с земли на расстоянии. Параметров нет.

---

### `archetype:fire_attack` — Огненные удары
Каждый удар поджигает цель. Параметров нет.

---

### `archetype:no_sneak` — Нельзя красться
Игрок не может использовать режим крадения (Shift). Параметров нет.

---

### `archetype:destroy_holy_items` — Уничтожение святых предметов
Золотые предметы и тотемы исчезают из инвентаря. Параметров нет.

---

### `archetype:shield_block` — Без щита
Игрок не может использовать щит. Параметров нет.

---

### `archetype:tool_fragility` — Хрупкие инструменты
Инструменты ломаются быстрее. Параметров нет.

---

### `archetype:effect_immunity` — Иммунитет к эффектам
Постоянно снимает указанные эффекты.
```json
"params": {
  "effects": ["minecraft:poison", "minecraft:wither"]
}
```
| Параметр | Описание |
|----------|----------|
| `effects` | Массив ID эффектов (см. таблицу ID эффектов в конце гайда) |

---

### `archetype:custom_diet` — Особая диета
Только указанные предметы дают сытость. Всё остальное вызывает голод и тошноту.
```json
"params": {
  "food_items": ["minecraft:rotten_flesh", "minecraft:spider_eye"],
  "food_value": 4,
  "saturation": 0.3
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `food_items` | — | Разрешённые предметы. Формат: `"minecraft:item_id"` |
| `food_value` | `6` | Единицы голода, которые восстанавливает |
| `saturation` | `0.6` | Насыщение (0.0–1.0) |

---

### `archetype:villager_rejection` — Отвергнут деревенскими
Жители отказываются торговать с игроком. Параметров нет.

---

### `archetype:potion_block` — Без зелий
Игрок не может использовать зелья. Параметров нет.

---

### `archetype:xp_attribute_scaling` — Рост с уровнем
Постепенно снимает штрафы атрибутов по мере роста уровня класса. Используй вместе с отрицательными атрибутами, чтобы компенсировать их при прокачке.
```json
"params": {
  "target_level": 40,
  "health_bonus": 6.0,
  "attack_speed_bonus": 0.2,
  "attack_damage_bonus": 0.2
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `target_level` | `40` | Уровень, на котором бонус раскрывается полностью |
| `health_bonus` | `0` | Максимальный бонус к здоровью (единицы хп, ADDITION) |
| `attack_speed_bonus` | `0` | Максимальный бонус к скорости атаки (MULTIPLY_BASE) |
| `attack_damage_bonus` | `0` | Максимальный бонус к урону (MULTIPLY_BASE) |

---

## Активные способности

Используются клавишами R, V, G (настраиваются в управлении). Максимум 3 слота.

```json
"active_abilities": [
  {
    "type": "archetype:dash",
    "slot": "ability_1",
    "cooldown": 100,
    "resource_cost": 0,
    "unlock_level": 0,
    "params": {
      "dash_speed": 3.0,
      "no_fall_ticks": 40,
      "damage": 4.0
    },
    "name": "Рывок",
    "description": "Мощный рывок вперёд",
    "icon": "archetype:textures/gui/ability/dash.png"
  }
]
```

| Поле | Описание |
|------|----------|
| `type` | Тип способности (см. ниже) |
| `slot` | `ability_1`, `ability_2` или `ability_3` |
| `cooldown` | Кулдаун в тиках (20 тиков = 1 секунда). `0` = без кулдауна |
| `resource_cost` | Стоимость в единицах ресурса класса |
| `unlock_level` | Минимальный уровень для разблокировки. `0` = сразу |
| `name` | Название в GUI |
| `description` | Описание в GUI |
| `icon` | Иконка |

---

### `archetype:dash` — Рывок
Бросает игрока в направлении взгляда.
```json
"params": {
  "dash_speed": 3.0,
  "no_fall_ticks": 40,
  "damage": 0.0,
  "fire_trail": false,
  "knockback_strength": 0.0
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `dash_speed` | `3.0` | Скорость броска |
| `no_fall_ticks` | `40` | Тики без урона от падения после рывка |
| `damage` | `0` | Урон по сущностям на пути |
| `fire_trail` | `false` | Оставлять огонь за собой |
| `knockback_strength` | `0` | Отбрасывание сущностей на пути |

---

### `archetype:teleport` — Телепорт
Телепортирует к блоку, на который смотрит игрок.
```json
"params": {
  "range": 30.0,
  "require_safe_landing": true,
  "enderpearl_damage": false
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `range` | `30.0` | Максимальная дальность в блоках |
| `require_safe_landing` | `true` | Требовать свободное место для приземления |
| `enderpearl_damage` | `false` | Наносить 1 урон при телепорте (как эндер-жемчуг) |

---

### `archetype:area_attack` — Удар по площади
Наносит урон всем существам вокруг игрока.
```json
"params": {
  "damage": 6.0,
  "radius": 5.0,
  "knockback_strength": 1.0
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `damage` | `6.0` | Урон |
| `radius` | `5.0` | Радиус в блоках |
| `knockback_strength` | `1.0` | Сила отбрасывания |

---

### `archetype:timed_buff` — Временный бафф
Накладывает эффекты на игрока на время.
```json
"params": {
  "duration_ticks": 200,
  "effects": [
    { "effect": "minecraft:strength", "amplifier": 1 },
    { "effect": "minecraft:speed",    "amplifier": 0 }
  ]
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `duration_ticks` | `200` | Длительность в тиках (20 тиков = 1 сек) |
| `effects[].effect` | — | ID эффекта |
| `effects[].amplifier` | `0` | `0` = уровень I, `1` = уровень II, `2` = уровень III |

---

### `archetype:targeted_effect` — Эффект на цель
Накладывает эффекты на существо, на которое смотришь.
```json
"params": {
  "range": 10.0,
  "damage": 0.0,
  "effects": [
    { "effect": "minecraft:slowness", "duration": 100, "amplifier": 2 }
  ]
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `range` | `10.0` | Дальность прицеливания |
| `damage` | `0` | Дополнительный урон при попадании |
| `effects[].duration` | `200` | Длительность эффекта в тиках |
| `effects[].amplifier` | `0` | Уровень эффекта |

---

### `archetype:projectile` — Снаряд
Выпускает снаряд в направлении взгляда.
```json
"params": {
  "speed": 1.5,
  "damage": 8.0,
  "gravity": false,
  "explosion_radius": 0.0,
  "pierce": 0
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `speed` | `1.5` | Скорость снаряда |
| `damage` | `8.0` | Урон при попадании |
| `gravity` | `false` | Подчиняться гравитации |
| `explosion_radius` | `0` | Радиус взрыва при попадании (0 = нет взрыва) |
| `pierce` | `0` | Сколько существ снаряд пробивает насквозь |

---

### `archetype:self_heal` — Самолечение
Восстанавливает здоровье игрока.
```json
"params": {
  "amount": 0.0,
  "percent": 0.3,
  "remove_effects": ["minecraft:poison"]
}
```
| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `amount` | `0` | Фиксированное количество хп (если задано — приоритет над `percent`) |
| `percent` | `0.3` | Доля от максимального хп |
| `remove_effects` | — | Эффекты, снимаемые при лечении |

---

### `archetype:random_teleport` — Случайный телепорт
Телепортирует в случайном направлении. Параметров нет.

---

### `archetype:antigravity_throw` — Антигравитационный бросок
Подбрасывает ближайших врагов в воздух. Параметров нет.

---

### `archetype:evoker_fangs` — Клыки призывателя
Вызывает клыки из земли вокруг цели. Параметров нет.

---

### `archetype:chase_teleport` — Телепорт к цели
Телепортируется к противнику, на которого смотришь. Параметров нет.

---

### `archetype:blood_drain` — Кража жизни
Высасывает здоровье из цели. Параметров нет.

---

## Команды (ванильная интеграция)

Позволяют использовать любые ванильные команды как часть класса — без написания Java-кода. Удобно для тех, кто умеет в командные блоки.

```json
"commands": [
  { "trigger": "on_assign", "command": "effect give {player} minecraft:night_vision 99999 0 true" },
  { "trigger": "on_remove", "command": "effect clear {player} minecraft:night_vision" },
  { "trigger": "on_tick",   "command": "particle minecraft:flame {player}", "interval": 40 },
  { "trigger": "on_death",  "command": "say {player} погиб как герой" },
  { "trigger": "on_respawn","command": "effect give {player} minecraft:resistance 100 1 true" },
  { "trigger": "on_level_up","command": "title {player} actionbar {\"text\":\"Уровень {level}!\",\"color\":\"gold\"}" }
]
```

### Триггеры

| Триггер | Когда срабатывает |
|---------|------------------|
| `on_assign` | При получении класса |
| `on_remove` | При снятии класса (смена, команда) |
| `on_tick` | Каждые N тиков (настраивается полем `interval`) |
| `on_death` | При смерти игрока |
| `on_respawn` | При возрождении |
| `on_level_up` | При повышении уровня класса |

### Поля

| Поле | Описание |
|------|----------|
| `trigger` | Триггер из таблицы выше |
| `command` | Команда без `/`. Поддерживает плейсхолдеры |
| `interval` | Только для `on_tick`: интервал в тиках. По умолчанию `20` (1 сек) |

### Плейсхолдеры

| Плейсхолдер | Что подставляется |
|-------------|------------------|
| `{player}` | Имя игрока |
| `{level}` | Текущий уровень класса (удобно для `on_level_up`) |

### Как работают команды

- `@s` в команде указывает на **игрока** (не на сервер)
- Права уровня ОП (4) — работают все команды, в том числе `attribute`, `effect`, `title`, `scoreboard`
- Команда выполняется **на сервере**, поэтому серверные команды вроде `/particle` работают только если сервер их обрабатывает

### Примеры использования

**Добавить тег при выборе класса:**
```json
{ "trigger": "on_assign", "command": "tag {player} add is_vampire" }
{ "trigger": "on_remove", "command": "tag {player} remove is_vampire" }
```

**Эффект при возрождении:**
```json
{ "trigger": "on_respawn", "command": "effect give {player} minecraft:resistance 200 1 true" }
```

**Повторяющийся эффект каждые 5 секунд:**
```json
{ "trigger": "on_tick", "command": "effect give {player} minecraft:night_vision 21 0 true", "interval": 100 }
```

**Титул при повышении уровня:**
```json
{ "trigger": "on_level_up", "command": "title {player} actionbar {\"text\":\"Уровень {level}!\",\"color\":\"gold\"}" }
```

**Модификатор атрибута через ваниль:**
```json
{ "trigger": "on_assign", "command": "attribute {player} minecraft:generic.luck modifier add 11111111-1111-1111-1111-111111111111 archetype.luck 5 add" },
{ "trigger": "on_remove", "command": "attribute {player} minecraft:generic.luck modifier remove 11111111-1111-1111-1111-111111111111" }
```

> **Совет:** UUID в команде `attribute` должен быть уникальным для каждого модификатора каждого класса. Придумай свой и не меняй его — иначе `on_remove` не сможет удалить старый.

---

## Прогрессия класса

Отображается в GUI как история прокачки. Чисто текстовое, механику не меняет.

```json
"progression": [
  { "level": 10, "key": "Открывается второй заряд рывка" },
  { "level": 25, "key": "Рывок начинает поджигать врагов" },
  { "level": 50, "key": "Скорость рывка максимальна" }
]
```

---

## Статистика способностей

Показывает числовые характеристики в GUI и как они растут с уровнем.

```json
"ability_stats": [
  {
    "name": "Урон рывка",
    "base": 4,
    "format": "int",
    "bonuses": [
      { "level": 10, "value": 2 },
      { "level": 30, "value": 2 }
    ]
  }
]
```

| Параметр | Описание |
|----------|----------|
| `name` | Название строки в GUI |
| `base` | Базовое значение |
| `format` | `int` — целое число, `seconds` — секунды, `boolean` — да/нет |
| `bonuses` | Прибавки на определённых уровнях |

---

## Готовый пример — класс «Лесной Страж»

```json
{
  "name": "Лесной Страж",
  "description": "Дух леса. Силён в чаще, уязвим у огня.",
  "icon": "archetype:textures/gui/class/vi.png",
  "color": "2D7A2D",
  "category": "UTILITY",
  "lore": ["Лес помнит каждый твой шаг."],
  "size_modifier": 1.0,

  "attributes": [
    { "attribute": "minecraft:generic.max_health",    "operation": "ADDITION",      "value":  4.0 },
    { "attribute": "minecraft:generic.movement_speed","operation": "MULTIPLY_BASE", "value":  0.15 },
    { "attribute": "minecraft:generic.attack_damage", "operation": "MULTIPLY_BASE", "value": -0.2 }
  ],

  "passive_abilities": [
    {
      "type": "archetype:mob_neutral",
      "params": {},
      "positive": true,
      "name": "Дух леса",
      "description": "Лесные мобы не атакуют первыми"
    },
    {
      "type": "archetype:slow_fall",
      "params": {},
      "positive": true,
      "name": "Мягкое падение",
      "description": "Падает медленно, как лист"
    },
    {
      "type": "archetype:sun_damage",
      "params": {},
      "positive": false,
      "name": "Страх солнца",
      "description": "Горит под открытым небом днём"
    }
  ],

  "active_abilities": [
    {
      "type": "archetype:teleport",
      "slot": "ability_1",
      "cooldown": 200,
      "resource_cost": 0,
      "unlock_level": 0,
      "params": {
        "range": 25.0,
        "require_safe_landing": true,
        "enderpearl_damage": false
      },
      "name": "Лесной шаг",
      "description": "Телепорт к точке в лесу",
      "icon": "archetype:textures/gui/ability/dash.png"
    },
    {
      "type": "archetype:self_heal",
      "slot": "ability_2",
      "cooldown": 400,
      "resource_cost": 0,
      "unlock_level": 10,
      "params": {
        "percent": 0.4
      },
      "name": "Исцеление природы",
      "description": "Восстанавливает 40% здоровья",
      "icon": "archetype:textures/gui/ability/dash.png"
    }
  ],

  "ability_stats": [
    {
      "name": "Дальность шага",
      "base": 25,
      "format": "int",
      "bonuses": [
        { "level": 20, "value": 5 },
        { "level": 40, "value": 5 }
      ]
    }
  ],

  "progression": [
    { "level": 10, "key": "Открывается Исцеление природы" },
    { "level": 20, "key": "Дальность телепорта возрастает" },
    { "level": 40, "key": "Телепорт достигает максимума" }
  ]
}
```

---

## Частые ошибки

**Класс не появляется после `/archetype reload`**
- Проверь JSON на ошибки: [jsonlint.com](https://jsonlint.com)
- Убедись, что файл в `.minecraft/config/archetype/classes/`
- Расширение должно быть `.json`, а не `.json.txt`

**Неправильные значения здоровья**
`max_health` считается в единицах хп, не в сердцах. 1 сердце = 2 единицы.
Хочешь дать +3 сердца → `"value": 6.0`

**Запятые в JSON**
Каждый элемент в массиве и каждое поле объекта разделяются запятой, **кроме последнего**.

**Неверный ID способности**
Пиши `"archetype:dash"`, а не просто `"dash"`.

---

## Иконки из мода

```
archetype:textures/gui/ability/dash.png
archetype:textures/gui/class/vi.png
archetype:textures/gui/class/ram.png
archetype:textures/gui/class/lin_qi.png
archetype:textures/gui/class/ru_yi.png
```

---

## ID эффектов Minecraft

| Эффект | ID |
|--------|----|
| Сила | `minecraft:strength` |
| Скорость | `minecraft:speed` |
| Замедление | `minecraft:slowness` |
| Прыжок | `minecraft:jump_boost` |
| Регенерация | `minecraft:regeneration` |
| Сопротивление | `minecraft:resistance` |
| Яд | `minecraft:poison` |
| Иссушение | `minecraft:wither` |
| Слабость | `minecraft:weakness` |
| Голод | `minecraft:hunger` |
| Ночное зрение | `minecraft:night_vision` |
| Невидимость | `minecraft:invisibility` |
| Тошнота | `minecraft:confusion` |
| Огнестойкость | `minecraft:fire_resistance` |
| Водное дыхание | `minecraft:water_breathing` |
| Левитация | `minecraft:levitation` |
