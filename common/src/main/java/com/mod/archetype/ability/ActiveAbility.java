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

    ResourceLocation getType();

    String getSlot();

    int getCooldownTicks();

    int getResourceCost();

    int getUnlockLevel();

    String getNameKey();

    String getDescriptionKey();

    ResourceLocation getIcon();
}
