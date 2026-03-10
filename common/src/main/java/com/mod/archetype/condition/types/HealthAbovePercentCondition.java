package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class HealthAbovePercentCondition implements Condition {
    private final float percent;

    public HealthAbovePercentCondition(JsonObject params) {
        this.percent = params.has("percent") ? params.get("percent").getAsFloat() : 0.8f;
    }

    @Override
    public boolean test(Player player) {
        return player.getHealth() / player.getMaxHealth() > percent;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "health_above_percent");
    }
}
