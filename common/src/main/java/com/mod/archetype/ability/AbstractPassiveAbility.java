package com.mod.archetype.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPassiveAbility implements PassiveAbility {

    protected final PassiveAbilityEntry entry;
    protected final JsonObject params;

    protected AbstractPassiveAbility(PassiveAbilityEntry entry) {
        this.entry = entry;
        this.params = entry.params();
    }

    @Override
    public void onApply(ServerPlayer player) {}

    @Override
    public void onRemove(ServerPlayer player) {}

    @Override
    public boolean isPositive() {
        return entry.positive();
    }

    @Override
    public String getNameKey() {
        return entry.nameKey();
    }

    @Override
    public String getDescriptionKey() {
        return entry.descriptionKey();
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

    protected List<String> getStringList(String key) {
        List<String> result = new ArrayList<>();
        if (params.has(key) && params.get(key).isJsonArray()) {
            JsonArray arr = params.getAsJsonArray(key);
            for (int i = 0; i < arr.size(); i++) {
                result.add(arr.get(i).getAsString());
            }
        }
        return result;
    }
}
