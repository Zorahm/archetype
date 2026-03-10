package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class IsSprintingCondition implements Condition {
    public IsSprintingCondition(JsonObject params) {}

    @Override
    public boolean test(Player player) {
        return player.isSprinting();
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "is_sprinting");
    }
}
