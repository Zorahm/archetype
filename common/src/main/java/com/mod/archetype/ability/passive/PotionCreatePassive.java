package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.List;
import java.util.Random;

public class PotionCreatePassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;

    private static final List<Holder<Potion>> POTIONS = List.of(
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
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null || !contents.is(Potions.WATER)) continue;

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
        Holder<Potion> potion = POTIONS.get(random.nextInt(POTIONS.size()));

        if (stack.getCount() > 1) {
            stack.shrink(1);
            ItemStack newPotion = new ItemStack(potionItem, 1);
            newPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
            if (!player.getInventory().add(newPotion)) {
                player.drop(newPotion, false);
            }
        } else {
            stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.4f, 1.0f);
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "potion_create");
    }
}
