package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DashAbility extends AbstractActiveAbility {

    private final float dashSpeed;
    private final int noFallTicks;
    private final float damage;
    private final boolean fireTrail;
    private final float knockbackStrength;
    private int remainingNoFall;
    private final Set<Integer> hitEntities = new HashSet<>();

    public DashAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.dashSpeed = getFloat("dash_speed", 3.0f);
        this.noFallTicks = getInt("no_fall_ticks", 40);
        this.damage = getFloat("damage", 0f);
        this.fireTrail = getBool("fire_trail", false);
        this.knockbackStrength = getFloat("knockback_strength", 0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(dashSpeed));
        player.hurtMarked = true;
        active = true;
        remainingNoFall = noFallTicks;
        hitEntities.clear();
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (remainingNoFall > 0) {
            player.fallDistance = 0;
            remainingNoFall--;

            // Fire trail: place fire at player's feet
            if (fireTrail) {
                BlockPos below = player.blockPosition();
                if (player.level().getBlockState(below).isAir()
                        && BaseFireBlock.canBePlacedAt(player.level(), below, player.getDirection())) {
                    player.level().setBlockAndUpdate(below, BaseFireBlock.getState(player.level(), below));
                }
            }

            // Damage entities in path (hit each entity only once per dash)
            if (damage > 0 || knockbackStrength > 0) {
                AABB hitbox = player.getBoundingBox().inflate(0.5);
                List<Entity> entities = player.level().getEntities(player, hitbox, e -> e instanceof LivingEntity && e.isAlive());
                for (Entity entity : entities) {
                    if (hitEntities.contains(entity.getId())) continue;
                    hitEntities.add(entity.getId());

                    if (damage > 0) {
                        entity.hurt(player.damageSources().playerAttack(player), damage);
                    }
                    if (knockbackStrength > 0 && entity instanceof LivingEntity living) {
                        Vec3 knockDir = player.getLookAngle().normalize();
                        living.knockback(knockbackStrength, -knockDir.x, -knockDir.z);
                    }
                }
            }
        }
        if (remainingNoFall <= 0) {
            active = false;
            hitEntities.clear();
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "dash");
    }
}
