package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class UnderwaterCondition implements Condition {
    public UnderwaterCondition(JsonObject params) {}

    @Override
    public boolean test(Player player) {
        return player.isUnderWater();
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "underwater");
    }
}
