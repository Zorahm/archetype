package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;
import java.util.Random;

public class PotionCreatePassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;

    private static final List<Potion> POTIONS = List.of(
            Potions.HEALING, Potions.HARMING, Potions.REGENERATION,
            Potions.STRENGTH, Potions.SWIFTNESS, Potions.SLOWNESS,
            Potions.POISON, Potions.WEAKNESS, Potions.FIRE_RESISTANCE,
            Potions.NIGHT_VISION, Potions.INVISIBILITY, Potions.WATER_BREATHING,
            Potions.LEAPING, Potions.SLOW_FALLING
    );

    public PotionCreatePassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (PotionUtils.getPotion(stack) != Potions.WATER) continue;

            // Handle regular, splash, and lingering water potions
            if (stack.is(Items.POTION)) {
                convertPotion(player, stack, Items.POTION);
                return;
            } else if (stack.is(Items.SPLASH_POTION)) {
                convertPotion(player, stack, Items.SPLASH_POTION);
                return;
            } else if (stack.is(Items.LINGERING_POTION)) {
                convertPotion(player, stack, Items.LINGERING_POTION);
                return;
            }
        }
    }

    private void convertPotion(ServerPlayer player, ItemStack stack, net.minecraft.world.item.Item potionItem) {
        Potion potion = POTIONS.get(random.nextInt(POTIONS.size()));

        if (stack.getCount() > 1) {
            stack.shrink(1);
            ItemStack newPotion = new ItemStack(potionItem, 1);
            PotionUtils.setPotion(newPotion, potion);
            if (!player.getInventory().add(newPotion)) {
                player.drop(newPotion, false);
            }
        } else {
            PotionUtils.setPotion(stack, potion);
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "potion_create");
    }
}
