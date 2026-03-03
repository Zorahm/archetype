package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import com.mod.archetype.platform.NetworkHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FabricNetworkHandler implements NetworkHandler {

    private final Map<Class<?>, ResourceLocation> packetChannels = new HashMap<>();
    private final Map<Class<?>, PacketEncoder<?>> packetEncoders = new HashMap<>();

    @Override
    public void init() {
        // Packets are registered via registerServerReceiver / registerClientReceiver
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToPlayer(ServerPlayer player, T packet) {
        ResourceLocation channel = packetChannels.get(packet.getClass());
        if (channel == null) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketEncoder<T> encoder = (PacketEncoder<T>) packetEncoders.get(packet.getClass());
        if (encoder != null) {
            encoder.encode(packet, buf);
        }
        ServerPlayNetworking.send(player, channel, buf);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToServer(T packet) {
        ResourceLocation channel = packetChannels.get(packet.getClass());
        if (channel == null) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketEncoder<T> encoder = (PacketEncoder<T>) packetEncoders.get(packet.getClass());
        if (encoder != null) {
            encoder.encode(packet, buf);
        }
        ClientPlayNetworking.send(channel, buf);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void sendToTracking(Entity entity, T packet) {
        if (!(entity.level().getServer() != null)) return;

        ResourceLocation channel = packetChannels.get(packet.getClass());
        if (channel == null) return;

        FriendlyByteBuf buf = PacketByteBufs.create();
        PacketEncoder<T> encoder = (PacketEncoder<T>) packetEncoders.get(packet.getClass());
        if (encoder != null) {
            encoder.encode(packet, buf);
        }

        for (ServerPlayer tracking : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(tracking, channel, buf);
        }
    }

    @Override
    public <T> void registerServerReceiver(ResourceLocation id, Class<T> packetClass,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ServerPacketHandler<T> handler) {
        packetChannels.put(packetClass, id);
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, netHandler, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            server.execute(() -> handler.handle(player, packet));
        });
    }

    @Override
    public <T> void registerClientReceiver(ResourceLocation id, Class<T> packetClass,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ClientPacketHandler<T> handler) {
        packetChannels.put(packetClass, id);
        ClientPlayNetworking.registerGlobalReceiver(id, (client, netHandler, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            client.execute(() -> handler.handle(packet));
        });
    }

    public <T> void registerPacketEncoder(Class<T> packetClass, PacketEncoder<T> encoder) {
        packetEncoders.put(packetClass, encoder);
    }

    @FunctionalInterface
    public interface PacketEncoder<T> {
        void encode(T packet, FriendlyByteBuf buf);
    }
}
