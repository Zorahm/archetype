package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class FireImmunityPassive extends AbstractPassiveAbility {

    private static final UUID HP_SCALING_UUID = UUID.nameUUIDFromBytes("archetype:fire_immunity_hp_scaling".getBytes());
    private int lastAppliedBonus = -1;

    public FireImmunityPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        // HP scaling from XP level: +2 per 25 levels, max +4
        int hpPerLevels = getInt("hp_per_levels", 0);
        if (hpPerLevels > 0) {
            float hpBonus = getFloat("hp_bonus", 2.0f);
            float maxHpBonus = getFloat("max_hp_bonus", 4.0f);

            int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
            int intervals = classLevel / hpPerLevels;
            float bonus = Math.min(intervals * hpBonus, maxHpBonus);
            int bonusInt = (int) bonus;

            if (bonusInt != lastAppliedBonus) {
                AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.removeModifier(HP_SCALING_UUID);
                    if (bonusInt > 0) {
                        attr.addTransientModifier(new AttributeModifier(
                                HP_SCALING_UUID, "archetype:hp_xp_scaling", bonus,
                                AttributeModifier.Operation.ADDITION));
                    }
                    lastAppliedBonus = bonusInt;
                }
            }
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.removeModifier(HP_SCALING_UUID);
        }
        lastAppliedBonus = -1;
    }

    @Override
    public boolean shouldCancelDamage(ServerPlayer player, DamageSource source) {
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
