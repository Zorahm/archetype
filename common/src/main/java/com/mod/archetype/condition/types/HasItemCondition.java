package com.mod.archetype.condition.types;

import com.google.gson.JsonObject;
import com.mod.archetype.condition.Condition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HasItemCondition implements Condition {
    private final Identifier itemId;
    private final String slot;

    public HasItemCondition(JsonObject params) {
        String item = params.has("item") ? params.get("item").getAsString() : "minecraft:air";
        this.itemId = Identifier.parse(item);
        this.slot = params.has("slot") ? params.get("slot").getAsString() : "any";
    }

    @Override
    public boolean test(Player player) {
        var targetItem = BuiltInRegistries.ITEM.getValue(itemId);
        return switch (slot) {
            case "mainhand" -> player.getMainHandItem().is(targetItem);
            case "offhand" -> player.getOffhandItem().is(targetItem);
            case "armor_head" -> player.getItemBySlot(EquipmentSlot.HEAD).is(targetItem);
            case "armor_chest" -> player.getItemBySlot(EquipmentSlot.CHEST).is(targetItem);
            case "armor_legs" -> player.getItemBySlot(EquipmentSlot.LEGS).is(targetItem);
            case "armor_feet" -> player.getItemBySlot(EquipmentSlot.FEET).is(targetItem);
            default -> hasItemAnywhere(player, targetItem); // "any"
        };
    }

    private boolean hasItemAnywhere(Player player, net.minecraft.world.item.Item targetItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(targetItem)) return true;
        }
        return false;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "has_item");
    }
}
