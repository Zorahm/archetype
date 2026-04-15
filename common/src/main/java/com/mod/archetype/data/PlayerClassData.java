package com.mod.archetype.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerClassData {

    @Nullable
    private Identifier currentClassId;
    private long classAssignedTime;
    private int classLevel = 0;
    private int classExperience;
    private float resourceCurrent;
    private Map<Identifier, Integer> cooldowns = new HashMap<>();
    private Set<Integer> activeConditionalSets = new HashSet<>();
    private Map<Identifier, Boolean> toggleStates = new HashMap<>();
    private long lastClassChangeTime;
    private Set<Identifier> triedClasses = new HashSet<>();

    @Nullable
    public Identifier getCurrentClassId() {
        return currentClassId;
    }

    public void setCurrentClassId(@Nullable Identifier classId) {
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

    public Map<Identifier, Integer> getCooldowns() {
        return cooldowns;
    }

    public int getCooldown(Identifier abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void setCooldown(Identifier abilityId, int ticks) {
        if (ticks <= 0) {
            cooldowns.remove(abilityId);
        } else {
            cooldowns.put(abilityId, ticks);
        }
    }

    public boolean isOnCooldown(Identifier abilityId) {
        return cooldowns.containsKey(abilityId) && cooldowns.get(abilityId) > 0;
    }

    public int getRemainingCooldown(Identifier abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void tickCooldowns() {
        cooldowns.entrySet().removeIf(entry -> {
            int newTicks = entry.getValue() - 1;
            if (newTicks <= 0) {
                return true;
            }
            entry.setValue(newTicks);
            return false;
        });
    }

    public void addExperience(int amount, int maxLevel, int[] expTable) {
        if (classLevel >= maxLevel) {
            classExperience = 0;
            return;
        }
        
        classExperience += amount;
        
        while (classLevel < maxLevel && classExperience >= expTable[classLevel]) {
            classExperience -= expTable[classLevel];
            classLevel++;
        }
        
        if (classLevel >= maxLevel) {
            classExperience = 0;
        }
    }

    public boolean canChangeClass(long currentTime, long cooldownTicks) {
        return currentTime - lastClassChangeTime >= cooldownTicks;
    }

    public Set<Integer> getActiveConditionalSets() {
        return activeConditionalSets;
    }

    public Map<Identifier, Boolean> getToggleStates() {
        return toggleStates;
    }

    public boolean getToggleState(Identifier abilityId) {
        return toggleStates.getOrDefault(abilityId, false);
    }

    public void setToggleState(Identifier abilityId, boolean state) {
        toggleStates.put(abilityId, state);
    }

    public long getLastClassChangeTime() {
        return lastClassChangeTime;
    }

    public void setLastClassChangeTime(long time) {
        this.lastClassChangeTime = time;
    }

    public Set<Identifier> getTriedClasses() {
        return triedClasses;
    }

    public boolean hasTriedClass(Identifier classId) {
        return triedClasses.contains(classId);
    }

    public void addTriedClass(Identifier classId) {
        triedClasses.add(classId);
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
        tag.putLong("LastChangeTime", lastClassChangeTime);

        CompoundTag cooldownTag = new CompoundTag();
        cooldowns.forEach((id, ticks) -> cooldownTag.putInt(id.toString(), ticks));
        tag.put("Cooldowns", cooldownTag);

        CompoundTag toggleTag = new CompoundTag();
        toggleStates.forEach((id, state) -> toggleTag.putBoolean(id.toString(), state));
        tag.put("Toggles", toggleTag);

        ListTag triedTag = new ListTag();
        triedClasses.forEach(id -> triedTag.add(StringTag.valueOf(id.toString())));
        tag.put("TriedClasses", triedTag);

        tag.putIntArray("ActiveCondSets", activeConditionalSets.stream().mapToInt(Integer::intValue).toArray());

        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ClassId")) {
            try {
                currentClassId = Identifier.parse(tag.getStringOr("ClassId", ""));
            } catch (Exception e) {
                currentClassId = null;
            }
        } else {
            currentClassId = null;
        }
        classAssignedTime = tag.getLongOr("AssignedTime", 0L);
        classLevel = tag.getIntOr("Level", 0);
        classExperience = tag.getIntOr("Experience", 0);
        resourceCurrent = tag.getFloatOr("Resource", 0f);
        lastClassChangeTime = tag.getLongOr("LastChangeTime", 0L);

        cooldowns.clear();
        CompoundTag cooldownTag = tag.getCompoundOrEmpty("Cooldowns");
        for (String key : cooldownTag.keySet()) {
            try {
                cooldowns.put(Identifier.parse(key), cooldownTag.getIntOr(key, 0));
            } catch (Exception ignored) {
            }
        }

        activeConditionalSets.clear();
        int[] conditionalArray = tag.getIntArray("ActiveCondSets").orElse(new int[0]);
        for (int i : conditionalArray) {
            activeConditionalSets.add(i);
        }

        toggleStates.clear();
        CompoundTag toggleTag = tag.getCompoundOrEmpty("Toggles");
        for (String key : toggleTag.keySet()) {
            try {
                toggleStates.put(Identifier.parse(key), toggleTag.getBooleanOr(key, false));
            } catch (Exception ignored) {
            }
        }

        triedClasses.clear();
        ListTag triedTag = tag.getListOrEmpty("TriedClasses");
        for (int i = 0; i < triedTag.size(); i++) {
            try {
                triedClasses.add(Identifier.parse(triedTag.getStringOr(i, "")));
            } catch (Exception ignored) {
            }
        }
    }

    public void writeSyncData(FriendlyByteBuf buf) {
        buf.writeBoolean(hasClass());
        if (hasClass()) {
            buf.writeIdentifier(currentClassId);
            buf.writeVarInt(classLevel);
            buf.writeVarInt(classExperience);
            buf.writeFloat(resourceCurrent);

            buf.writeVarInt(cooldowns.size());
            cooldowns.forEach((id, ticks) -> {
                buf.writeIdentifier(id);
                buf.writeVarInt(ticks);
            });

            buf.writeVarInt(toggleStates.size());
            toggleStates.forEach((id, state) -> {
                buf.writeIdentifier(id);
                buf.writeBoolean(state);
            });
        }
    }

    public void readSyncData(FriendlyByteBuf buf) {
        boolean has = buf.readBoolean();
        if (has) {
            currentClassId = buf.readIdentifier();
            classLevel = buf.readVarInt();
            classExperience = buf.readVarInt();
            resourceCurrent = buf.readFloat();

            cooldowns.clear();
            int cooldownCount = buf.readVarInt();
            for (int i = 0; i < cooldownCount; i++) {
                cooldowns.put(buf.readIdentifier(), buf.readVarInt());
            }

            toggleStates.clear();
            int toggleCount = buf.readVarInt();
            for (int i = 0; i < toggleCount; i++) {
                toggleStates.put(buf.readIdentifier(), buf.readBoolean());
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
        classLevel = 0;
        classExperience = 0;
        resourceCurrent = 0;
        cooldowns.clear();
        activeConditionalSets.clear();
        toggleStates.clear();
        lastClassChangeTime = 0;
        triedClasses.clear();
    }

    public PlayerClassData copy() {
        PlayerClassData copied = new PlayerClassData();
        copied.currentClassId = this.currentClassId;
        copied.classAssignedTime = this.classAssignedTime;
        copied.classLevel = this.classLevel;
        copied.classExperience = this.classExperience;
        copied.resourceCurrent = this.resourceCurrent;
        copied.lastClassChangeTime = this.lastClassChangeTime;
        copied.cooldowns = new HashMap<>(this.cooldowns);
        copied.activeConditionalSets = new HashSet<>(this.activeConditionalSets);
        copied.toggleStates = new HashMap<>(this.toggleStates);
        copied.triedClasses = new HashSet<>(this.triedClasses);
        return copied;
    }

    public static int experienceForLevel(int level, int baseExp) {
        return (int)(baseExp * Math.pow(level - 1, 1.5));
    }
}
