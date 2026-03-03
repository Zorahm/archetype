# MM #14 — Платформенные модули (Forge + Fabric)

## Роль

Java-разработчик Minecraft модов. Реализуешь платформо-специфичный код для Forge и Fabric.

## Контекст

Посмотри проект

## Задача

Полная реализация всех файлов в `forge/` и `fabric/` модулях.

---

## Forge модуль

### ArchetypeForge.java

```java
@Mod(Archetype.MOD_ID)
public class ArchetypeForge {
    public ArchetypeForge() {
        Archetype.init();
        
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerKeyMappings);
        
        MinecraftForge.EVENT_BUS.register(ForgeEventTranslator.class);
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeNetworkHandler.INSTANCE.init();
        });
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
        Archetype.initClient();
    }
    
    private void registerAttributes(EntityAttributeModificationEvent event) {
        // Регистрация кастомных атрибутов мода на сущность PLAYER
        // archetype:cooldown_reduction, archetype:ability_range, и т.д.
    }
    
    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Регистрация 4 клавиш
    }
}
```

### ForgeNetworkHandler.java

Реализация `NetworkHandler` через Forge `SimpleChannel`:

```java
public class ForgeNetworkHandler implements NetworkHandler {
    public static final ForgeNetworkHandler INSTANCE = new ForgeNetworkHandler();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Archetype.MOD_ID, "main"),
        () -> "1", s -> true, s -> true
    );
    private int packetId = 0;
    
    @Override
    public void init() {
        NetworkInit.register(this);
    }
    
    @Override
    public <T> void registerServerReceiver(ResourceLocation id, Class<T> cls, 
            PacketDecoder<T> decoder, ServerPacketHandler<T> handler) {
        CHANNEL.registerMessage(packetId++, cls,
            (msg, buf) -> ((Encodable)msg).encode(buf),  // cast or wrapper
            decoder::decode,
            (msg, ctxSupplier) -> {
                NetworkEvent.Context ctx = ctxSupplier.get();
                ctx.enqueueWork(() -> handler.handle(ctx.getSender(), msg));
                ctx.setPacketHandled(true);
            }
        );
    }
    
    @Override
    public <T> void registerClientReceiver(ResourceLocation id, Class<T> cls,
            PacketDecoder<T> decoder, ClientPacketHandler<T> handler) {
        CHANNEL.registerMessage(packetId++, cls,
            (msg, buf) -> ((Encodable)msg).encode(buf),
            decoder::decode,
            (msg, ctxSupplier) -> {
                NetworkEvent.Context ctx = ctxSupplier.get();
                ctx.enqueueWork(() -> handler.handle(msg));
                ctx.setPacketHandled(true);
            }
        );
    }
    
    @Override
    public <T> void sendToPlayer(ServerPlayer player, T packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    @Override
    public <T> void sendToServer(T packet) {
        CHANNEL.sendToServer(packet);
    }
    
    @Override
    public <T> void sendToTracking(Entity entity, T packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
}
```

### ForgePlayerDataAccess.java

Через Capability:

```java
public class ForgePlayerDataAccess implements PlayerDataAccess {
    public static final ResourceLocation CAP_ID = new ResourceLocation(Archetype.MOD_ID, "class_data");
    
    // Capability
    public static final Capability<PlayerClassData> CLASS_DATA_CAP = CapabilityManager.get(new CapabilityToken<>() {});
    
    @Override
    public PlayerClassData getClassData(Player player) {
        return player.getCapability(CLASS_DATA_CAP).orElseThrow(
            () -> new IllegalStateException("Player missing class data capability")
        );
    }
    
    @Override
    public void setClassData(Player player, PlayerClassData data) {
        player.getCapability(CLASS_DATA_CAP).ifPresent(existing -> existing.copyFrom(data));
    }
    
    // CapabilityProvider — внутренний класс
    // Регистрация через AttachCapabilitiesEvent<Entity>
    // Сериализация через INBTSerializable<CompoundTag>
}
```

Отдельно: `ForgeCapabilityProvider.java` с полной реализацией `ICapabilitySerializable<CompoundTag>`.

### ForgeEventTranslator.java

Маппинг Forge-событий → вызовы ClassManager:

```java
public class ForgeEventTranslator {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        ClassManager.getInstance().tickPlayer((ServerPlayer) event.player);
    }
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ClassManager.getInstance().onPlayerJoin((ServerPlayer) event.getEntity());
    }
    
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassManager.getInstance().onPlayerDeath(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ClassManager.getInstance().onPlayerRespawn((ServerPlayer) event.getEntity());
    }
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Делегировать пассивкам: проверить shouldCancelDamage
            // event.setCanceled(true) если нужно
            ClassManager.getInstance().onPlayerHurt(player, event.getSource(), event.getAmount());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassManager.getInstance().onPlayerAttack(player, event.getTarget(), /* source */);
        }
    }
    
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AbilityHudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }
    
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ArchetypeKeybinds.tickKeybinds(/* передать KeyMapping инстансы */);
    }
    
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ForgePlayerDataAccess.CAP_ID, new ForgeCapabilityProvider());
        }
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Копировать capability данные при смерти/возвращении из End
        event.getOriginal().getCapability(ForgePlayerDataAccess.CLASS_DATA_CAP).ifPresent(oldData -> {
            event.getEntity().getCapability(ForgePlayerDataAccess.CLASS_DATA_CAP).ifPresent(newData -> {
                newData.copyFrom(oldData);
            });
        });
    }
    
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ClassRegistry.getInstance());
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ArchetypeCommand.register(event.getDispatcher());
    }
}
```

