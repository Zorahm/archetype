package com.mod.archetype.fabric;

import com.mod.archetype.Archetype;
import com.mod.archetype.command.ArchetypeCommand;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.registry.ClassRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FabricEventTranslator {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ClassManager.getInstance().tickPlayer(player);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Load saved class data before ClassManager processes the join
            ArchetypeWorldData worldData = ArchetypeWorldData.get(server);
            net.minecraft.nbt.CompoundTag savedData = worldData.getPlayerData(player.getUUID());
            FabricPlayerDataAccess.onPlayerJoin(player, savedData);
            ClassManager.getInstance().onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            // Save class data before removing from memory
            ArchetypeWorldData worldData = ArchetypeWorldData.get(server);
            net.minecraft.nbt.CompoundTag savedTag = FabricPlayerDataAccess.onPlayerSave(player);
            worldData.setPlayerData(player.getUUID(), savedTag);
            FabricPlayerDataAccess.onPlayerLeave(player);
            ClassManager.getInstance().onPlayerLeave(player);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            FabricPlayerDataAccess.copyData(oldPlayer, newPlayer);
            ClassManager.getInstance().onPlayerRespawn(newPlayer);
        });

        // Death event
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                ClassManager.getInstance().onPlayerDeath(serverPlayer);
            }
        });

        // Hurt event
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer serverPlayer) {
                if (ClassManager.getInstance().shouldCancelDamage(serverPlayer, source)) {
                    return false;
                }
                ClassManager.getInstance().onPlayerHurt(serverPlayer, source, amount);
            }
            return true;
        });

        // Attack event
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof ServerPlayer serverPlayer && entity != null) {
                ClassManager.getInstance().onPlayerAttack(serverPlayer, entity,
                        serverPlayer.damageSources().playerAttack(serverPlayer));
            }
            return InteractionResult.PASS;
        });

        // Item use event (cancel shield etc.)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack item = player.getItemInHand(hand);
                if (ClassManager.getInstance().shouldCancelItemUse(serverPlayer, item)) {
                    return InteractionResultHolder.fail(item);
                }
            }
            return InteractionResultHolder.pass(ItemStack.EMPTY);
        });

        // Block break event
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ClassManager.getInstance().onBlockBreak(serverPlayer, pos, state);
            }
        });

        // Command registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ArchetypeCommand.register(dispatcher);
        });

        // Reload listener for class data (wrap in IdentifiableResourceReloadListener)
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new IdentifiableResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return new ResourceLocation(Archetype.MOD_ID, "class_registry");
                    }

                    @Override
                    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                                           ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                                           Executor backgroundExecutor, Executor gameExecutor) {
                        return ClassRegistry.getInstance().reload(barrier, manager,
                                preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
                    }
                });
    }
}
