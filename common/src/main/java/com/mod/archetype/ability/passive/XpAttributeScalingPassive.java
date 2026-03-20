package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Gradually removes class attribute penalties as player XP level increases.
 * At target_level, all penalties are fully compensated.
 * Params: target_level, health_bonus, attack_speed_bonus, attack_damage_bonus
 */
public class XpAttributeScalingPassive extends AbstractPassiveAbility {

    private static final UUID HP_UUID = UUID.nameUUIDFromBytes("archetype:xp_scaling_hp".getBytes());
    private static final UUID SPEED_UUID = UUID.nameUUIDFromBytes("archetype:xp_scaling_attack_speed".getBytes());
    private static final UUID DAMAGE_UUID = UUID.nameUUIDFromBytes("archetype:xp_scaling_attack_damage".getBytes());

    private final int targetLevel;
    private final float healthBonus;
    private final float attackSpeedBonus;
    private final float attackDamageBonus;
    private final int healthThreshold;
    private final int attackSpeedThreshold;
    private final int attackDamageThreshold;
    private int lastClassLevel = -1;

    public XpAttributeScalingPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.targetLevel = getInt("target_level", 40);
        this.healthBonus = getFloat("health_bonus", 0f);
        this.attackSpeedBonus = getFloat("attack_speed_bonus", 0f);
        this.attackDamageBonus = getFloat("attack_damage_bonus", 0f);
        this.healthThreshold = getInt("health_threshold", 0);
        this.attackSpeedThreshold = getInt("attack_speed_threshold", 0);
        this.attackDamageThreshold = getInt("attack_damage_threshold", 0);
    }

    @Override
    public void tick(ServerPlayer player) {
        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        if (classLevel == lastClassLevel) return;
        int prevLevel = lastClassLevel;
        lastClassLevel = classLevel;

        float ratio = Math.min(1.0f, (float) classLevel / targetLevel);

        if (healthBonus > 0) {
            double value = healthThreshold > 0
                    ? (classLevel >= healthThreshold ? healthBonus : 0)
                    : healthBonus * ratio;
            applyModifier(player, Attributes.MAX_HEALTH, HP_UUID, "archetype:xp_hp",
                    value, AttributeModifier.Operation.ADDITION);
            // Heal to match new max health when threshold is first crossed
            if (healthThreshold > 0 && prevLevel >= 0 && classLevel >= healthThreshold && prevLevel < healthThreshold) {
                player.heal(healthBonus);
            }
        }
        if (attackSpeedBonus > 0) {
            double value = attackSpeedThreshold > 0
                    ? (classLevel >= attackSpeedThreshold ? attackSpeedBonus : 0)
                    : attackSpeedBonus * ratio;
            applyModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID, "archetype:xp_atk_speed",
                    value, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (attackDamageBonus > 0) {
            double value = attackDamageThreshold > 0
                    ? (classLevel >= attackDamageThreshold ? attackDamageBonus : 0)
                    : attackDamageBonus * ratio;
            applyModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, "archetype:xp_atk_dmg",
                    value, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    }

    private void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                               UUID uuid, String name, double value, AttributeModifier.Operation op) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr == null) return;
        attr.removeModifier(uuid);
        if (value > 0.001) {
            attr.addTransientModifier(new AttributeModifier(uuid, name, value, op));
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, HP_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, SPEED_UUID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_UUID);
        lastClassLevel = -1;
    }

    private void removeModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr != null) attr.removeModifier(uuid);
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "xp_attribute_scaling");
    }
}
