package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.List;

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
            Identifier effectId = Identifier.parse(effectIdStr);
            BuiltInRegistries.MOB_EFFECT.get(effectId).ifPresent(effectHolder -> {
                if (player.hasEffect(effectHolder)) {
                    player.removeEffect(effectHolder);
                }
            });
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "effect_immunity");
    }
}
