package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ElytraFragilityPassive extends AbstractPassiveAbility {

    public ElytraFragilityPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (!player.isFallFlying()) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) return;

        if (player.tickCount % 20 == 0) {
            chest.hurtAndBreak(2, player, EquipmentSlot.CHEST);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "elytra_fragility");
    }
}