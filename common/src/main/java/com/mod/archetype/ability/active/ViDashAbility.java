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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViDashAbility extends AbstractActiveAbility {

    private final float dashSpeed;
    private final int noFallTicks;
    private int remainingNoFall;
    private final Set<Integer> hitEntities = new HashSet<>();

    // Offhand modifier state for current dash
    private boolean currentFireTrail;
    private boolean currentTotemBlast;
    private boolean currentWitherTrail;
    private float currentBonusDamage;

    // Charge tracking
    private int charges = -1; // -1 = not initialized
    private int maxChargesBase;
    private long lastRefillTime = 0;
    private static final int CHARGE_REFILL_TICKS = 100; // 5 seconds

    public ViDashAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.dashSpeed = getFloat("dash_speed", 3.5f);
        this.noFallTicks = getInt("no_fall_ticks", 30);
        this.maxChargesBase = getInt("charges", 1);
    }

    private int getMaxCharges(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int extra = level / 10; // +1 per 10 levels
        int baseMax = Math.min(maxChargesBase + extra, getInt("max_charges", 3));
        // Nether star in offhand grants +1 bonus
        if (player.getOffhandItem().is(Items.NETHER_STAR)) {
            baseMax++;
        }
        return baseMax;
    }

    private void refillCharges(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        int max = getMaxCharges(player);
        if (charges == -1) {
            charges = max;
            lastRefillTime = gameTime;
            return;
        }
        if (charges < max && gameTime - lastRefillTime >= CHARGE_REFILL_TICKS) {
            int gained = (int)((gameTime - lastRefillTime) / CHARGE_REFILL_TICKS);
            charges = Math.min(max, charges + gained);
            lastRefillTime = gameTime;
        }
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

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        charges--;
        lastRefillTime = player.level().getGameTime();

        // Determine offhand modifiers
        ItemStack offhand = player.getOffhandItem();
        currentFireTrail = offhand.is(Items.BLAZE_POWDER);
        currentTotemBlast = offhand.is(Items.TOTEM_OF_UNDYING);
        currentWitherTrail = offhand.is(Items.WITHER_ROSE);
        currentBonusDamage = offhand.is(Items.NETHERITE_SWORD) ? 2.0f : 0f;

        // Consume offhand item (except nether star and netherite sword)
        if (currentTotemBlast || currentWitherTrail) {
            offhand.shrink(1);
        }
        if (currentFireTrail) {
            offhand.shrink(1);
        }

        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.scale(dashSpeed));
        player.hurtMarked = true;
        active = true;
        remainingNoFall = noFallTicks;
        hitEntities.clear();

        // Totem blast: immediate 40 damage to all nearby + self
        if (currentTotemBlast) {
            AABB area = player.getBoundingBox().inflate(3.0);
            List<Entity> entities = player.level().getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive());
            for (Entity entity : entities) {
                entity.hurt(player.damageSources().playerAttack(player), 40f);
            }
            // Also damage Vi herself
            player.hurt(player.damageSources().generic(), 40f);
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (remainingNoFall > 0) {
            player.fallDistance = 0;
            remainingNoFall--;

            // Fire trail
            if (currentFireTrail) {
                BlockPos below = player.blockPosition();
                if (player.level().getBlockState(below).isAir()
                        && BaseFireBlock.canBePlacedAt(player.level(), below, player.getDirection())) {
                    player.level().setBlockAndUpdate(below, BaseFireBlock.getState(player.level(), below));
                }
            }

            // Calculate damage
            float baseDamage = getFloat("damage", 3.0f);
            double weaponDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float totalDamage = baseDamage + (float)(weaponDmg / 3.0) + currentBonusDamage;

            // Hit entities
            AABB hitbox = player.getBoundingBox().inflate(0.5);
            List<Entity> entities = player.level().getEntities(player, hitbox, e -> e instanceof LivingEntity && e.isAlive());
            for (Entity entity : entities) {
                if (hitEntities.contains(entity.getId())) continue;
                hitEntities.add(entity.getId());

                entity.hurt(player.damageSources().playerAttack(player), totalDamage);

                // Wither trail effect
                if (currentWitherTrail && entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
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
        return new ResourceLocation("archetype", "vi_dash");
    }
}
