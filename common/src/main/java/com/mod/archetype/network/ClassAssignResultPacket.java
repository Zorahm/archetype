package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

public class ClassAssignResultPacket {

    private final boolean success;
    @Nullable
    private final String failReasonKey;

    public ClassAssignResultPacket(boolean success, @Nullable String failReasonKey) {
        this.success = success;
        this.failReasonKey = failReasonKey;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeBoolean(failReasonKey != null);
        if (failReasonKey != null) {
            buf.writeUtf(failReasonKey, 256);
        }
    }

    public static ClassAssignResultPacket decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        String reason = null;
        if (buf.readBoolean()) {
            reason = buf.readUtf(256);
        }
        return new ClassAssignResultPacket(success, reason);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getFailReasonKey() {
        return failReasonKey;
    }
}
