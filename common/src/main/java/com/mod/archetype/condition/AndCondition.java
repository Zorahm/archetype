package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class AndCondition implements Condition {

    private static final Identifier TYPE = Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "and");

    private final List<Condition> children;

    public AndCondition(List<Condition> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public boolean test(Player player) {
        for (Condition child : children) {
            if (!child.test(player)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Identifier getType() {
        return TYPE;
    }
}
