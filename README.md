# Archetype

Система классов для Minecraft. Forge + Fabric | MC 1.20.1 | Architectury

## Что это

Мод добавляет классы игрокам: Вампир, Голем, Фантом, Берсерк, Маг и другие. Каждый класс даёт уникальные активные и пассивные способности, меняет характеристики, накладывает ограничения. Классы определяются через JSON — без написания Java-кода. Серверы добавляют свои классы через датапаки.

Главный принцип: **баланс через компромисс**. Вампир силён ночью, но горит днём. Голем неубиваем, но тонет. Фантом проходит сквозь стены, но хрупок.

## Архитектура

Мультилоадерный проект на [Architectury](https://github.com/architectury/architectury-api):

```
archetype/
├── common/    95% кода — ядро, способности, сеть, GUI
├── forge/     Forge-специфичное: Capabilities, SimpleChannel, события
└── fabric/    Fabric-специфичное: Data Attachments, Networking API, события
```

### Модули (common)

| Модуль | Назначение |
|--------|-----------|
| `core/` | ClassManager, PlayerClass, жизненный цикл классов |
| `ability/` | Интерфейсы и фабрики пассивных/активных способностей |
| `condition/` | Система условий с комбинаторами (and/or/not) |
| `data/` | PlayerClassData — хранение и сериализация данных игрока |
| `network/` | 7 пакетов, серверная валидация, клиентская синхронизация |
| `platform/` | Абстракции платформы (ServiceLoader) |
| `registry/` | Загрузка классов из JSON-датапаков |
| `command/` | Команды `/archetype` |
| `gui/` | Экран выбора класса, досье, HUD |
| `config/` | Серверный и клиентский конфиг |
| `item/` | Свиток Перерождения |
| `advancement/` | Достижения |

## Сборка

```bash
./gradlew build
```

Артефакты: `forge/build/libs/`, `fabric/build/libs/`

## Зависимости

| | Forge | Fabric |
|---|---|---|
| Architectury API | обязательно | обязательно |
| Fabric API | — | обязательно |
| Minecraft | 1.20.1 | 1.20.1 |
| Java | 17 | 17 |

## Расширяемость

**Контент-мейкерам** — новые классы через JSON-датапаки без кода.

**Мододелам** — API для регистрации типов способностей и условий:

```java
ArchetypeAPI.registerAbilityType(id, factory);
ArchetypeAPI.registerConditionType(id, factory);
ArchetypeAPI.getPlayerClass(player);
```

## Лицензия

All Rights Reserved
