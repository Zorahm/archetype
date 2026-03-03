package com.mod.archetype.network.handler;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.core.ActiveClassInstance;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.AbilityReleasePacket;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AbilityReleaseHandler {

    public static void handle(ServerPlayer player, AbilityReleasePacket packet) {
        Archetype.LOGGER.trace("Received AbilityReleasePacket from {}: slot={}",
                player.getName().getString(), packet.getSlotName());

        String slotName = packet.getSlotName();

        // Validation: slot valid, class assigned
        if (slotName == null || slotName.isEmpty() || slotName.length() > 32) return;

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        ActiveClassInstance instance = ClassManager.getInstance().getInstance(player);
        if (instance == null) return;

        ActiveAbility ability = instance.getAbilityBySlot(slotName);
        if (ability == null) return;

        // Validation: ability must be active (charging)
        if (!ability.isActive()) return;

        // Calculate charge level from how long it's been active
        // The charge level tracking is internal to the ability implementation
        ability.onRelease(player, 0); // chargeLevel managed by ability internally

        // Set cooldown and deduct resource
        ResourceLocation abilityId = new ResourceLocation(ability.getType().getNamespace(), slotName);
        data.setCooldown(abilityId, ability.getCooldownTicks());

        if (!player.isCreative() && ability.getResourceCost() > 0) {
            data.setResourceCurrent(data.getResourceCurrent() - ability.getResourceCost());
        }
    }
}
