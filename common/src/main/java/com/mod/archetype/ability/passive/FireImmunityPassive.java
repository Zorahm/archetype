package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FireImmunityPassive extends AbstractPassiveAbility {

    public FireImmunityPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        player.clearFire();
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE, 40, 0, true, false, false
        ));
    }

    @Override
    public void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
        // Fire damage cancellation is checked via shouldCancelDamage
    }

    /**
     * Returns true if this passive should cancel the given damage source.
     * Intended to be checked by the damage event handler.
     */
    public boolean shouldCancelDamage(DamageSource source) {
        return source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.LAVA)
                || source.is(DamageTypes.HOT_FLOOR);
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "fire_immunity");
    }
}
