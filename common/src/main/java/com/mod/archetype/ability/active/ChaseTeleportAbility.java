package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ChaseTeleportAbility extends AbstractActiveAbility {

    private final float baseDamage;
    private final float baseSelfDamage;

    public ChaseTeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.baseDamage = getFloat("base_damage", 1.0f);
        this.baseSelfDamage = getFloat("self_damage", 3.0f);
    }

    private float getEffectiveDamage(int level) {
        float dmg = baseDamage;
        if (level >= 20) dmg += 1.0f;
        if (level >= 40) dmg += 1.0f;
        return dmg;
    }

    private float getEffectiveSelfDamage(int level) {
        float dmg = baseSelfDamage;
        if (level >= 20) dmg -= 1.0f;
        if (level >= 40) dmg -= 1.0f;
        if (level >= 60) dmg -= 1.0f;
        return Math.max(0, dmg);
    }

    @Override
    public int getCooldownTicks(ServerPlayer player) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int cd = entry.cooldownTicks();
        if (level >= 20) cd -= 20;
        if (level >= 40) cd -= 20;
        if (level >= 60) cd -= 20;
        return Math.max(20, cd);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        LivingEntity target = player.getLastHurtMob();
        if (target == null || !target.isAlive()) {
            return ActivationResult.FAILED;
        }

        double distSq = player.distanceToSqr(target);
        if (distSq > 64 * 64) {
            return ActivationResult.FAILED;
        }

        Vec3 targetPos = target.position();
        Vec3 offset = player.position().subtract(targetPos).normalize();
        double teleX = targetPos.x + offset.x * 1.5;
        double teleY = targetPos.y;
        double teleZ = targetPos.z + offset.z * 1.5;

        player.teleportTo(teleX, teleY, teleZ);

        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        target.hurt(player.damageSources().playerAttack(player), getEffectiveDamage(level));

        float selfDamage = getEffectiveSelfDamage(level);
        if (selfDamage > 0) {
            player.hurt(player.damageSources().magic(), selfDamage);
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "chase_teleport");
    }
}
