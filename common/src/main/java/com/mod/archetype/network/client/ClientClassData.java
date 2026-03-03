package com.mod.archetype.network.client;

import com.mod.archetype.network.SyncClassDataPacket;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ClientClassData {

    private static final ClientClassData INSTANCE = new ClientClassData();

    private boolean hasClass;
    @Nullable
    private ResourceLocation classId;
    private int level;
    private int experience;
    private float resourceCurrent;
    private float resourceMax;
    private Map<ResourceLocation, CooldownInfo> cooldowns = new HashMap<>();
    private Map<ResourceLocation, Boolean> toggleStates = new HashMap<>();

    public static ClientClassData getInstance() {
        return INSTANCE;
    }

    public void update(SyncClassDataPacket packet) {
        this.hasClass = packet.hasClass();
        this.classId = packet.getClassId();
        this.level = packet.getLevel();
        this.experience = packet.getExperience();
        this.resourceCurrent = packet.getResourceCurrent();
        this.resourceMax = packet.getResourceMax();

        this.cooldowns.clear();
        packet.getCooldowns().forEach((id, entry) ->
                cooldowns.put(id, new CooldownInfo(entry.remaining(), entry.max())));

        this.toggleStates.clear();
        this.toggleStates.putAll(packet.getToggleStates());
    }

    public void clear() {
        hasClass = false;
        classId = null;
        level = 0;
        experience = 0;
        resourceCurrent = 0;
        resourceMax = 0;
        cooldowns.clear();
        toggleStates.clear();
    }

    public boolean hasClass() { return hasClass; }
    @Nullable public ResourceLocation getClassId() { return classId; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public float getResourceCurrent() { return resourceCurrent; }
    public float getResourceMax() { return resourceMax; }
    public Map<ResourceLocation, CooldownInfo> getCooldowns() { return cooldowns; }
    public Map<ResourceLocation, Boolean> getToggleStates() { return toggleStates; }

    @Nullable
    public CooldownInfo getCooldown(ResourceLocation abilityId) {
        return cooldowns.get(abilityId);
    }

    public boolean isToggleActive(ResourceLocation abilityId) {
        return toggleStates.getOrDefault(abilityId, false);
    }

    public record CooldownInfo(int remaining, int maxTicks) {
        public float getProgress() {
            return maxTicks > 0 ? (float) remaining / maxTicks : 0f;
        }

        public boolean isOnCooldown() {
            return remaining > 0;
        }
    }
}
