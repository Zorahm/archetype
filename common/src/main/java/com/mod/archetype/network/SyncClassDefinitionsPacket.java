package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class SyncClassDefinitionsPacket {

    private final Map<ResourceLocation, String> classJsonMap;

    public SyncClassDefinitionsPacket(Map<ResourceLocation, String> classJsonMap) {
        this.classJsonMap = classJsonMap;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(classJsonMap.size());
        for (var entry : classJsonMap.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeUtf(entry.getValue(), 65536);
        }
    }

    public static SyncClassDefinitionsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<ResourceLocation, String> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String json = buf.readUtf(65536);
            map.put(id, json);
        }
        return new SyncClassDefinitionsPacket(map);
    }

    public Map<ResourceLocation, String> getClassJsonMap() {
        return classJsonMap;
    }
}
