package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

public class ShieldBlockPassive extends AbstractPassiveAbility {
    public ShieldBlockPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {}

    @Override
    public boolean shouldCancelItemUse(ServerPlayer player, ItemStack item) {
        return item.getItem() instanceof ShieldItem;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "shield_block");
    }
}
