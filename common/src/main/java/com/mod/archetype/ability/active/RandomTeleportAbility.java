package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RandomTeleportAbility extends AbstractActiveAbility {
    private static final int BASE_COOLDOWN_TICKS = 180; // 9 seconds

    private final Random random = new Random();
    private final float range;

    public RandomTeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.range = getFloat("range", 40.0f);
    }

    @Override
    public boolean managesCooldown() { return true; }

    private int computeCooldown(int classLevel) {
        // -2s at level 30, -2s at level 60
        int reduction = (classLevel >= 30 ? 1 : 0) + (classLevel >= 60 ? 1 : 0);
        return BASE_COOLDOWN_TICKS - (reduction * 40);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int levelTier = Math.min(5, classLevel / 10);

        int snowballChance = Math.max(0, 70 - levelTier * 10);
        int pearlChance = 25 + levelTier * 10;
        int eggChance = 5;
        int total = snowballChance + pearlChance + eggChance;

        int roll = random.nextInt(total);
        Vec3 look = player.getLookAngle();

        if (roll < snowballChance) {
            // Fail: snowball
            Snowball snowball = new Snowball(player.level(), player);
            snowball.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            snowball.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(snowball);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        } else if (roll < snowballChance + pearlChance) {
            // Success: throw ender pearl as projectile
            ThrownEnderpearl pearl = new ThrownEnderpearl(player.level(), player);
            pearl.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            pearl.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(pearl);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENDER_PEARL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        } else {
            // Fail: egg
            ThrownEgg egg = new ThrownEgg(player.level(), player);
            egg.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            egg.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(egg);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        }

        // Ability particles and sound
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.WARPED_SPORE,
                    player.getX(), player.getY(), player.getZ(),
                    10, 0, 0, 0, 1.0);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VEX_HURT, SoundSource.AMBIENT, 100.0f, 1.2f);

        // Set cooldown (scaled by class level)
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        ResourceLocation abilityId = new ResourceLocation("archetype", entry.slot());
        data.setCooldown(abilityId, computeCooldown(data.getClassLevel()));

        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "random_teleport");
    }
}
