package com.mod.archetype.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

public class SyncClassDataPacket {

    private final boolean hasClass;
    @Nullable
    private final Identifier classId;
    private final int level;
    private final int experience;
    private final float resourceCurrent;
    private final float resourceMax;
    private final Map<Identifier, CooldownEntry> cooldowns;
    private final Map<Identifier, Boolean> toggleStates;
    private final Map<Identifier, ChargeEntry> charges;

    public SyncClassDataPacket(boolean hasClass, @Nullable Identifier classId,
                                int level, int experience,
                                float resourceCurrent, float resourceMax,
                                Map<Identifier, CooldownEntry> cooldowns,
                                Map<Identifier, Boolean> toggleStates) {
        this(hasClass, classId, level, experience, resourceCurrent, resourceMax,
                cooldowns, toggleStates, Map.of());
    }

    public SyncClassDataPacket(boolean hasClass, @Nullable Identifier classId,
                                int level, int experience,
                                float resourceCurrent, float resourceMax,
                                Map<Identifier, CooldownEntry> cooldowns,
                                Map<Identifier, Boolean> toggleStates,
                                Map<Identifier, ChargeEntry> charges) {
        this.hasClass = hasClass;
        this.classId = classId;
        this.level = level;
        this.experience = experience;
        this.resourceCurrent = resourceCurrent;
        this.resourceMax = resourceMax;
        this.cooldowns = cooldowns;
        this.toggleStates = toggleStates;
        this.charges = charges;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(hasClass);
        if (hasClass) {
            buf.writeIdentifier(classId);
            buf.writeVarInt(level);
            buf.writeVarInt(experience);
            buf.writeFloat(resourceCurrent);
            buf.writeFloat(resourceMax);

            buf.writeVarInt(cooldowns.size());
            cooldowns.forEach((id, entry) -> {
                buf.writeIdentifier(id);
                buf.writeVarInt(entry.remaining());
                buf.writeVarInt(entry.max());
            });

            buf.writeVarInt(toggleStates.size());
            toggleStates.forEach((id, state) -> {
                buf.writeIdentifier(id);
                buf.writeBoolean(state);
            });

            buf.writeVarInt(charges.size());
            charges.forEach((id, entry) -> {
                buf.writeIdentifier(id);
                buf.writeVarInt(entry.current());
                buf.writeVarInt(entry.max());
            });
        }
    }

    public static SyncClassDataPacket decode(FriendlyByteBuf buf) {
        boolean hasClass = buf.readBoolean();
        if (!hasClass) {
            return new SyncClassDataPacket(false, null, 0, 0, 0, 0,
                    Map.of(), Map.of());
        }

        Identifier classId = buf.readIdentifier();
        int level = buf.readVarInt();
        int experience = buf.readVarInt();
        float resourceCurrent = buf.readFloat();
        float resourceMax = buf.readFloat();

        int cooldownCount = buf.readVarInt();
        Map<Identifier, CooldownEntry> cooldowns = new HashMap<>();
        for (int i = 0; i < cooldownCount; i++) {
            Identifier id = buf.readIdentifier();
            int remaining = buf.readVarInt();
            int max = buf.readVarInt();
            cooldowns.put(id, new CooldownEntry(remaining, max));
        }

        int toggleCount = buf.readVarInt();
        Map<Identifier, Boolean> toggleStates = new HashMap<>();
        for (int i = 0; i < toggleCount; i++) {
            toggleStates.put(buf.readIdentifier(), buf.readBoolean());
        }

        Map<Identifier, ChargeEntry> charges = new HashMap<>();
        if (buf.isReadable()) {
            int chargeCount = buf.readVarInt();
            for (int i = 0; i < chargeCount; i++) {
                Identifier id = buf.readIdentifier();
                int current = buf.readVarInt();
                int max = buf.readVarInt();
                charges.put(id, new ChargeEntry(current, max));
            }
        }

        return new SyncClassDataPacket(true, classId, level, experience,
                resourceCurrent, resourceMax, cooldowns, toggleStates, charges);
    }

    public boolean hasClass() { return hasClass; }
    @Nullable public Identifier getClassId() { return classId; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public float getResourceCurrent() { return resourceCurrent; }
    public float getResourceMax() { return resourceMax; }
    public Map<Identifier, CooldownEntry> getCooldowns() { return cooldowns; }
    public Map<Identifier, Boolean> getToggleStates() { return toggleStates; }
    public Map<Identifier, ChargeEntry> getCharges() { return charges; }

    public record CooldownEntry(int remaining, int max) {}
    public record ChargeEntry(int current, int max) {}
}
