package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class BloodDrainAbility extends AbstractActiveAbility {

    private final float damagePerTick;
    private final float healPercent;
    private final float resourceGainPerTick;
    private final int durationTicks;
    private final float range;
    private UUID targetUUID;
    private int remainingTicks;

    public BloodDrainAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.damagePerTick = getFloat("damage_per_tick", 1.0f);
        this.healPercent = getFloat("heal_percent", 0.5f);
        this.resourceGainPerTick = getFloat("resource_gain_per_tick", 2.0f);
        this.durationTicks = getInt("duration_ticks", 40);
        this.range = getFloat("range", 4.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != player, range * range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity)) {
            return ActivationResult.FAILED;
        }
        targetUUID = hit.getEntity().getUUID();
        remainingTicks = durationTicks;
        active = true;
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            forceDeactivate(player);
            return;
        }
        Entity entity = level.getEntity(targetUUID);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || target.distanceTo(player) > range) {
            forceDeactivate(player);
            return;
        }
        target.hurt(player.damageSources().playerAttack(player), damagePerTick);
        player.heal(damagePerTick * healPercent);
        if (resourceGainPerTick > 0) {
            PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
            data.setResourceCurrent(Math.min(data.getResourceCurrent() + resourceGainPerTick, 100f));
        }
        remainingTicks--;
        if (remainingTicks <= 0) {
            forceDeactivate(player);
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        targetUUID = null;
        remainingTicks = 0;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "blood_drain");
    }
}
