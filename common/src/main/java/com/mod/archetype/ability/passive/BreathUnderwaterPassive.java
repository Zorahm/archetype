package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class BreathUnderwaterPassive extends AbstractPassiveAbility {

    public BreathUnderwaterPassive(PassiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        player.setAirSupply(player.getMaxAirSupply());
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "breath_underwater");
    }
}
