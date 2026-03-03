package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;

public class AbilityUsePacket {

    private final String slotName;

    public AbilityUsePacket(String slotName) {
        this.slotName = slotName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(slotName, 32);
    }

    public static AbilityUsePacket decode(FriendlyByteBuf buf) {
        return new AbilityUsePacket(buf.readUtf(32));
    }

    public String getSlotName() {
        return slotName;
    }
}
