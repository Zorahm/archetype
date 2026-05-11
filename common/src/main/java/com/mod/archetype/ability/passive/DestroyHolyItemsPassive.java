package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DestroyHolyItemsPassive extends AbstractPassiveAbility {
    public DestroyHolyItemsPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        if (totemDamageCooldown > 0) totemDamageCooldown--;
        // Totem: deal periodic damage while held in main or off hand
        checkTotem(player, player.getMainHandItem());
        checkTotem(player, player.getOffhandItem());

        // Elytra: set durability to 1 when worn in chest slot
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.is(Items.ELYTRA)) {
            int maxDurability = chest.getMaxDamage();
            if (chest.getDamageValue() < maxDurability - 1) {
                chest.setDamageValue(maxDurability - 1);
            }
        }
    }

    private int totemDamageCooldown = 0;

    private void checkTotem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (stack.is(Items.TOTEM_OF_UNDYING)) {
            // Deal damage periodically while totem is held (every 20 ticks = 1 sec)
            if (totemDamageCooldown <= 0) {
                player.hurt(player.damageSources().magic(), 5f);
                totemDamageCooldown = 20;
            }
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "destroy_holy_items");
    }
}
