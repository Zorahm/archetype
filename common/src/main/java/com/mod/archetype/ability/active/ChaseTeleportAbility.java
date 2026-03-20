package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ChaseTeleportAbility extends AbstractActiveAbility {

    private final float baseDamage;
    private final float maxDamage;
    private final float damageGrowthPer5Levels;

    public ChaseTeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.baseDamage = getFloat("base_damage", 3.0f);
        this.maxDamage = getFloat("max_damage", 10.0f);
        this.damageGrowthPer5Levels = getFloat("damage_growth_per_5_levels", 0.5f);
    }

    private float getEffectiveDamage(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        float bonus = (level / 5) * damageGrowthPer5Levels;
        return Math.min(baseDamage + bonus, maxDamage);
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

        float damage = getEffectiveDamage(player);
        target.hurt(player.damageSources().playerAttack(player), damage);

        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "chase_teleport");
    }
}
