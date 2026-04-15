package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class IsSneakingCondition implements Condition {
    public IsSneakingCondition(JsonObject params) {}

    @Override
    public boolean test(Player player) {
        return player.isShiftKeyDown();
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "is_sneaking");
    }
}
