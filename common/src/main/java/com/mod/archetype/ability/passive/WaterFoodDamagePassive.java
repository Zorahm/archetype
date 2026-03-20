package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class WaterFoodDamagePassive extends AbstractPassiveAbility {

    private final float damage;
    private final List<String> safeFoods;

    public WaterFoodDamagePassive(PassiveAbilityEntry entry) {
        super(entry);
        this.damage = getFloat("damage", 1.0f);
        this.safeFoods = getStringList("safe_foods");
    }

    @Override
    public void tick(ServerPlayer player) {
    }

    @Override
    public void onPlayerEat(ServerPlayer player, ItemStack food) {
        String itemId = BuiltInRegistries.ITEM.getKey(food.getItem()).toString();
        if (!safeFoods.contains(itemId)) {
            player.hurt(player.damageSources().generic(), damage);
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "water_food_damage");
    }
}
