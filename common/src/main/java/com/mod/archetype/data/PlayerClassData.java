package com.mod.archetype.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerClassData {

    @Nullable
    private ResourceLocation currentClassId;
    private long classAssignedTime;
    private int classLevel = 1;
    private int classExperience;
    private float resourceCurrent;
    private Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private Set<Integer> activeConditionalSets = new HashSet<>();
    private Map<ResourceLocation, Boolean> toggleStates = new HashMap<>();

    @Nullable
    public ResourceLocation getCurrentClassId() {
        return currentClassId;
    }

    public void setCurrentClassId(@Nullable ResourceLocation classId) {
        this.currentClassId = classId;
    }

    public boolean hasClass() {
        return currentClassId != null;
    }

    public long getClassAssignedTime() {
        return classAssignedTime;
    }

    public void setClassAssignedTime(long time) {
        this.classAssignedTime = time;
    }

    public int getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(int level) {
        this.classLevel = level;
    }

    public int getClassExperience() {
        return classExperience;
    }

    public void setClassExperience(int experience) {
        this.classExperience = experience;
    }

    public float getResourceCurrent() {
        return resourceCurrent;
    }

    public void setResourceCurrent(float value) {
        this.resourceCurrent = value;
    }

    public Map<ResourceLocation, Integer> getCooldowns() {
        return cooldowns;
    }

    public int getCooldown(ResourceLocation abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void setCooldown(ResourceLocation abilityId, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(abilityId);
        } else {
            cooldowns.put(abilityId, ticks);
        }
    }

    public Set<Integer> getActiveConditionalSets() {
        return activeConditionalSets;
    }

    public Map<ResourceLocation, Boolean> getToggleStates() {
        return toggleStates;
    }

    public boolean getToggleState(ResourceLocation abilityId) {
        return toggleStates.getOrDefault(abilityId, false);
    }

    public void setToggleState(ResourceLocation abilityId, boolean state) {
        toggleStates.put(abilityId, state);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        if (currentClassId != null) {
            tag.putString("ClassId", currentClassId.toString());
        }
        tag.putLong("AssignedTime", classAssignedTime);
        tag.putInt("Level", classLevel);
        tag.putInt("Experience", classExperience);
        tag.putFloat("Resource", resourceCurrent);

        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach((id, ticks) -> cooldownTag.putInt(id.toString(), ticks));
        tag.put("Cooldowns", cooldownTag);

        int[] conditionalArray = activeConditionalSets.stream().mapToInt(Integer::intValue).toArray();
        tag.putIntArray("ConditionalSets", conditionalArray);

        CompoundTag toggleTag = new CompoundTag();
        toggleStates.forEach((id, state) -> toggleTag.putBoolean(id.toString(), state));
        tag.put("Toggles", toggleTag);

        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ClassId")) {
            currentClassId = new ResourceLocation(tag.getString("ClassId"));
        } else {
            currentClassId = null;
        }
        classAssignedTime = tag.getLong("AssignedTime");
        classLevel = tag.getInt("Level");
        if (classLevel < 1) classLevel = 1;
        classExperience = tag.getInt("Experience");
        resourceCurrent = tag.getFloat("Resource");

        cooldowns.clear();
        CompoundTag cooldownTag = tag.getCompound("Cooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            cooldowns.put(new ResourceLocation(key), cooldownTag.getInt(key));
        }

        activeConditionalSets.clear();
        int[] conditionalArray = tag.getIntArray("ConditionalSets");
        for (int i : conditionalArray) {
            activeConditionalSets.add(i);
        }

        toggleStates.clear();
        CompoundTag toggleTag = tag.getCompound("Toggles");
        for (String key : toggleTag.getAllKeys()) {
            toggleStates.put(new ResourceLocation(key), toggleTag.getBoolean(key));
        }
    }

    public void writeSyncData(FriendlyByteBuf buf) {
        buf.writeBoolean(hasClass());
        if (hasClass()) {
            buf.writeResourceLocation(currentClassId);
            buf.writeVarInt(classLevel);
            buf.writeVarInt(classExperience);
            buf.writeFloat(resourceCurrent);

            buf.writeVarInt(cooldowns.size());
            cooldowns.forEach((id, ticks) -> {
                buf.writeResourceLocation(id);
                buf.writeVarInt(ticks);
            });

            buf.writeVarInt(toggleStates.size());
            toggleStates.forEach((id, state) -> {
                buf.writeResourceLocation(id);
                buf.writeBoolean(state);
            });
        }
    }

    public void readSyncData(FriendlyByteBuf buf) {
        boolean has = buf.readBoolean();
        if (has) {
            currentClassId = buf.readResourceLocation();
            classLevel = buf.readVarInt();
            classExperience = buf.readVarInt();
            resourceCurrent = buf.readFloat();

            cooldowns.clear();
            int cooldownCount = buf.readVarInt();
            for (int i = 0; i < cooldownCount; i++) {
                cooldowns.put(buf.readResourceLocation(), buf.readVarInt());
            }

            toggleStates.clear();
            int toggleCount = buf.readVarInt();
            for (int i = 0; i < toggleCount; i++) {
                toggleStates.put(buf.readResourceLocation(), buf.readBoolean());
            }
        } else {
            currentClassId = null;
            cooldowns.clear();
            toggleStates.clear();
        }
    }

    public void reset() {
        currentClassId = null;
        classAssignedTime = 0;
        classLevel = 1;
        classExperience = 0;
        resourceCurrent = 0;
        cooldowns.clear();
        activeConditionalSets.clear();
        toggleStates.clear();
    }
}
