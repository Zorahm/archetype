package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class NightVisionPassive extends AbstractPassiveAbility {

    private final int amplifier;

    public NightVisionPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.amplifier = getInt("amplifier", 0);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION, 400, amplifier, true, false, false
        ));
    }

    @Override
    public void onRemove(ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "night_vision");
    }
}
