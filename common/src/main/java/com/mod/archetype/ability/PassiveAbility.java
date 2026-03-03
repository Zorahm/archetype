package com.mod.archetype.ability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public interface PassiveAbility {

    void onApply(ServerPlayer player);

    void tick(ServerPlayer player);

    void onRemove(ServerPlayer player);

    default void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {}

    default void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {}

    default void onPlayerEat(ServerPlayer player, ItemStack food) {}

    ResourceLocation getType();

    boolean isPositive();

    String getNameKey();

    String getDescriptionKey();
}
