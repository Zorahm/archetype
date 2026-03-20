package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class AntigravityThrowAbility extends AbstractActiveAbility {

    private final int levitationDuration;
    private final int levitationAmplifier;
    private final float baseFallDamageMultiplier;
    private final float maxFallDamageMultiplier;
    private final float fallDamageGrowthPer5Levels;

    private LivingEntity currentTarget;
    private int phase; // 0=idle, 1=levitating, 2=slamming
    private int ticksRemaining;

    public AntigravityThrowAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.levitationDuration = getInt("levitation_duration", 40);
        this.levitationAmplifier = getInt("levitation_amplifier", 2);
        this.baseFallDamageMultiplier = getFloat("base_fall_damage_multiplier", 1.2f);
        this.maxFallDamageMultiplier = getFloat("max_fall_damage_multiplier", 3.0f);
        this.fallDamageGrowthPer5Levels = getFloat("fall_damage_growth_per_5_levels", 0.1f);
    }

    private float getEffectiveFallDamageMultiplier(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        float bonus = (level / 5) * fallDamageGrowthPer5Levels;
        return Math.min(baseFallDamageMultiplier + bonus, maxFallDamageMultiplier);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        LivingEntity target = player.getLastHurtMob();
        if (target == null || !target.isAlive()) {
            return ActivationResult.FAILED;
        }

        double distSq = player.distanceToSqr(target);
        if (distSq > 32 * 32) {
            return ActivationResult.FAILED;
        }

        currentTarget = target;
        phase = 1;
        ticksRemaining = levitationDuration;
        active = true;

        target.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, levitationDuration + 10, levitationAmplifier,
                false, true, true
        ));

        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (currentTarget == null || !currentTarget.isAlive()) {
            cleanup();
            return;
        }

        if (phase == 1) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                phase = 2;
                ticksRemaining = 60;
                currentTarget.removeEffect(MobEffects.LEVITATION);
                currentTarget.setDeltaMovement(new Vec3(0, -2.0, 0));
                currentTarget.hurtMarked = true;
            }
        } else if (phase == 2) {
            ticksRemaining--;
            boolean onGround = currentTarget.onGround();
            if (onGround || ticksRemaining <= 0) {
                float multiplier = getEffectiveFallDamageMultiplier(player);
                float slamDamage = currentTarget.fallDistance * multiplier;
                if (slamDamage > 0) {
                    currentTarget.hurt(player.damageSources().playerAttack(player), slamDamage);
                }
                cleanup();
            }
        }
    }

    private void cleanup() {
        if (currentTarget != null) {
            currentTarget.removeEffect(MobEffects.LEVITATION);
            currentTarget = null;
        }
        phase = 0;
        active = false;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        cleanup();
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "antigravity_throw");
    }
}
