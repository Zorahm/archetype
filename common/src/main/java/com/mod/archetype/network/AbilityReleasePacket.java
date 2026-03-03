package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;

public class AbilityReleasePacket {

    private final String slotName;

    public AbilityReleasePacket(String slotName) {
        this.slotName = slotName;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(slotName, 32);
    }

    public static AbilityReleasePacket decode(FriendlyByteBuf buf) {
        return new AbilityReleasePacket(buf.readUtf(32));
    }

    public String getSlotName() {
        return slotName;
    }
}
