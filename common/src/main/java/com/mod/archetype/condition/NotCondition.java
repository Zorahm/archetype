package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class NotCondition implements Condition {

    private static final ResourceLocation TYPE = new ResourceLocation(Archetype.MOD_ID, "not");

    private final Condition child;

    public NotCondition(Condition child) {
        this.child = child;
    }

    @Override
    public boolean test(Player player) {
        return !child.test(player);
    }

    @Override
    public ResourceLocation getType() {
        return TYPE;
    }
}
