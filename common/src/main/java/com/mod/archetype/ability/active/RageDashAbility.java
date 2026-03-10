package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RageDashAbility extends AbstractActiveAbility {

    private final float dashSpeed;
    private int dashTicksRemaining;
    private final Set<Integer> hitEntities = new HashSet<>();

    // Current dash mode
    private enum DashMode { NORMAL, FEATHER_JUMP, ICE, OBSIDIAN_DOME, NETHER_STAR }
    private DashMode currentMode = DashMode.NORMAL;
    private boolean hasJumped = false; // for feather mode

    // Tracks regen block expiry time per player (game time when block expires)
    private static final Map<UUID, Long> REGEN_BLOCK_MAP = new ConcurrentHashMap<>();

    public RageDashAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.dashSpeed = getFloat("dash_speed", 4.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        // Need some rage to activate
        float currentRage = data.getResourceCurrent();
        if (currentRage < 10) return ActivationResult.NOT_ENOUGH_RESOURCE;

        // Determine mode from offhand
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(Items.FEATHER)) {
            currentMode = DashMode.FEATHER_JUMP;
            offhand.shrink(1);
        } else if (offhand.is(Items.BLUE_ICE)) {
            currentMode = DashMode.ICE;
            offhand.shrink(1);
        } else if (offhand.is(Items.OBSIDIAN)) {
            currentMode = DashMode.OBSIDIAN_DOME;
            offhand.shrink(1);
        } else if (offhand.is(Items.NETHER_STAR)) {
            currentMode = DashMode.NETHER_STAR;
            offhand.shrink(1);
        } else {
            currentMode = DashMode.NORMAL;
        }

        // Calculate duration: rage / (max/10) seconds
        float maxRage = 100f; // from resource definition
        float drainPerSecond = maxRage / 10f; // 10 per second
        float durationSeconds = currentRage / drainPerSecond;
        dashTicksRemaining = (int)(durationSeconds * 20);

        // Consume all rage
        data.setResourceCurrent(0);

        hitEntities.clear();
        hasJumped = false;
        active = true;

        if (currentMode == DashMode.OBSIDIAN_DOME) {
            // Create dome immediately
            createObsidianDome(player);
            active = false;
            // Block rage regen for 1 minute
            REGEN_BLOCK_MAP.put(player.getUUID(), player.level().getGameTime() + 1200);
            return ActivationResult.SUCCESS;
        }

        if (currentMode == DashMode.FEATHER_JUMP) {
            // Jump up
            player.setDeltaMovement(player.getDeltaMovement().add(0, 1.8, 0));
            player.hurtMarked = true;
            hasJumped = true;
        } else {
            // Forward dash
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.scale(dashSpeed));
            player.hurtMarked = true;
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (dashTicksRemaining <= 0) {
            active = false;
            hitEntities.clear();
            return;
        }
        dashTicksRemaining--;
        player.fallDistance = 0;

        if (currentMode == DashMode.FEATHER_JUMP) {
            // Wait for landing
            if (hasJumped && player.onGround()) {
                // Shockwave on landing
                AABB area = player.getBoundingBox().inflate(5.0);
                List<Entity> entities = player.level().getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive());
                for (Entity entity : entities) {
                    if (entity.distanceTo(player) <= 5.0) {
                        entity.hurt(player.damageSources().playerAttack(player), 7f);
                        if (entity instanceof LivingEntity living) {
                            Vec3 knockDir = entity.position().subtract(player.position()).normalize();
                            living.knockback(1.5, -knockDir.x, -knockDir.z);
                        }
                    }
                }
                active = false;
                hitEntities.clear();
                return;
            }
            return;
        }

        // Fire trail (for NORMAL, NETHER_STAR modes - not ICE)
        if (currentMode != DashMode.ICE) {
            BlockPos below = player.blockPosition();
            if (player.level().getBlockState(below).isAir()
                    && BaseFireBlock.canBePlacedAt(player.level(), below, player.getDirection())) {
                player.level().setBlockAndUpdate(below, BaseFireBlock.getState(player.level(), below));
            }
        }

        // Hit entities
        float damage;
        switch (currentMode) {
            case NETHER_STAR -> damage = 40f;
            case ICE -> damage = 3f;
            default -> damage = 5f;
        }

        AABB hitbox = player.getBoundingBox().inflate(0.6);
        List<Entity> entities = player.level().getEntities(player, hitbox, e -> e instanceof LivingEntity && e.isAlive());
        for (Entity entity : entities) {
            if (hitEntities.contains(entity.getId())) continue;
            hitEntities.add(entity.getId());

            entity.hurt(player.damageSources().playerAttack(player), damage);

            if (currentMode == DashMode.ICE && entity instanceof LivingEntity living) {
                // Freeze: slowness 127 for 1 second (no knockback)
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 126));
            } else if (entity instanceof LivingEntity living) {
                // Knockback (not for ICE mode)
                float kb = getFloat("knockback_strength", 1.5f);
                Vec3 knockDir = player.getLookAngle().normalize();
                living.knockback(kb, -knockDir.x, -knockDir.z);
            }
        }
    }

    private void createObsidianDome(ServerPlayer player) {
        BlockPos center = player.blockPosition();
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + y * y + z * z);
                    if (dist >= radius - 0.5 && dist <= radius + 0.5) {
                        BlockPos pos = center.offset(x, y, z);
                        if (player.level().getBlockState(pos).isAir()) {
                            player.level().setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        hitEntities.clear();
        dashTicksRemaining = 0;
    }

    /**
     * Checks if the player's rage regen is currently blocked (by obsidian dome).
     */
    public static boolean isRegenBlocked(ServerPlayer player) {
        Long blockedUntil = REGEN_BLOCK_MAP.get(player.getUUID());
        if (blockedUntil == null) return false;
        if (player.level().getGameTime() >= blockedUntil) {
            REGEN_BLOCK_MAP.remove(player.getUUID());
            return false;
        }
        return true;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "rage_dash");
    }
}
