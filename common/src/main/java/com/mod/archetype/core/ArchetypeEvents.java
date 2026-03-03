package com.mod.archetype.core;

import com.mod.archetype.ability.ActiveAbility;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerPlayer;

public final class ArchetypeEvents {

    public static final Event<ClassAssigned> CLASS_ASSIGNED =
            EventFactory.createLoop(ClassAssigned.class);

    public static final Event<ClassRemoved> CLASS_REMOVED =
            EventFactory.createLoop(ClassRemoved.class);

    public static final Event<AbilityUsed> ABILITY_USED =
            EventFactory.createLoop(AbilityUsed.class);

    public static final Event<AbilityPreUse> ABILITY_PRE_USE =
            EventFactory.createLoop(AbilityPreUse.class);

    public static final Event<ClassLevelUp> CLASS_LEVEL_UP =
            EventFactory.createLoop(ClassLevelUp.class);

    private ArchetypeEvents() {}

    @FunctionalInterface
    public interface ClassAssigned {
        void onAssigned(ServerPlayer player, PlayerClass newClass);
    }

    @FunctionalInterface
    public interface ClassRemoved {
        void onRemoved(ServerPlayer player, PlayerClass oldClass);
    }

    @FunctionalInterface
    public interface AbilityUsed {
        void onUsed(ServerPlayer player, ActiveAbility ability);
    }

    @FunctionalInterface
    public interface AbilityPreUse {
        EventResult onPreUse(ServerPlayer player, ActiveAbility ability);
    }

    @FunctionalInterface
    public interface ClassLevelUp {
        void onLevelUp(ServerPlayer player, int newLevel);
    }
}
