package com.mod.archetype.item;

import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.OpenClassSelectionPacket;
import com.mod.archetype.platform.NetworkHandler;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class RebirthScrollItem extends Item {

    public RebirthScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(serverPlayer);
            if (data.hasClass()) {
                // Don't consume here — ClassSelectHandler will shrink mainhand after successful selection
                NetworkHandler.INSTANCE.sendToPlayer(serverPlayer, new OpenClassSelectionPacket((byte) 1));
                return InteractionResult.CONSUME;
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("commands.archetype.error.no_class"), true);
            return InteractionResult.FAIL;
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.archetype.rebirth_scroll.tooltip")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
    }
}
