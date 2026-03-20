package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;

public class VillagerRejectionPassive extends AbstractPassiveAbility {

    public VillagerRejectionPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        if (player.containerMenu instanceof MerchantMenu merchantMenu) {
            if (!player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
                // Clear offers so the villager appears to have no trades (like a nitwit)
                merchantMenu.getOffers().clear();
            }
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "villager_rejection");
    }
}
