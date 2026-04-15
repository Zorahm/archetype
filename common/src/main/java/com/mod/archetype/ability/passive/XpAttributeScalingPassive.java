package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Gradually removes class attribute penalties as player XP level increases.
 * At target_level, all penalties are fully compensated.
 * Params: target_level, health_bonus, attack_speed_bonus, attack_damage_bonus
 */
public class XpAttributeScalingPassive extends AbstractPassiveAbility {

    private static final Identifier HP_ID = Identifier.fromNamespaceAndPath("archetype", "xp_scaling_hp");
    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath("archetype", "xp_scaling_attack_speed");
    private static final Identifier DAMAGE_ID = Identifier.fromNamespaceAndPath("archetype", "xp_scaling_attack_damage");

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
            applyModifier(player, Attributes.MAX_HEALTH, HP_ID,
                    value, AttributeModifier.Operation.ADD_VALUE);
            // Heal to match new max health when threshold is first crossed
            if (healthThreshold > 0 && prevLevel >= 0 && classLevel >= healthThreshold && prevLevel < healthThreshold) {
                player.heal(healthBonus);
            }
        }
        if (attackSpeedBonus > 0) {
            double value = attackSpeedThreshold > 0
                    ? (classLevel >= attackSpeedThreshold ? attackSpeedBonus : 0)
                    : attackSpeedBonus * ratio;
            applyModifier(player, Attributes.ATTACK_SPEED, SPEED_ID,
                    value, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
        if (attackDamageBonus > 0) {
            double value = attackDamageThreshold > 0
                    ? (classLevel >= attackDamageThreshold ? attackDamageBonus : 0)
                    : attackDamageBonus * ratio;
            applyModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID,
                    value, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
    }

    private void applyModifier(ServerPlayer player, Holder<Attribute> attribute,
                               Identifier id, double value, AttributeModifier.Operation op) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr == null) return;
        attr.removeModifier(id);
        if (value > 0.001) {
            attr.addTransientModifier(new AttributeModifier(id, value, op));
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, HP_ID);
        removeModifier(player, Attributes.ATTACK_SPEED, SPEED_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        lastClassLevel = -1;
    }

    private void removeModifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id) {
        AttributeInstance attr = player.getAttribute(attribute);
        if (attr != null) attr.removeModifier(id);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "xp_attribute_scaling");
    }
}
