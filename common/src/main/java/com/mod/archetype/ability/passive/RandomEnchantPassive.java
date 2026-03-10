package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomEnchantPassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;

    public RandomEnchantPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.isEnchantable() && !stack.isEnchanted()) continue;
            if (stack.getOrCreateTag().getBoolean("archetype_enchanted")) continue;

            // Find valid enchantments for this item
            List<Enchantment> valid = new ArrayList<>();
            for (Enchantment ench : BuiltInRegistries.ENCHANTMENT) {
                if (ench.canEnchant(stack)) {
                    valid.add(ench);
                }
            }
            if (valid.isEmpty()) {
                stack.getOrCreateTag().putBoolean("archetype_enchanted", true);
                continue;
            }

            // Apply random enchantment at level 1
            Enchantment chosen = valid.get(random.nextInt(valid.size()));
            stack.enchant(chosen, 1);

            // 15% chance curse of vanishing
            if (random.nextFloat() < 0.15f) {
                stack.enchant(Enchantments.VANISHING_CURSE, 1);
            }

            stack.getOrCreateTag().putBoolean("archetype_enchanted", true);
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "random_enchant");
    }
}
