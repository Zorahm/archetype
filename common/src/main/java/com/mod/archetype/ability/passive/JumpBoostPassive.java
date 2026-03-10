package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class JumpBoostPassive extends AbstractPassiveAbility {

    private final float multiplier;
    private final int amplifier;

    public JumpBoostPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.multiplier = getFloat("multiplier", 0.5f);
        // Convert multiplier to amplifier: each level adds ~0.1 to jump height
        // amplifier 0 = Jump Boost I, amplifier 1 = Jump Boost II, etc.
        this.amplifier = Math.max(0, Math.round(multiplier / 0.5f) - 1);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        player.addEffect(new MobEffectInstance(
                MobEffects.JUMP, 40, amplifier, true, false, false
        ));
    }

    @Override
    public void onRemove(ServerPlayer player) {
        player.removeEffect(MobEffects.JUMP);
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "jump_boost");
    }
}
