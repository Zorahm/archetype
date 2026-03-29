package com.mod.archetype.ability.active;

import net.minecraft.world.entity.projectile.EvokerFangs;

import java.util.Collections;
import java.util.WeakHashMap;

/**
 * Maps spawned EvokerFangs entities to their custom damage values.
 * WeakHashMap so entries are GC'd with the entity.
 */
public class FangsDamageRegistry {

    public static final java.util.Map<EvokerFangs, Float> DAMAGE =
            Collections.synchronizedMap(new WeakHashMap<>());
}
