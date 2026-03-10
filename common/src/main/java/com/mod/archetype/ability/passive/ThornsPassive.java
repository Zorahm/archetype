package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class ThornsPassive extends AbstractPassiveAbility {

    private final float reflectPercent;

    public ThornsPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.reflectPercent = getFloat("reflect_percent", 0.15f);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior
    }

    @Override
    public void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity attacker) {
            float reflectedDamage = amount * reflectPercent;
            if (reflectedDamage > 0) {
                attacker.hurt(player.damageSources().thorns(player), reflectedDamage);
            }
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "thorns_passive");
    }
}