### ForgePlatformHelper.java

```java
public class ForgePlatformHelper implements PlatformHelper {
    @Override public boolean isForge() { return true; }
    @Override public boolean isFabric() { return false; }
    @Override public boolean isClient() { return FMLEnvironment.dist == Dist.CLIENT; }
    @Override public boolean isDedicatedServer() { return FMLEnvironment.dist == Dist.DEDICATED_SERVER; }
    @Override public Path getConfigDir() { return FMLPaths.CONFIGDIR.get(); }
}
```

---

## Fabric модуль

### ArchetypeFabric.java

```java
public class ArchetypeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Archetype.init();
        FabricNetworkHandler.INSTANCE.init();
        
        // Регистрация событий
        ServerTickEvents.END_SERVER_TICK.register(server -> { /* tick all players */ });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { /* onPlayerJoin */ });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { /* if Player → onDeath */ });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> { /* onRespawn */ });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> { /* onAttack */ });
        
        // Команды
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ArchetypeCommand.register(dispatcher);
        });
        
        // Data reload
        ResourceManagerHelper.get(PackType.SERVER_DATA)
            .registerReloadListener(ClassRegistry.getInstance());
    }
}
```

### ArchetypeFabricClient.java

```java
public class ArchetypeFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Archetype.initClient();
        
        // KeyBindings
        KeyBinding ability1 = KeyBindingHelper.registerKeyBinding(new KeyBinding(...));
        // ...
        
        // HUD
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            AbilityHudOverlay.render(drawContext, tickDelta);
        });
        
        // Client tick для keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ArchetypeKeybinds.tickKeybinds(ability1, ability2, ability3, classInfo);
        });
    }
}
```

### FabricNetworkHandler.java

Через Fabric Networking API:

```java
public class FabricNetworkHandler implements NetworkHandler {
    public static final FabricNetworkHandler INSTANCE = new FabricNetworkHandler();
    
    @Override
    public <T> void registerServerReceiver(ResourceLocation id, Class<T> cls,
            PacketDecoder<T> decoder, ServerPacketHandler<T> handler) {
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler2, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            server.execute(() -> handler.handle(player, packet));
        });
    }
    
    @Override
    public <T> void registerClientReceiver(ResourceLocation id, Class<T> cls,
            PacketDecoder<T> decoder, ClientPacketHandler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler2, buf, responseSender) -> {
            T packet = decoder.decode(buf);
            client.execute(() -> handler.handle(packet));
        });
    }
    
    @Override
    public <T> void sendToPlayer(ServerPlayer player, T packet) {
        // Encode to PacketByteBuf, send via ServerPlayNetworking.send
    }
    
    @Override
    public <T> void sendToServer(T packet) {
        // Encode, ClientPlayNetworking.send
    }
    
    @Override
    public <T> void sendToTracking(Entity entity, T packet) {
        // PlayerLookup.tracking(entity).forEach(p -> sendToPlayer(p, packet))
    }
}
```

### FabricPlayerDataAccess.java

Через Cardinal Components API или Fabric Data Attachment (1.20.5+):

Для 1.20.1 (Cardinal Components):
```java
public class FabricPlayerDataAccess implements PlayerDataAccess, ComponentV3 {
    // Регистрация ComponentKey
    // Сериализация через readFromNbt / writeToNbt
}
```

### FabricPlatformHelper.java

Аналогично Forge, но `FabricLoader.getInstance()`.

---

## Файлы ресурсов

### fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "archetype",
  "version": "${version}",
  "name": "Archetype",
  "description": "Class system for Minecraft",
  "entrypoints": {
    "main": ["com.mod.archetype.fabric.ArchetypeFabric"],
    "client": ["com.mod.archetype.fabric.ArchetypeFabricClient"]
  },
  "depends": {
    "fabricloader": ">=0.14.0",
    "fabric-api": "*",
    "architectury": "*",
    "minecraft": ">=1.20.1"
  }
}
```

### META-INF/mods.toml (Forge)
Стандартный файл с зависимостью на Architectury.

## Формат вывода

Forge (6 файлов):
1. `ArchetypeForge.java`
2. `ForgeNetworkHandler.java`
3. `ForgePlayerDataAccess.java` + `ForgeCapabilityProvider.java`
4. `ForgeEventTranslator.java`
5. `ForgePlatformHelper.java`

Fabric (6 файлов):
6. `ArchetypeFabric.java`
7. `ArchetypeFabricClient.java`
8. `FabricNetworkHandler.java`
9. `FabricPlayerDataAccess.java`
10. `FabricPlatformHelper.java`

Ресурсы:
11. `fabric.mod.json`
12. `mods.toml`
13. ServiceLoader файлы (META-INF/services/*)
