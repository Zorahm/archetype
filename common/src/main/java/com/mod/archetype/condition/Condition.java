package com.mod.archetype.condition;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface Condition {

    boolean test(Player player);

    Identifier getType();
}
