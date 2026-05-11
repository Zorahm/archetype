package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class UnderwaterCondition implements Condition {
    public UnderwaterCondition(JsonObject params) {}

    @Override
    public boolean test(Player player) {
        return player.isUnderWater();
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "underwater");
    }
}
