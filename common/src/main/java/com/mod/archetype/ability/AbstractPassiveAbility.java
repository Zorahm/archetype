package com.mod.archetype.ability;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class AbstractPassiveAbility implements PassiveAbility {

    protected final JsonObject params;
    protected final boolean positive;
    protected final String nameKey;
    protected final String descriptionKey;

    protected AbstractPassiveAbility(JsonObject params, boolean positive, String nameKey, String descriptionKey) {
        this.params = params;
        this.positive = positive;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
    }

    @Override
    public void onApply(ServerPlayer player) {}

    @Override
    public void onRemove(ServerPlayer player) {}

    @Override
    public boolean isPositive() {
        return positive;
    }

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public String getDescriptionKey() {
        return descriptionKey;
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
}
