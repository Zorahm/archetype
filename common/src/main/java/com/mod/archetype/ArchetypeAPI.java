package com.mod.archetype;

import com.mod.archetype.ability.AbilityRegistry;
import com.mod.archetype.ability.ActiveAbilityFactory;
import com.mod.archetype.ability.PassiveAbilityFactory;
import com.mod.archetype.condition.ConditionFactory;
import com.mod.archetype.condition.ConditionRegistry;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Optional;

public final class ArchetypeAPI {

    private ArchetypeAPI() {}

    @Nullable
    public static PlayerClass getPlayerClass(Player player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return null;
        return ClassManager.getInstance().getClassDefinition(data.getCurrentClassId());
    }

    public static boolean hasClass(Player player, Identifier classId) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        return data.hasClass() && classId.equals(data.getCurrentClassId());
    }

    public static int getClassLevel(Player player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        return data.getClassLevel();
    }

    public static void assignClass(ServerPlayer player, Identifier classId) {
        ClassManager.getInstance().assignClass(player, classId);
    }

    public static void removeClass(ServerPlayer player) {
        ClassManager.getInstance().removeClass(player);
    }

    public static void registerAbilityType(Identifier id, ActiveAbilityFactory factory) {
        AbilityRegistry.getInstance().registerActive(id, factory);
    }

    public static void registerPassiveType(Identifier id, PassiveAbilityFactory factory) {
        AbilityRegistry.getInstance().registerPassive(id, factory);
    }

    public static void registerConditionType(Identifier id, ConditionFactory factory) {
        ConditionRegistry.getInstance().register(id, factory);
    }

    public static Collection<PlayerClass> getAllClasses() {
        return ClassManager.getInstance().getAllClasses();
    }

    public static Optional<PlayerClass> getClass(Identifier id) {
        return Optional.ofNullable(ClassManager.getInstance().getClassDefinition(id));
    }
}
