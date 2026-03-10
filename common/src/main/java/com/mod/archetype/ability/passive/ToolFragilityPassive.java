package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class ToolFragilityPassive extends AbstractPassiveAbility {
    private int tickCounter = 0;

    public ToolFragilityPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        tickCounter++;
        // Every second, damage the held tool/weapon by 1 extra if being used
        // Bows get extra 3 damage (total 4x faster with normal usage)
        if (tickCounter % 20 == 0) {
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
                if (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem) {
                    mainHand.hurtAndBreak(3, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
                } else {
                    mainHand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
                }
            }
        }
    }

    @Override
    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        // Extra durability damage on attack (makes tools break 2x faster)
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            int extraDamage = (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem) ? 3 : 1;
            mainHand.hurtAndBreak(extraDamage, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "tool_fragility");
    }
}
