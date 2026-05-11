package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class NotCondition implements Condition {

    private static final Identifier TYPE = Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "not");

    private final Condition child;

    public NotCondition(Condition child) {
        this.child = child;
    }

    @Override
    public boolean test(Player player) {
        return !child.test(player);
    }

    @Override
    public Identifier getType() {
        return TYPE;
    }
}
