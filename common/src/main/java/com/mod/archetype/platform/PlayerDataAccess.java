package com.mod.archetype.platform;

import com.mod.archetype.data.PlayerClassData;
import net.minecraft.world.entity.player.Player;

import java.util.ServiceLoader;

public interface PlayerDataAccess {

    PlayerDataAccess INSTANCE = ServiceLoader.load(PlayerDataAccess.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No PlayerDataAccess implementation found"));

    PlayerClassData getClassData(Player player);

    void setClassData(Player player, PlayerClassData data);
}
