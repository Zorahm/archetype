package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class OrCondition implements Condition {

    private static final ResourceLocation TYPE = new ResourceLocation(Archetype.MOD_ID, "or");

    private final List<Condition> children;

    public OrCondition(List<Condition> children) {
        this.children = List.copyOf(children);
    }

    @Override
    public boolean test(Player player) {
        for (Condition child : children) {
            if (child.test(player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
