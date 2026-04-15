package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;

public class PotionBlockPassive extends AbstractPassiveAbility {

    private final float damage;

    public PotionBlockPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.damage = getFloat("damage", 10.0f);
    }

    @Override
    public void tick(ServerPlayer player) {
    }

    @Override
    public void onPlayerEat(ServerPlayer player, ItemStack food) {
        if (food.getItem() instanceof PotionItem) {
            player.hurt(player.damageSources().magic(), damage);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "potion_block");
    }
}
