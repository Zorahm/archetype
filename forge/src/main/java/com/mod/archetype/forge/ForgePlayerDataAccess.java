package com.mod.archetype.forge;

import com.mod.archetype.Archetype;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ForgePlayerDataAccess implements PlayerDataAccess {

    public static final Capability<PlayerClassData> CLASS_DATA_CAP =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation CAP_ID =
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
}
