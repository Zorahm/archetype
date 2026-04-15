package com.mod.archetype.fabric;

import com.mod.archetype.platform.NetworkHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FabricNetworkHandler implements NetworkHandler {

    // ── Internal multiplexed payload wrappers ──────────────────────────────

    record ArchetypeC2SPayload(Identifier channel, byte[] data) implements CustomPacketPayload {
        static final Identifier PAYLOAD_ID =
                Identifier.fromNamespaceAndPath("archetype", "c2s");
        static final CustomPacketPayload.Type<ArchetypeC2SPayload> TYPE =
                new CustomPacketPayload.Type<>(PAYLOAD_ID);
        static final StreamCodec<RegistryFriendlyByteBuf, ArchetypeC2SPayload> CODEC =
                StreamCodec.composite(
                        Identifier.STREAM_CODEC, ArchetypeC2SPayload::channel,
                        ByteBufCodecs.BYTE_ARRAY, ArchetypeC2SPayload::data,
                        ArchetypeC2SPayload::new
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    record ArchetypeS2CPayload(Identifier channel, byte[] data) implements CustomPacketPayload {
        static final Identifier PAYLOAD_ID =
                Identifier.fromNamespaceAndPath("archetype", "s2c");
        static final CustomPacketPayload.Type<ArchetypeS2CPayload> TYPE =
                new CustomPacketPayload.Type<>(PAYLOAD_ID);
        static final StreamCodec<RegistryFriendlyByteBuf, ArchetypeS2CPayload> CODEC =
                StreamCodec.composite(
                        Identifier.STREAM_CODEC, ArchetypeS2CPayload::channel,
                        ByteBufCodecs.BYTE_ARRAY, ArchetypeS2CPayload::data,
                        ArchetypeS2CPayload::new
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ── Storage ────────────────────────────────────────────────────────────

    private record ReceiverEntry<T>(Function<FriendlyByteBuf, T> decoder,
                                    ServerPacketHandler<T> handler) {}

    private record ClientEntry<T>(Function<FriendlyByteBuf, T> decoder,
                                  ClientPacketHandler<T> handler) {}

    private final Map<Identifier, ReceiverEntry<?>> serverReceivers = new HashMap<>();
    private final Map<Identifier, ClientEntry<?>> clientReceivers = new HashMap<>();
    private final Map<Class<?>, Identifier> packetChannels = new HashMap<>();
    private final Map<Class<?>, BiConsumer<?, FriendlyByteBuf>> encoders = new HashMap<>();

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void init() {
        PayloadTypeRegistry.playC2S().register(ArchetypeC2SPayload.TYPE, ArchetypeC2SPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ArchetypeS2CPayload.TYPE, ArchetypeS2CPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ArchetypeC2SPayload.TYPE, this::handleC2S);
    }

    /** Called from {@link ArchetypeFabricClient#onInitializeClient()}. */
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(ArchetypeS2CPayload.TYPE, this::handleS2C);
    }

    // ── Handlers ───────────────────────────────────────────────────────────

    private void handleC2S(ArchetypeC2SPayload payload, ServerPlayNetworking.Context ctx) {
        ReceiverEntry<?> entry = serverReceivers.get(payload.channel());
        if (entry == null) return;
        dispatchServer(entry, payload.data(), ctx.player());
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatchServer(ReceiverEntry<T> entry, byte[] data, ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        T packet = entry.decoder().apply(buf);
        entry.handler().handle(player, packet);
    }

    private void handleS2C(ArchetypeS2CPayload payload, ClientPlayNetworking.Context ctx) {
        ClientEntry<?> entry = clientReceivers.get(payload.channel());
        if (entry == null) return;
        dispatchClient(entry, payload.data());
    }

    @SuppressWarnings("unchecked")
    private <T> void dispatchClient(ClientEntry<T> entry, byte[] data) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        T packet = entry.decoder().apply(buf);
        entry.handler().handle(packet);
    }

    // ── Send ───────────────────────────────────────────────────────────────

    @Override
    public <T> void sendToPlayer(ServerPlayer player, T packet) {
        ArchetypeS2CPayload payload = buildS2C(packet);
        if (payload != null) ServerPlayNetworking.send(player, payload);
    }

    @Override
    public <T> void sendToServer(T packet) {
        ArchetypeC2SPayload payload = buildC2S(packet);
        if (payload != null) ClientPlayNetworking.send(payload);
    }

    @Override
    public <T> void sendToTracking(Entity entity, T packet) {
        if (entity.level().getServer() == null) return;
        ArchetypeS2CPayload payload = buildS2C(packet);
        if (payload == null) return;
        for (ServerPlayer tracking : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(tracking, payload);
        }
    }

    // ── Registration ──────────────────────────────────────────────────────

    @Override
    public <T> void registerServerReceiver(Identifier id, Class<T> packetClass,
                                            BiConsumer<T, FriendlyByteBuf> encoder,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ServerPacketHandler<T> handler) {
        packetChannels.put(packetClass, id);
        encoders.put(packetClass, encoder);
        serverReceivers.put(id, new ReceiverEntry<>(decoder, handler));
    }

    @Override
    public <T> void registerClientReceiver(Identifier id, Class<T> packetClass,
                                            BiConsumer<T, FriendlyByteBuf> encoder,
                                            Function<FriendlyByteBuf, T> decoder,
                                            ClientPacketHandler<T> handler) {
        packetChannels.put(packetClass, id);
        encoders.put(packetClass, encoder);
        clientReceivers.put(id, new ClientEntry<>(decoder, handler));
    }

    // ── Serialization helpers ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> ArchetypeS2CPayload buildS2C(T packet) {
        Identifier channel = packetChannels.get(packet.getClass());
        BiConsumer<T, FriendlyByteBuf> enc =
                (BiConsumer<T, FriendlyByteBuf>) encoders.get(packet.getClass());
        if (channel == null || enc == null) return null;
        return new ArchetypeS2CPayload(channel, serialize(packet, enc));
    }

    @SuppressWarnings("unchecked")
    private <T> ArchetypeC2SPayload buildC2S(T packet) {
        Identifier channel = packetChannels.get(packet.getClass());
        BiConsumer<T, FriendlyByteBuf> enc =
                (BiConsumer<T, FriendlyByteBuf>) encoders.get(packet.getClass());
        if (channel == null || enc == null) return null;
        return new ArchetypeC2SPayload(channel, serialize(packet, enc));
    }

    private <T> byte[] serialize(T packet, BiConsumer<T, FriendlyByteBuf> encoder) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            encoder.accept(packet, buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }
}
