package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViDashAbility extends AbstractActiveAbility {

    private final int dashTicks;
    private final int noFallTicks;
    private int remainingDashTicks;
    private int remainingNoFall;
    private final Set<Integer> hitEntities = new HashSet<>();
    private Vec3 dashDirection = Vec3.ZERO;

    // Client-sent movement direction (set before activate() by handler)
    private float clientDirX;
    private float clientDirZ;

    // Offhand modifier state for current dash
    private boolean currentFireTrail;
    private boolean currentWitherTrail;
    private boolean currentSnowSlow;

    // Scaled values for current dash (computed once at activation from XP level)
    private float currentDashDamage;
    private int currentEffectDurationTicks;
    private int currentEffectAmplifier;
    private int currentResistanceAmplifier;
    private float currentDashSpeed;

    // Fire immunity after blaze powder dash
    private int remainingFireImmunityTicks;

    // Charge tracking
    private int charges = -1; // -1 = not initialized
    private int maxChargesBase;
    private long lastRefillTime = 0;
    private static final int CHARGE_REFILL_TICKS_BASE = 180; // 9 seconds base

    public ViDashAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.dashTicks = getInt("dash_ticks", 5);
        this.noFallTicks = getInt("no_fall_ticks", 20);
        this.maxChargesBase = getInt("charges", 1);
    }

    @Override
    public void setClientMoveDirection(float dirX, float dirZ) {
        this.clientDirX = dirX;
        this.clientDirZ = dirZ;
    }

    private int computeRefillTicks(ServerPlayer player) {
        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int reduction = (classLevel >= 15 ? 1 : 0) + (classLevel >= 30 ? 1 : 0) + (classLevel >= 50 ? 1 : 0);
        return CHARGE_REFILL_TICKS_BASE - (reduction * 20);
    }

    private int computeMaxCharges(ServerPlayer player) {
        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        // +1 charge at XP 20, +1 at XP 40
        int extra = (classLevel >= 20 ? 1 : 0) + (classLevel >= 40 ? 1 : 0);
        return maxChargesBase + extra;
    }

    private void refillCharges(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        int max = computeMaxCharges(player);
        if (charges == -1) {
            charges = max;
            lastRefillTime = gameTime;
            return;
        }
        int refillTicks = computeRefillTicks(player);
        if (charges < max && gameTime - lastRefillTime >= refillTicks) {
            int gained = (int) ((gameTime - lastRefillTime) / refillTicks);
            charges = Math.min(max, charges + gained);
            lastRefillTime = gameTime;
        }
        if (charges > max) {
            charges = max;
        }
    }

    @Override
    public boolean managesCooldown() {
        return true;
    }

    @Override
    public int getCharges(ServerPlayer player) {
        refillCharges(player);
        return charges;
    }

    @Override
    public int getMaxCharges(ServerPlayer player) {
        return computeMaxCharges(player);
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (entry.unlockLevel() > 0 && data.getClassLevel() < entry.unlockLevel()) {
            return false;
        }
        refillCharges(player);
        return charges > 0;
    }

    private Vec3 computeDashDirection(ServerPlayer player) {
        // Use client-sent movement direction if available
        double lenSq = clientDirX * clientDirX + clientDirZ * clientDirZ;
        if (lenSq > 0.01) {
            double len = Math.sqrt(lenSq);
            return new Vec3(clientDirX / len, 0, clientDirZ / len);
        }

        // Fallback: look direction (player standing still)
        float yRot = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yRot), 0, Mth.cos(yRot));
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        charges--;
        lastRefillTime = player.level().getGameTime();

        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();

        // Compute scaled values (classLevel = XP level)
        // Damage: +1 at XP 10, 30
        float baseDamage = getFloat("damage", 1.0f);
        int damageBonus = (classLevel >= 10 ? 1 : 0) + (classLevel >= 30 ? 1 : 0);
        currentDashDamage = baseDamage + damageBonus;

        // Resistance: +1 at XP 15, 30
        currentResistanceAmplifier = (classLevel >= 15 ? 1 : 0) + (classLevel >= 30 ? 1 : 0);

        // Effect duration: base 1s + 1s at XP 10, 20, 30
        int durationBonus = (classLevel >= 10 ? 1 : 0) + (classLevel >= 20 ? 1 : 0) + (classLevel >= 30 ? 1 : 0);
        currentEffectDurationTicks = (1 + durationBonus) * 20;

        // Effect amplifier (slowness/wither): +1 at XP 20
        currentEffectAmplifier = classLevel >= 20 ? 1 : 0;

        // Determine offhand modifiers
        ItemStack offhand = player.getOffhandItem();
        currentFireTrail = offhand.is(Items.BLAZE_POWDER) && classLevel >= 20;
        currentWitherTrail = offhand.is(Items.WITHER_ROSE);
        currentSnowSlow = offhand.is(Items.SNOW_BLOCK);

        // Consume offhand item
        if (currentFireTrail || currentWitherTrail || currentSnowSlow) {
            offhand.shrink(1);
        }

        // Movement direction from client WASD input, fallback to look direction
        dashDirection = computeDashDirection(player);

        currentDashSpeed = getFloat("dash_speed", 2.0f);
        player.setDeltaMovement(dashDirection.scale(currentDashSpeed));
        player.hurtMarked = true;
        active = true;
        remainingDashTicks = dashTicks;
        remainingNoFall = noFallTicks;
        remainingFireImmunityTicks = currentFireTrail ? 20 : 0;
        hitEntities.clear();

        // Resistance during dash
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, currentResistanceAmplifier, true, false, false));

        // Sound effect
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Dragon breath particle
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    player.getX(), player.getY() + 1, player.getZ(),
                    15, 0.2, 0.5, 0.2, 0.1);
        }

        // Clear client direction after use
        clientDirX = 0;
        clientDirZ = 0;

        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (remainingDashTicks > 0) {
            // Force movement along dash direction
            player.setDeltaMovement(dashDirection.scale(currentDashSpeed));
            player.hurtMarked = true;
            remainingDashTicks--;

            // Project where the player will be after this tick's velocity is applied
            Vec3 currentPos = player.position();
            Vec3 projectedPos = currentPos.add(dashDirection.scale(currentDashSpeed));

            // Sweep-check: AABB covering path from current to projected position
            double minX = Math.min(currentPos.x, projectedPos.x) - 1.0;
            double minY = currentPos.y - 0.5;
            double minZ = Math.min(currentPos.z, projectedPos.z) - 1.0;
            double maxX = Math.max(currentPos.x, projectedPos.x) + 1.0;
            double maxY = currentPos.y + 2.0;
            double maxZ = Math.max(currentPos.z, projectedPos.z) + 1.0;
            AABB sweepBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            List<Entity> entities = player.level().getEntities(player, sweepBox, e -> e instanceof LivingEntity && e.isAlive());
            for (Entity entity : entities) {
                if (hitEntities.contains(entity.getId())) continue;
                hitEntities.add(entity.getId());

                entity.hurt(player.damageSources().playerAttack(player), currentDashDamage);

                if (currentWitherTrail && entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, currentEffectDurationTicks, currentEffectAmplifier));
                }
                if (currentSnowSlow && entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, currentEffectDurationTicks, currentEffectAmplifier));
                }
            }

            // Fire trail along projected path
            if (currentFireTrail) {
                placeFireAlongPath(player, currentPos, projectedPos);
            }

            // Stop horizontal movement when dash ends
            if (remainingDashTicks <= 0) {
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
                player.hurtMarked = true;
            }
        }

        // Fire immunity after blaze powder dash (1 second)
        if (remainingFireImmunityTicks > 0) {
            player.clearFire();
            remainingFireImmunityTicks--;
        }

        // No-fall protection continues after dash movement ends
        if (remainingNoFall > 0) {
            player.fallDistance = 0;
            remainingNoFall--;
        }

        if (remainingDashTicks <= 0 && remainingNoFall <= 0 && remainingFireImmunityTicks <= 0) {
            active = false;
            hitEntities.clear();
        }
    }

    private void placeFireAlongPath(ServerPlayer player, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(dist));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = Mth.lerp(t, from.x, to.x);
            double py = Mth.lerp(t, from.y, to.y);
            double pz = Mth.lerp(t, from.z, to.z);
            BlockPos pos = BlockPos.containing(px, py, pz);

            if (player.level().getBlockState(pos).isAir()
                    && BaseFireBlock.canBePlacedAt(player.level(), pos, player.getDirection())) {
                player.level().setBlockAndUpdate(pos, BaseFireBlock.getState(player.level(), pos));
            }
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "vi_dash");
    }
}
