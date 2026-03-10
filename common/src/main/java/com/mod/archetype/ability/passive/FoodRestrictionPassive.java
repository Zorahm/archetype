package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FoodRestrictionPassive extends AbstractPassiveAbility {

    private final List<String> allowedFoods;
    private final int poisonDuration;
    private final int hungerDuration;

    public FoodRestrictionPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.allowedFoods = getStringList("allowed_foods");
        this.poisonDuration = getInt("poison_duration", 100);
        this.hungerDuration = getInt("hunger_duration", 200);
    }

    @Override
    public void tick(ServerPlayer player) {
        // No tick behavior
    }

    @Override
    public void onPlayerEat(ServerPlayer player, ItemStack food) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(food.getItem());
        String itemIdStr = itemId.toString();

        if (!allowedFoods.isEmpty() && !allowedFoods.contains(itemIdStr)) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, hungerDuration, 0));
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "food_restriction");
    }
}
