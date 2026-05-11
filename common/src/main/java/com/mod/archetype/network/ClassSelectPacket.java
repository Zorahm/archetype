package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public class ClassSelectPacket {

    private final Identifier classId;
    private final boolean viaItem;

    public ClassSelectPacket(Identifier classId, boolean viaItem) {
        this.classId = classId;
        this.viaItem = viaItem;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeIdentifier(classId);
        buf.writeBoolean(viaItem);
    }

    public static ClassSelectPacket decode(FriendlyByteBuf buf) {
        return new ClassSelectPacket(buf.readIdentifier(), buf.readBoolean());
    }

    public Identifier getClassId() {
        return classId;
    }

    public boolean isViaItem() {
        return viaItem;
    }
}
