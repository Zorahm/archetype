# forge module

Forge-специфичный слой. Минимум логики — только трансляция к common.

## Файлы

| Файл | Роль |
|------|------|
| `ArchetypeForge.java` | `@Mod` entry point → `Archetype.init()`, event bus, команды, ClassRegistry |
| `ForgeNetworkHandler.java` | Реализация `NetworkHandler` через SimpleChannel |
| `ForgePlayerDataAccess.java` | Реализация `PlayerDataAccess` через Forge Capabilities |
| `ForgeCapabilityProvider.java` | Прикрепление Capability к игроку |
| `ForgeEventTranslator.java` | Трансляция Forge Events → common-обработчики |
| `ForgePlatformHelper.java` | `PlatformHelper` — определение окружения |

## Сборка

Shadow JAR включает common. `remapJar` переводит из dev в production маппинги. Mixin config: `archetype.mixins.json`.

## Метаданные

`META-INF/mods.toml` — `${version}` подставляется при processResources.
