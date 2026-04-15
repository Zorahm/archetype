package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public class SyncClassDefinitionsPacket {

    private final Map<Identifier, String> classJsonMap;

    public SyncClassDefinitionsPacket(Map<Identifier, String> classJsonMap) {
        this.classJsonMap = classJsonMap;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(classJsonMap.size());
        for (var entry : classJsonMap.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            buf.writeUtf(entry.getValue(), 65536);
        }
    }

    public static SyncClassDefinitionsPacket decode(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<Identifier, String> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            String json = buf.readUtf(65536);
            map.put(id, json);
        }
        return new SyncClassDefinitionsPacket(map);
    }

    public Map<Identifier, String> getClassJsonMap() {
        return classJsonMap;
    }
}
