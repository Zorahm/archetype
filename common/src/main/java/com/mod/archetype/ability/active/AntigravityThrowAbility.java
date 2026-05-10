package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class AntigravityThrowAbility extends AbstractActiveAbility {

    private final int levitationDuration;
    private final int levitationAmplifier;
    private final float baseDamage;
    private final int baseSelfSlownessAmplifier;

    private LivingEntity currentTarget;
    private ServerPlayer ownerRef;
    private int phase; // 0=idle, 1=levitating, 2=slamming
    private int ticksRemaining;

    public AntigravityThrowAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.levitationDuration = getInt("levitation_duration", 40);
        this.levitationAmplifier = getInt("levitation_amplifier", 2);
        this.baseDamage = getFloat("base_damage", 1.0f);
        this.baseSelfSlownessAmplifier = getInt("self_slowness_amplifier", 2);
    }

    private float getEffectiveDamage(int level) {
        float dmg = baseDamage;
        if (level >= 10) dmg += 1.0f;
        if (level >= 20) dmg += 1.0f;
        if (level >= 30) dmg += 1.0f;
        if (level >= 40) dmg += 1.0f;
        if (level >= 60) dmg += 1.0f;
        return dmg;
    }

    private int getEffectiveSelfSlownessAmplifier(int level) {
        int amp = baseSelfSlownessAmplifier;
        if (level >= 10) amp -= 1;
        if (level >= 30) amp -= 1;
        if (level >= 50) amp -= 1;
        return amp;
    }

    @Override
    public int getCooldownTicks(ServerPlayer player) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int cd = entry.cooldownTicks();
        if (level >= 10) cd -= 20;
        if (level >= 20) cd -= 20;
        if (level >= 30) cd -= 20;
        if (level >= 40) cd -= 20;
        if (level >= 50) cd -= 20;
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
        if (distSq > 32 * 32) {
            return ActivationResult.FAILED;
        }

        currentTarget = target;
        ownerRef = player;
        phase = 1;
        ticksRemaining = levitationDuration;
        active = true;

        target.addEffect(new MobEffectInstance(
                MobEffects.LEVITATION, levitationDuration + 10, levitationAmplifier,
                false, true, true
        ));

        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int slownessAmp = getEffectiveSelfSlownessAmplifier(level);
        if (slownessAmp >= 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS, levitationDuration + 70, slownessAmp,
                    false, true, true
            ));
        }

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
                int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
                currentTarget.hurt(player.damageSources().playerAttack(player), getEffectiveDamage(level));
                cleanup();
            }
        }
    }

    private void cleanup() {
        if (currentTarget != null) {
            currentTarget.removeEffect(MobEffects.LEVITATION);
            currentTarget = null;
        }
        if (ownerRef != null) {
            ownerRef.removeEffect(MobEffects.SLOWNESS);
            ownerRef = null;
        }
        phase = 0;
        active = false;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        cleanup();
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "antigravity_throw");
    }
}
