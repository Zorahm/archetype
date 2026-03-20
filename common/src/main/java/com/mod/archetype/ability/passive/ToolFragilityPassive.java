package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ToolFragilityPassive extends AbstractPassiveAbility {

    public ToolFragilityPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        // No passive durability drain — only extra wear on actual use (attack/block break)
    }

    @Override
    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        // Extra durability damage on attack: +1 for tools (2x total), +3 for bows (4x total)
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            int extraDamage = (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem) ? 3 : 1;
            mainHand.hurtAndBreak(extraDamage, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public void onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        // Extra durability damage on block break: +1 for tools (2x total)
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            mainHand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "tool_fragility");
    }
}
