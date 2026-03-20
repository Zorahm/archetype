# fabric module

Fabric-специфичный слой. Минимум логики — только трансляция к common.

## Файлы

| Файл | Роль |
|------|------|
| `ArchetypeFabric.java` | `ModInitializer` → вызывает `Archetype.init()`, регистрирует команды, события, ClassRegistry |
| `ArchetypeFabricClient.java` | `ClientModInitializer` → клиентская инициализация, рендереры, keybinds |
| `FabricNetworkHandler.java` | Реализация `NetworkHandler` через Fabric Networking API |
| `FabricPlayerDataAccess.java` | Реализация `PlayerDataAccess` через Data Attachments / Cardinal Components |
| `FabricEventTranslator.java` | Трансляция Fabric Callbacks → common-обработчики |
| `FabricPlatformHelper.java` | `PlatformHelper` — определение окружения |
| `ArchetypeWorldData.java` | World-scoped данные |

## Сборка

Shadow JAR включает common. `remapJar` переводит из dev в production маппинги.

## Метаданные

`fabric.mod.json` — `${version}` подставляется при processResources.
