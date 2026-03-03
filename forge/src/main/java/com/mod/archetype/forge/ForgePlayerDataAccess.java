package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = Archetype.MOD_ID)
public class ForgePlayerDataAccess implements PlayerDataAccess {

    public static final Capability<PlayerClassData> CLASS_DATA_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    private static final ResourceLocation CAP_ID =
            new ResourceLocation(Archetype.MOD_ID, "class_data");

    @Override
    public PlayerClassData getClassData(Player player) {
        return player.getCapability(CLASS_DATA_CAP)
                .orElseGet(PlayerClassData::new);
    }

    @Override
    public void setClassData(Player player, PlayerClassData data) {
        player.getCapability(CLASS_DATA_CAP).ifPresent(existing -> {
            CompoundTag tag = data.save();
            existing.load(tag);
        });
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerClassData data = new PlayerClassData();
            LazyOptional<PlayerClassData> optional = LazyOptional.of(() -> data);

            event.addCapability(CAP_ID, new ICapabilitySerializable<CompoundTag>() {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                    if (cap == CLASS_DATA_CAP) {
                        return optional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT() {
                    return data.save();
                }

                @Override
                public void deserializeNBT(CompoundTag tag) {
                    data.load(tag);
                }
            });

            event.addListener(optional::invalidate);
        }
    }
}
