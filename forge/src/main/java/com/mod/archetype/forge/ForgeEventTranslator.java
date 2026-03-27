package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import com.mod.archetype.command.ArchetypeCommand;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.gui.AbilityHudOverlay;
import com.mod.archetype.keybind.ArchetypeKeybinds;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Archetype.MOD_ID)
public class ForgeEventTranslator {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().tickPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().onPlayerJoin(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().onPlayerRespawn(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (ClassManager.getInstance().shouldCancelDamage(serverPlayer, event.getSource())) {
                event.setCanceled(true);
                return;
            }
            ClassManager.getInstance().onPlayerHurt(serverPlayer, event.getSource(), event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().onPlayerDeath(serverPlayer, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() != null) {
            ClassManager.getInstance().onPlayerAttack(player, event.getTarget(),
                    player.damageSources().playerAttack(player));
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            if (ClassManager.getInstance().onEntityInteract(serverPlayer, event.getTarget())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (ClassManager.getInstance().shouldCancelItemUse(serverPlayer, event.getItemStack())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().onBlockBreak(serverPlayer, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ClassManager.getInstance().onPlayerLeave(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer newPlayer) {
            original.getCapability(ForgePlayerDataAccess.CLASS_DATA_CAP).ifPresent(oldData -> {
                newPlayer.getCapability(ForgePlayerDataAccess.CLASS_DATA_CAP).ifPresent(newData -> {
                    newData.load(oldData.save());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ForgePlayerDataAccess.CAP_ID, new ForgeCapabilityProvider());
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(ClassRegistry.getInstance());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ArchetypeCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderGui(RenderGuiEvent.Post event) {
        AbilityHudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ArchetypeKeybinds.tickKeybinds(
                    ArchetypeForge.ABILITY_1_KEY,
                    ArchetypeForge.ABILITY_2_KEY,
                    ArchetypeForge.ABILITY_3_KEY,
                    ArchetypeForge.CLASS_INFO_KEY);
        }
    }
}
