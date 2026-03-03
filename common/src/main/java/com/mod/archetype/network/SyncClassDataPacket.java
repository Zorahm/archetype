package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SyncClassDataPacket {

    private final boolean hasClass;
    @Nullable
    private final ResourceLocation classId;
    private final int level;
    private final int experience;
    private final float resourceCurrent;
    private final float resourceMax;
    private final Map<ResourceLocation, CooldownEntry> cooldowns;
    private final Map<ResourceLocation, Boolean> toggleStates;

    public SyncClassDataPacket(boolean hasClass, @Nullable ResourceLocation classId,
                                int level, int experience,
                                float resourceCurrent, float resourceMax,
                                Map<ResourceLocation, CooldownEntry> cooldowns,
                                Map<ResourceLocation, Boolean> toggleStates) {
        this.hasClass = hasClass;
        this.classId = classId;
        this.level = level;
        this.experience = experience;
        this.resourceCurrent = resourceCurrent;
        this.resourceMax = resourceMax;
        this.cooldowns = cooldowns;
        this.toggleStates = toggleStates;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasClass);
        if (hasClass) {
            buf.writeResourceLocation(classId);
            buf.writeVarInt(level);
            buf.writeVarInt(experience);
            buf.writeFloat(resourceCurrent);
            buf.writeFloat(resourceMax);

            buf.writeVarInt(cooldowns.size());
            cooldowns.forEach((id, entry) -> {
                buf.writeResourceLocation(id);
                buf.writeVarInt(entry.remaining());
                buf.writeVarInt(entry.max());
            });

            buf.writeVarInt(toggleStates.size());
            toggleStates.forEach((id, state) -> {
                buf.writeResourceLocation(id);
                buf.writeBoolean(state);
            });
        }
    }

    public static SyncClassDataPacket decode(FriendlyByteBuf buf) {
        boolean hasClass = buf.readBoolean();
        if (!hasClass) {
            return new SyncClassDataPacket(false, null, 0, 0, 0, 0,
                    Map.of(), Map.of());
        }

        ResourceLocation classId = buf.readResourceLocation();
        int level = buf.readVarInt();
        int experience = buf.readVarInt();
        float resourceCurrent = buf.readFloat();
        float resourceMax = buf.readFloat();

        int cooldownCount = buf.readVarInt();
        Map<ResourceLocation, CooldownEntry> cooldowns = new HashMap<>();
        for (int i = 0; i < cooldownCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int remaining = buf.readVarInt();
            int max = buf.readVarInt();
            cooldowns.put(id, new CooldownEntry(remaining, max));
        }

        int toggleCount = buf.readVarInt();
        Map<ResourceLocation, Boolean> toggleStates = new HashMap<>();
        for (int i = 0; i < toggleCount; i++) {
            toggleStates.put(buf.readResourceLocation(), buf.readBoolean());
        }

        return new SyncClassDataPacket(true, classId, level, experience,
                resourceCurrent, resourceMax, cooldowns, toggleStates);
    }

    public boolean hasClass() { return hasClass; }
    @Nullable public ResourceLocation getClassId() { return classId; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public float getResourceCurrent() { return resourceCurrent; }
    public float getResourceMax() { return resourceMax; }
    public Map<ResourceLocation, CooldownEntry> getCooldowns() { return cooldowns; }
    public Map<ResourceLocation, Boolean> getToggleStates() { return toggleStates; }

    public record CooldownEntry(int remaining, int max) {}
}
