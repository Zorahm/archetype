package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class NoFallDamagePassive extends AbstractPassiveAbility {

    public NoFallDamagePassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior
    }

    @Override
    public void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
        // Fall damage cancellation is checked via shouldCancelDamage
    }

    /**
     * Returns true if this passive should cancel the given damage source.
     * Intended to be checked by the damage event handler.
     */
    public boolean shouldCancelDamage(DamageSource source) {
        return source.is(DamageTypes.FALL);
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "no_fall_damage");
    }
}
