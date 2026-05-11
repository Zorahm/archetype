package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CustomDietPassive extends AbstractPassiveAbility {

    private final List<String> foodItems;
    private final int foodValue;
    private final float saturation;

    public CustomDietPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.foodItems = getStringList("food_items");
        this.foodValue = getInt("food_value", 6);
        this.saturation = getFloat("saturation", 0.6f);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior
    }

    @Override
    public void onPlayerEat(ServerPlayer player, ItemStack food) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(food.getItem());
        String itemIdStr = itemId.toString();

        if (foodItems.contains(itemIdStr)) {
            FoodData foodData = player.getFoodData();
            foodData.eat(foodValue, saturation);
        } else {
            // Non-diet foods cause negative effects
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "custom_diet");
    }
}
