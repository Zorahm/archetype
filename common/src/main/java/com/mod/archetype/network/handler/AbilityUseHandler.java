package com.mod.archetype.network.handler;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.ActiveClassInstance;
import com.mod.archetype.core.ArchetypeEvents;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.AbilityUsePacket;
import com.mod.archetype.platform.PlayerDataAccess;
import dev.architectury.event.EventResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AbilityUseHandler {

    public static void handle(ServerPlayer player, AbilityUsePacket packet) {
        Archetype.LOGGER.trace("Received AbilityUsePacket from {}: slot={}",
                player.getName().getString(), packet.getSlotName());

        String slotName = packet.getSlotName();

        // Validation 1: slot name sanity
        if (slotName == null || slotName.isEmpty() || slotName.length() > 32) {
            Archetype.LOGGER.warn("Player {} sent invalid ability use packet (bad slot name)",
                    player.getName().getString());
            return;
        }

        // Validation 2: player has a class
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        // Validation 3: ability exists in slot
        ActiveClassInstance instance = ClassManager.getInstance().getInstance(player);
        if (instance == null) return;

        ActiveAbility ability = instance.getAbilityBySlot(slotName);
        if (ability == null) {
            Archetype.LOGGER.warn("Player {} tried to use non-existent ability in slot: {}",
                    player.getName().getString(), slotName);
            return;
        }

        // Validation 4: not on cooldown (skip for abilities that manage their own cooldown/charges)
        ResourceLocation abilityId = new ResourceLocation(ability.getType().getNamespace(), slotName);
        if (!ability.managesCooldown() && data.getCooldown(abilityId) > 0) return;

        // Validation 5: enough resource
        if (!player.isCreative() && ability.getResourceCost() > 0) {
            if (data.getResourceCurrent() < ability.getResourceCost()) return;
        }

        // Validation 6: level check
        if (ability.getUnlockLevel() > 0 && data.getClassLevel() < ability.getUnlockLevel()) return;

        // Pass client movement direction to ability
        ability.setClientMoveDirection(packet.getMoveDirX(), packet.getMoveDirZ());

        // Validation 7: ability can activate
        if (!ability.canActivate(player)) return;

        // Validation 8: pre-use event
        EventResult preUseResult = ArchetypeEvents.ABILITY_PRE_USE.invoker().onPreUse(player, ability);
        if (preUseResult == EventResult.interruptFalse()) return;

        // Activate
        ActivationResult result = ability.activate(player);

        if (result == ActivationResult.SUCCESS) {
            // Deduct resource
            if (!player.isCreative() && ability.getResourceCost() > 0) {
                data.setResourceCurrent(data.getResourceCurrent() - ability.getResourceCost());
            }

            // Set cooldown (skip for abilities that manage their own cooldown/charges)
            if (!ability.managesCooldown()) {
                data.setCooldown(abilityId, ability.getCooldownTicks());
            }

            // Publish event
            ArchetypeEvents.ABILITY_USED.invoker().onUsed(player, ability);

            // Immediate sync (don't wait for 20-tick cycle)
            // ClassManager handles sync in tickPlayer, but we force one here
        }
    }
}
