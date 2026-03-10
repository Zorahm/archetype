package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DestroyHolyItemsPassive extends AbstractPassiveAbility {
    public DestroyHolyItemsPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        // Check main hand
        checkAndDestroy(player, player.getMainHandItem());
        // Check off hand
        checkAndDestroy(player, player.getOffhandItem());
        // Check armor slots
        for (ItemStack armor : player.getArmorSlots()) {
            checkAndDestroy(player, armor);
        }
    }

    private void checkAndDestroy(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.ELYTRA)) {
            stack.shrink(stack.getCount());
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "destroy_holy_items");
    }
}
