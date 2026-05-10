package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RageDashAbility extends AbstractActiveAbility {

    // Base values
    private static final float BASE_DRAIN_PER_SECOND = 10.0f;
    private static final float ACTIVATION_COST = 30.0f;
    private static final float CANCEL_COST = 0.0f;
    private static final int BASE_COOLDOWN_TICKS = 200; // 10 seconds
    private static final float BASE_COLLISION_DAMAGE = 6.0f;
    private static final double BASE_SHOCKWAVE_RADIUS = 1.0;
    private static final double BASE_FEATHER_RADIUS = 2.0;
    private static final double BASE_FEATHER_JUMP = 0.5; // ~1 block
    private static final int HEARTBEAT_INTERVAL = 10;

    private boolean featherMode = false;
    private boolean hasJumped = false;
    private int drainTickCounter = 0;
    private int soundTickCounter = 0;

    // Cached scaled values for current activation
    private float currentDrain;
    private float currentDamage;
    private double currentRadius;
    private int currentCooldown;
    private float currentKnockbackReduction;

    public RageDashAbility(ActiveAbilityEntry entry) {
        super(entry);
    }

    // --- Level Progression (classLevel = XP level) ---

    private float computeDrainPerSecond(int classLevel) {
        // -1 at XP 5, 10, 15, 20, 40
        int reduction = (classLevel >= 5 ? 1 : 0) + (classLevel >= 10 ? 1 : 0)
                + (classLevel >= 15 ? 1 : 0) + (classLevel >= 20 ? 1 : 0)
                + (classLevel >= 40 ? 1 : 0);
        return BASE_DRAIN_PER_SECOND - reduction;
    }

    private float computeDamage(int classLevel) {
        // +1 at XP 10, 20, 30
        int bonus = (classLevel >= 10 ? 1 : 0) + (classLevel >= 20 ? 1 : 0)
                + (classLevel >= 30 ? 1 : 0);
        return BASE_COLLISION_DAMAGE + bonus;
    }

    private double computeRadius(int classLevel) {
        // +1 at XP 15, 30
        int bonus = (classLevel >= 15 ? 1 : 0) + (classLevel >= 30 ? 1 : 0);
        return BASE_SHOCKWAVE_RADIUS + bonus;
    }

    private double computeFeatherJump(int classLevel) {
        int bonus = (classLevel >= 5 ? 1 : 0) + (classLevel >= 10 ? 1 : 0)
                + (classLevel >= 15 ? 1 : 0) + (classLevel >= 20 ? 1 : 0)
                + (classLevel >= 50 ? 1 : 0);
        double blocks = 4.0 + bonus;
        return 0.4 * Math.sqrt(blocks);
    }

    private float computeKnockbackReduction(int classLevel) {
        return getLevelScaledFloat("knockback_scale", classLevel, 1.0f);
    }

    private int computeCooldown(int classLevel) {
        // -1s at XP 10, 20, 30, 40, 50
        int reduction = (classLevel >= 10 ? 1 : 0) + (classLevel >= 20 ? 1 : 0)
                + (classLevel >= 30 ? 1 : 0) + (classLevel >= 40 ? 1 : 0)
                + (classLevel >= 50 ? 1 : 0);
        return BASE_COOLDOWN_TICKS - (reduction * 20);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        float currentRage = data.getResourceCurrent();

        // Cancel rage if already active (free)
        if (active) {
            stopRage(player);
            return ActivationResult.SUCCESS;
        }

        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        float knockbackReduction = computeKnockbackReduction(classLevel);

        // Activation cost: 30, reduced to 20 at XP 25
        float activationCost = classLevel >= 25 ? 20.0f : ACTIVATION_COST;
        if (currentRage < activationCost) return ActivationResult.NOT_ENOUGH_RESOURCE;

        // Deduct activation cost
        data.setResourceCurrent(currentRage - activationCost);

        // Cache progression values
        currentDrain = computeDrainPerSecond(classLevel);
        currentDamage = computeDamage(classLevel);
        currentCooldown = computeCooldown(classLevel);
        currentKnockbackReduction = knockbackReduction;

        // Radius: feather mode uses BASE_FEATHER_RADIUS, normal mode uses BASE_SHOCKWAVE_RADIUS
        currentRadius = featherMode
                ? BASE_FEATHER_RADIUS + (classLevel >= 15 ? 1 : 0) + (classLevel >= 30 ? 1 : 0)
                : computeRadius(classLevel);

        // Check offhand for feather mode
        if (player.getOffhandItem().is(Items.FEATHER)) {
            featherMode = true;
            player.getOffhandItem().shrink(1);
        } else {
            featherMode = false;
        }

        hasJumped = false;
        drainTickCounter = 0;
        soundTickCounter = 0;
        active = true;

        if (featherMode) {
            double jumpVelocity = computeFeatherJump(classLevel);
            player.setDeltaMovement(player.getDeltaMovement().add(0, jumpVelocity, 0));
            player.hurtMarked = true;
            hasJumped = true;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100, 0, true, false, false));
            applyForwardMovement(player);
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        // Heartbeat sound every 10 ticks
        soundTickCounter++;
        if (soundTickCounter >= HEARTBEAT_INTERVAL) {
            soundTickCounter = 0;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.1f);
        }

        if (featherMode) {
            tickFeatherMode(player);
            return;
        }

        // Normal rage mode: drain resource every second (20 ticks)
        drainTickCounter++;
        if (drainTickCounter >= 20) {
            drainTickCounter = 0;
            float current = data.getResourceCurrent();
            if (current < currentDrain) {
                stopRage(player);
                return;
            }
            data.setResourceCurrent(current - currentDrain);
        }

        // Keep player moving forward
        applyForwardMovement(player);
        player.fallDistance = 0;

        // Flame particle trail
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY(), player.getZ(),
                    1, 0.2, 0, 0.2, 0.1);
        }

        // Check collision with entities
        AABB hitbox = player.getBoundingBox().inflate(0.6);
        List<Entity> entities = player.level().getEntities(player, hitbox,
                e -> e instanceof LivingEntity && e.isAlive());

        if (!entities.isEmpty()) {
            // Hit enemies — deal damage
            for (Entity entity : entities) {
                entity.hurt(player.damageSources().playerAttack(player), currentDamage);
            }

            // Knockback in shockwave radius
            AABB explosionArea = player.getBoundingBox().inflate(currentRadius);
            List<Entity> nearbyEntities = player.level().getEntities(player, explosionArea,
                    e -> e instanceof LivingEntity && e.isAlive());
            for (Entity entity : nearbyEntities) {
                if (entity instanceof LivingEntity living) {
                    Vec3 knockDir = entity.position().subtract(player.position()).normalize();
                    living.knockback(1.5f * currentKnockbackReduction, -knockDir.x, -knockDir.z);
                }
            }

            stopRage(player);
        }
    }

    private void tickFeatherMode(ServerPlayer player) {
        player.fallDistance = 0;

        if (hasJumped && player.onGround()) {
            // Shockwave on landing
            AABB area = player.getBoundingBox().inflate(currentRadius);
            List<Entity> entities = player.level().getEntities(player, area,
                    e -> e instanceof LivingEntity && e.isAlive());
            for (Entity entity : entities) {
                if (entity.distanceTo(player) <= currentRadius) {
                    entity.hurt(player.damageSources().playerAttack(player), currentDamage);
                    if (entity instanceof LivingEntity living) {
                        Vec3 knockDir = entity.position().subtract(player.position()).normalize();
                        living.knockback(1.5f * currentKnockbackReduction, -knockDir.x, -knockDir.z);
                    }
                }
            }
            stopRage(player);
        }
    }

    private void applyForwardMovement(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontalDir = new Vec3(look.x, 0, look.z).normalize();
        double speed = 0.35;
        Vec3 currentMotion = player.getDeltaMovement();
        player.setDeltaMovement(horizontalDir.x * speed, currentMotion.y, horizontalDir.z * speed);
        player.hurtMarked = true;
    }

    private void stopRage(ServerPlayer player) {
        active = false;
        drainTickCounter = 0;
        soundTickCounter = 0;

        // Remove speed effect
        player.removeEffect(MobEffects.SPEED);

        // Explosion sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 1.1f);

        // Explosion particle at end
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + 1, player.getZ(),
                    1, 0, 0, 0, 1);
        }

        // Set cooldown (scaled by XP level)
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        Identifier abilityId = Identifier.fromNamespaceAndPath("archetype", entry.slot());
        data.setCooldown(abilityId, currentCooldown);
    }

    @Override
    public boolean managesCooldown() {
        return true;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        drainTickCounter = 0;
        soundTickCounter = 0;
        player.removeEffect(MobEffects.SPEED);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "rage_dash");
    }
}
