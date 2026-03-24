package com.mod.archetype.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface ActiveAbility {

    boolean canActivate(ServerPlayer player);

    ActivationResult activate(ServerPlayer player);

    void tickActive(ServerPlayer player);

    void forceDeactivate(ServerPlayer player);

    boolean isActive();

    default void onRelease(ServerPlayer player, int chargeLevel) {}

    default void setClientMoveDirection(float dirX, float dirZ) {}

    /**
     * If true, this ability manages its own cooldown/charge system.
     * The handler will skip standard cooldown checks and won't set cooldowns after activation.
     */
    default boolean managesCooldown() { return false; }

    default int getCharges(ServerPlayer player) { return -1; }

    default int getMaxCharges(ServerPlayer player) { return -1; }

    ResourceLocation getType();

    String getSlot();

    int getCooldownTicks();

    default int getCooldownTicks(ServerPlayer player) {
        return getCooldownTicks();
    }

    int getResourceCost();

    int getUnlockLevel();

    String getNameKey();

    String getDescriptionKey();

    ResourceLocation getIcon();
}
