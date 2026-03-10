package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EffectImmunityPassive extends AbstractPassiveAbility {

    private final List<String> effectIds;

    public EffectImmunityPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.effectIds = getStringList("effects");
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        for (String effectIdStr : effectIds) {
            ResourceLocation effectId = new ResourceLocation(effectIdStr);
            Optional<MobEffect> effectOpt = BuiltInRegistries.MOB_EFFECT.getOptional(effectId);
            effectOpt.ifPresent(effect -> {
                if (player.hasEffect(effect)) {
                    player.removeEffect(effect);
                }
            });
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "effect_immunity");
    }
}
