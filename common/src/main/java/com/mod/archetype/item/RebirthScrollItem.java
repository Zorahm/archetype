package com.mod.archetype.item;

import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.network.OpenClassSelectionPacket;
import com.mod.archetype.platform.NetworkHandler;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class RebirthScrollItem extends Item {

    public RebirthScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(serverPlayer);
            if (data.hasClass()) {
                // Don't consume here — ClassSelectHandler will shrink mainhand after successful selection
                NetworkHandler.INSTANCE.sendToPlayer(serverPlayer, new OpenClassSelectionPacket((byte) 1));
                return InteractionResultHolder.consume(stack);
            }
            serverPlayer.displayClientMessage(
                    Component.translatable("commands.archetype.error.no_class"), true);
            return InteractionResultHolder.fail(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.archetype.rebirth_scroll.tooltip")
                .withStyle(s -> s.withColor(0xAAAAAA).withItalic(true)));
    }
}
