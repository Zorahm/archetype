<div align="center">
<p align="right">
  <a href="README.md">English</a> | <b>Русский</b>
</p>

<img src="icon.png" width="128" height="128" alt="Archetype Icon">

# ⚔ Archetype

### Система классов для Minecraft

![MC](https://img.shields.io/badge/Minecraft-1.21.11-62b447?style=flat-square&logo=minecraft&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric-c9b88a?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square)

*Каждый бонус компенсирован штрафом. Нет идеального класса — только твой выбор.*

</div>

---

## Что это

Archetype добавляет систему классов, которая меняет правила игры для каждого игрока. Классы переписывают характеристики, дают уникальные активные и пассивные способности, накладывают ограничения.

**Главный принцип — баланс через компромисс:**

| Класс | Сила | Слабость |
|-------|------|----------|
| 🧛 Ви | Высокий урон, крит-механика | Уязвим без накопленного ресурса |
| 🗿 Рам | Огонь, ярость, живучесть | Ограниченная мобильность |
| 🌀 Лин Ци | Хаотичная магия | Нестабильные эффекты |
| ☯ Ру И | Ресурсный баланс | Зависимость от состояния |
| 🦷 Призыватель | Призыв клыков (линия / цель / круг) | Ест только мясо, жители не торгуют |
| 🎭 Морф | Трансформация в 5 форм с уникальными бонусами | Ослаблен и замедлен без активной формы |

Классы задаются JSON-файлами — серверы добавляют свои через датапаки без единой строки Java.

---

## Архитектура

Fabric-мод с общим ядром и тонким платформо-специфичным слоем:

```
archetype/
├── common/     95% кода — ядро, способности, сеть, GUI, команды
└── fabric/     Fabric: Data Attachments, Networking API, события
```

Платформо-зависимый код изолирован через ServiceLoader (`NetworkHandler`, `PlayerDataAccess`, `PlatformHelper`).

### Модули

| Модуль | Назначение |
|--------|-----------|
| `core/` | `ClassManager` — жизненный цикл. `PlayerClass` — определение класса |
| `ability/` | 25 пассивных + 14 активных типов. Фабрики, интерфейсы, реестр |
| `condition/` | 12 типов условий + комбинаторы `and` / `or` / `not` |
| `data/` | `PlayerClassData` — состояние игрока, NBT-сериализация |
| `network/` | 8 пакетов, серверная валидация, клиентская синхронизация |
| `registry/` | `ClassRegistry` — JSON-датапаки через `SimpleJsonResourceReloadListener` |
| `gui/` | Экран выбора, досье, HUD-оверлей |
| `command/` | `/archetype set/remove/get/list/reload` |
| `item/` | Свиток Перерождения — смена класса в руках |
| `config/` | Серверный и клиентский конфиг |
| `advancement/` | Достижения за классы и способности |

---

## Формат класса (JSON)

Новый класс — один файл в `data/<namespace>/archetype_classes/<class_id>.json`. ID класса — `<namespace>:<имя_файла>`.

```json
{
  "name": "class.mypack.warrior.name",
  "description": "class.mypack.warrior.description",
  "icon": "mypack:textures/gui/class/warrior.png",
  "color": "8B0000",
  "category": "DAMAGE",
  "attributes": [
    { "attribute": "minecraft:max_health", "operation": "ADDITION", "value": 4.0 }
  ],
  "passive_abilities": [
    { "type": "archetype:lifesteal", "positive": true, "params": { "fraction": 0.1 },
      "name": "passive.mypack.warrior.lifesteal.name",
      "description": "passive.mypack.warrior.lifesteal.description" }
  ],
  "active_abilities": [
    { "type": "archetype:dash", "slot": "ability_1", "cooldown": 100,
      "params": { "dash_speed": 1.5, "damage": 4.0 },
      "name": "ability.mypack.warrior.dash.name",
      "description": "ability.mypack.warrior.dash.description",
      "icon": "mypack:textures/gui/ability/dash.png" }
  ]
}
```

Полная схема, список встроенных типов способностей/условий и правила валидации: [DATAPACK.md](DATAPACK.md).

---

## API для моддинга

```java
// Регистрация новых типов способностей
ArchetypeAPI.registerAbilityType(id, factory);
ArchetypeAPI.registerPassiveType(id, factory);
ArchetypeAPI.registerConditionType(id, factory);

// Управление классами игрока
ArchetypeAPI.getPlayerClass(player);
ArchetypeAPI.assignClass(serverPlayer, classId);
```

---

## Сборка

```bash
./gradlew build
```

Артефакты: `fabric/build/libs/`

**Зависимости:** Minecraft 1.21.11 · Fabric API · Java 21

---

## Лицензия

All Rights Reserved © ZorahM
