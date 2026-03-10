package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class InDimensionCondition implements Condition {
    private final ResourceLocation dimensionId;

    public InDimensionCondition(JsonObject params) {
        String dim = params.has("dimension") ? params.get("dimension").getAsString() : "minecraft:overworld";
        this.dimensionId = new ResourceLocation(dim);
    }

    @Override
    public boolean test(Player player) {
        return player.level().dimension().location().equals(dimensionId);
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "in_dimension");
    }
}
