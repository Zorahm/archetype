package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class LifestealPassive extends AbstractPassiveAbility {

    private final float percentOfDamage;
    private final boolean onlyMelee;

    public LifestealPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.percentOfDamage = getFloat("percent_of_damage", 0.2f);
        this.onlyMelee = getBool("only_melee", true);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior
    }

    @Override
    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        if (!(target instanceof LivingEntity livingTarget)) return;

        if (onlyMelee && !source.is(DamageTypes.PLAYER_ATTACK)) return;

        // Use the source damage amount from the attack
        float attackDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float healAmount = attackDamage * percentOfDamage;

        if (healAmount > 0) {
            player.heal(healAmount);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "lifesteal");
    }
}
