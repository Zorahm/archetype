package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public class FireAttackPassive extends AbstractPassiveAbility {
    public FireAttackPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {}

    @Override
    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        if (player.isOnFire()) {
            target.igniteForSeconds(5);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "fire_attack");
    }
}
