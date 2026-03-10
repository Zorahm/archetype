package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
    private final Random random = new Random();

    public RandomProjectileAbility(ActiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int levelTier = level / 10;

        // Calculate chances with level scaling
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
        } else if (roll < snowballChance + arrowChance) {
            // Arrow
            Arrow arrow = new Arrow(player.level(), player);
            arrow.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            arrow.shoot(look.x, look.y, look.z, 2.0f, 0f);
            arrow.setBaseDamage(4.0);
            arrow.pickup = Arrow.Pickup.DISALLOWED;
            player.level().addFreshEntity(arrow);
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
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "random_projectile");
    }
}
