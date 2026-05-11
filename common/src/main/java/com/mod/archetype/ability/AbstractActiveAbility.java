package com.mod.archetype.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public abstract class AbstractActiveAbility implements ActiveAbility {

    protected final ActiveAbilityEntry entry;
    protected final JsonObject params;
    protected boolean active;

    protected AbstractActiveAbility(ActiveAbilityEntry entry) {
        this.entry = entry;
        this.params = entry.params();
        this.active = false;
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        // Check cooldown
        Identifier abilityId = Identifier.fromNamespaceAndPath(entry.type().getNamespace(), entry.slot());
        if (data.getCooldown(abilityId) > 0) {
            return false;
        }

        // Check resource (skip in creative)
        if (!player.isCreative() && entry.resourceCost() > 0) {
            if (data.getResourceCurrent() < entry.resourceCost()) {
                return false;
            }
        }

        // Check level
        if (entry.unlockLevel() > 0 && data.getClassLevel() < entry.unlockLevel()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
    }

    @Override
    public void tickActive(ServerPlayer player) {}

    @Override
    public String getSlot() {
        return entry.slot();
    }

    @Override
    public int getCooldownTicks() {
        return entry.cooldownTicks();
    }

    @Override
    public int getResourceCost() {
        return entry.resourceCost();
    }

    @Override
    public int getUnlockLevel() {
        return entry.unlockLevel();
    }

    @Override
    public String getNameKey() {
        return entry.nameKey();
    }

    @Override
    public String getDescriptionKey() {
        return entry.descriptionKey();
    }

    @Override
    public Identifier getIcon() {
        return entry.icon();
    }

    // JSON helper methods

    protected int getInt(String key, int defaultValue) {
        if (params.has(key) && params.get(key).isJsonPrimitive()) {
            return params.get(key).getAsInt();
        }
        return defaultValue;
    }

    protected float getFloat(String key, float defaultValue) {
        if (params.has(key) && params.get(key).isJsonPrimitive()) {
            return params.get(key).getAsFloat();
        }
        return defaultValue;
    }

    protected double getDouble(String key, double defaultValue) {
        if (params.has(key) && params.get(key).isJsonPrimitive()) {
            return params.get(key).getAsDouble();
        }
        return defaultValue;
    }

    protected String getString(String key, String defaultValue) {
        if (params.has(key) && params.get(key).isJsonPrimitive()) {
            return params.get(key).getAsString();
        }
        return defaultValue;
    }

    protected boolean getBool(String key, boolean defaultValue) {
        if (params.has(key) && params.get(key).isJsonPrimitive()) {
            return params.get(key).getAsBoolean();
        }
        return defaultValue;
    }

    protected float getLevelScaledFloat(String key, int classLevel, float defaultValue) {
        if (!params.has(key) || !params.get(key).isJsonArray()) return defaultValue;
        JsonArray array = params.getAsJsonArray(key);
        float result = defaultValue;
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            int level = entry.get("level").getAsInt();
            if (classLevel >= level) {
                result = entry.get("value").getAsFloat();
            }
        }
        return result;
    }
}
