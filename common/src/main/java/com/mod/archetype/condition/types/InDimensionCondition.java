package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class InDimensionCondition implements Condition {
    private final Identifier dimensionId;

    public InDimensionCondition(JsonObject params) {
        String dim = params.has("dimension") ? params.get("dimension").getAsString() : "minecraft:overworld";
        this.dimensionId = Identifier.parse(dim);
    }

    @Override
    public boolean test(Player player) {
        return player.level().dimension().identifier().equals(dimensionId);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "in_dimension");
    }
}
