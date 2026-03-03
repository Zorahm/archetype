package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class ClassSelectPacket {

    private final ResourceLocation classId;
    private final boolean viaItem;

    public ClassSelectPacket(ResourceLocation classId, boolean viaItem) {
        this.classId = classId;
        this.viaItem = viaItem;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(classId);
        buf.writeBoolean(viaItem);
    }

    public static ClassSelectPacket decode(FriendlyByteBuf buf) {
        return new ClassSelectPacket(buf.readResourceLocation(), buf.readBoolean());
    }

    public ResourceLocation getClassId() {
        return classId;
    }

    public boolean isViaItem() {
        return viaItem;
    }
}
