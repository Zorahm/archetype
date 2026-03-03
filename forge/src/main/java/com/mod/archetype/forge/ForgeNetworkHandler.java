package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ForgeNetworkHandler implements NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Archetype.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private int packetId = 0;
    private final Map<Class<?>, Integer> packetIds = new HashMap<>();

    @Override
    public void init() {
        // Packets are registered via registerServerReceiver / registerClientReceiver
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToPlayer(ServerPlayer player, T packet) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToServer(T packet) {
        CHANNEL.sendToServer(packet);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToTracking(Entity entity, T packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    @Override
    public <T> void registerServerReceiver(ResourceLocation id, Class<T> packetClass,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ServerPacketHandler<T> handler) {
        int thisId = packetId++;
        packetIds.put(packetClass, thisId);

        CHANNEL.registerMessage(thisId, packetClass,
                (packet, buf) -> {}, // encode delegated to packet itself
                decoder::apply,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> {
                        ServerPlayer sender = ctx.get().getSender();
                        if (sender != null) {
                            handler.handle(sender, packet);
                        }
                    });
                    ctx.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
    }

    @Override
    public <T> void registerClientReceiver(ResourceLocation id, Class<T> packetClass,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ClientPacketHandler<T> handler) {
        int thisId = packetId++;
        packetIds.put(packetClass, thisId);

        CHANNEL.registerMessage(thisId, packetClass,
                (packet, buf) -> {}, // encode delegated to packet itself
                decoder::apply,
                (packet, ctx) -> {
                    ctx.get().enqueueWork(() -> handler.handle(packet));
                    ctx.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }
}
