package com.mod.archetype.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface NetworkHandler {

    NetworkHandler INSTANCE = ServiceLoader.load(NetworkHandler.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No NetworkHandler implementation found"));

    void init();

    <T> void sendToPlayer(ServerPlayer player, T packet);

    <T> void sendToServer(T packet);

    <T> void sendToTracking(Entity entity, T packet);

    <T> void registerServerReceiver(Identifier id, Class<T> packetClass,
                                     BiConsumer<T, FriendlyByteBuf> encoder,
                                     Function<FriendlyByteBuf, T> decoder,
                                     ServerPacketHandler<T> handler);

    <T> void registerClientReceiver(Identifier id, Class<T> packetClass,
                                     BiConsumer<T, FriendlyByteBuf> encoder,
                                     Function<FriendlyByteBuf, T> decoder,
                                     ClientPacketHandler<T> handler);

    @FunctionalInterface
    interface ServerPacketHandler<T> {
        void handle(ServerPlayer player, T packet);
    }

    @FunctionalInterface
    interface ClientPacketHandler<T> {
        void handle(T packet);
    }
}
