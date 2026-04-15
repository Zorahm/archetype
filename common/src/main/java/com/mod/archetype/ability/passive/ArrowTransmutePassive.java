package com.mod.archetype.ability.passive;

import com.google.gson.JsonArray;
import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import com.mod.archetype.platform.PlayerDataAccess;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ArrowTransmutePassive extends AbstractPassiveAbility {
    private final Random random = new Random();
    private int tickCounter = 0;
    private final List<Integer> speedBoostLevels;

    private static final List<Holder<Potion>> ARROW_POTIONS = List.of(
            Potions.POISON, Potions.HARMING, Potions.SLOWNESS,
            Potions.WEAKNESS, Potions.HEALING, Potions.STRENGTH,
            Potions.SWIFTNESS, Potions.REGENERATION, Potions.FIRE_RESISTANCE,
            Potions.NIGHT_VISION, Potions.INVISIBILITY
    );

    public ArrowTransmutePassive(PassiveAbilityEntry entry) {
        super(entry);
        this.speedBoostLevels = new ArrayList<>();
        if (params.has("speed_boost_levels") && params.get("speed_boost_levels").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("speed_boost_levels");
            for (int i = 0; i < arr.size(); i++) {
                speedBoostLevels.add(arr.get(i).getAsInt());
            }
        }
    }

    private int computeInterval(ServerPlayer player) {
        int classLevel = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int interval = 20;
        for (int threshold : speedBoostLevels) {
            if (classLevel >= threshold) {
                interval /= 2;
            }
        }
        return Math.max(1, interval);
    }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        if (tickCounter % computeInterval(player) != 0) return;

        ItemStack[] handItems = { player.getMainHandItem(), player.getOffhandItem() };
        for (ItemStack stack : handItems) {
            if (stack.isEmpty() || !stack.is(Items.ARROW)) continue;

            stack.shrink(1);

            Holder<Potion> potion = ARROW_POTIONS.get(random.nextInt(ARROW_POTIONS.size()));
            ItemStack tipped = new ItemStack(Items.TIPPED_ARROW, 1);
            tipped.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));

            if (!player.getInventory().add(tipped)) {
                player.drop(tipped, false);
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f);
            return;
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "arrow_transmute");
    }
}
