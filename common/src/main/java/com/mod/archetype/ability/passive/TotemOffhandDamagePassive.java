package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class TotemOffhandDamagePassive extends AbstractPassiveAbility {

    private final float damagePerSecond;

    public TotemOffhandDamagePassive(PassiveAbilityEntry entry) {
        super(entry);
        this.damagePerSecond = getFloat("damage_per_second", 8.0f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        if (player.tickCount % 20 != 0) return;
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            player.hurt(player.damageSources().magic(), damagePerSecond);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "totem_offhand_damage");
    }
}
