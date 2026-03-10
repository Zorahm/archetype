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

public class ArrowTransmutePassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;

    // Useful potions for tipped arrows
    private static final List<Potion> ARROW_POTIONS = List.of(
            Potions.POISON, Potions.HARMING, Potions.SLOWNESS,
            Potions.WEAKNESS, Potions.HEALING, Potions.STRENGTH,
            Potions.SWIFTNESS, Potions.REGENERATION, Potions.FIRE_RESISTANCE,
            Potions.NIGHT_VISION, Potions.INVISIBILITY
    );

    public ArrowTransmutePassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !stack.is(Items.ARROW)) continue;

            // Remove 1 arrow
            stack.shrink(1);

            // Create tipped arrow with random potion
            ItemStack tipped = new ItemStack(Items.TIPPED_ARROW, 1);
            Potion potion = ARROW_POTIONS.get(random.nextInt(ARROW_POTIONS.size()));
            PotionUtils.setPotion(tipped, potion);

            // Try to add to inventory, drop if full
            if (!player.getInventory().add(tipped)) {
                player.drop(tipped, false);
            }
            return; // Only convert one per tick cycle
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "arrow_transmute");
    }
}
