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
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RandomProjectileAbility extends AbstractActiveAbility {
    private static final int BASE_COOLDOWN_TICKS = 80; // 4 seconds

    private final Random random = new Random();

    public RandomProjectileAbility(ActiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public boolean managesCooldown() { return true; }

    private int computeCooldown(int classLevel) {
        // -1s at level 30, -1s at level 60
        int reduction = (classLevel >= 30 ? 1 : 0) + (classLevel >= 60 ? 1 : 0);
        return BASE_COOLDOWN_TICKS - (reduction * 20);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int levelTier = Math.min(5, classLevel / 10);

        int snowballChance = Math.max(0, 70 - levelTier * 10);
        int arrowChance = 25 + levelTier * 5;
        int potionChance = 5 + levelTier * 5;
        int total = snowballChance + arrowChance + potionChance;

        int roll = random.nextInt(total);

        Vec3 look = player.getLookAngle();

        if (roll < snowballChance) {
            // Snowball
            Snowball snowball = new Snowball(player.level(), player);
            snowball.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            snowball.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(snowball);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        } else if (roll < snowballChance + arrowChance) {
            // Arrow
            Arrow arrow = new Arrow(player.level(), player);
            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            arrow.shoot(look.x, look.y, look.z, 2.0f, 0f);
            arrow.setBaseDamage(4.0);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            player.level().addFreshEntity(arrow);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f / (random.nextFloat() * 0.4f + 1.2f));
        } else {
            // Splash potion with random harmful effect
            ThrownPotion potion = new ThrownPotion(player.level(), player);
            var potionTypes = new net.minecraft.world.item.alchemy.Potion[]{
                    Potions.HARMING, Potions.POISON, Potions.SLOWNESS
            };
            ItemStack potionStack = new ItemStack(Items.SPLASH_POTION);
            PotionUtils.setPotion(potionStack, potionTypes[random.nextInt(potionTypes.length)]);
            potion.setItem(potionStack);
            potion.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            potion.shoot(look.x, look.y, look.z, 1.2f, 0f);
            player.level().addFreshEntity(potion);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
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
        return new ResourceLocation("archetype", "random_projectile");
    }
}
