package com.mod.archetype.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface PassiveAbility {

    void onApply(ServerPlayer player);

    void tick(ServerPlayer player);

    void onRemove(ServerPlayer player);

    default void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {}

    default boolean shouldCancelDamage(ServerPlayer player, DamageSource source) { return false; }

    default void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {}

    default void onPlayerEat(ServerPlayer player, ItemStack food) {}

    default void onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {}

    default boolean shouldCancelItemUse(ServerPlayer player, ItemStack item) { return false; }

    default boolean onEntityInteract(ServerPlayer player, Entity entity) { return false; }

    ResourceLocation getType();

    boolean isPositive();

    String getNameKey();

    String getDescriptionKey();
}
