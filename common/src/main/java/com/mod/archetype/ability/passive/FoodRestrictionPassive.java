package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoodRestrictionPassive extends AbstractPassiveAbility {

    private final List<String> allowedFoods;

    public FoodRestrictionPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.allowedFoods = getStringList("allowed_foods");
    }

    @Override
    public void tick(ServerPlayer player) {
    }

    @Override
    public boolean shouldCancelItemUse(ServerPlayer player, ItemStack item) {
        // Block eating non-allowed food entirely
        if (item.getItem().isEdible()) {
            String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
            return !allowedFoods.isEmpty() && !allowedFoods.contains(itemId);
        }
        return false;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "food_restriction");
    }
}
