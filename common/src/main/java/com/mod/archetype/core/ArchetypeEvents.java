package com.mod.archetype.core;

import com.mod.archetype.ability.ActiveAbility;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ArchetypeEvents {

    public static final SimpleEvent<ClassAssigned> CLASS_ASSIGNED = new SimpleEvent<>(
            ls -> (player, cls) -> { for (var l : ls) l.onAssigned(player, cls); }
    );

    public static final SimpleEvent<ClassRemoved> CLASS_REMOVED = new SimpleEvent<>(
            ls -> (player, cls) -> { for (var l : ls) l.onRemoved(player, cls); }
    );

    public static final SimpleEvent<AbilityUsed> ABILITY_USED = new SimpleEvent<>(
            ls -> (player, ability) -> { for (var l : ls) l.onUsed(player, ability); }
    );

    public static final SimpleEvent<AbilityPreUse> ABILITY_PRE_USE = new SimpleEvent<>(
            ls -> (player, ability) -> {
                for (var l : ls) {
                    if (!l.onPreUse(player, ability)) return false;
                }
                return true;
            }
    );

    public static final SimpleEvent<ClassLevelUp> CLASS_LEVEL_UP = new SimpleEvent<>(
            ls -> (player, level) -> { for (var l : ls) l.onLevelUp(player, level); }
    );

    private ArchetypeEvents() {}

    public static class SimpleEvent<T> {
        private final List<T> listeners = new ArrayList<>();
        private final T invoker;

        SimpleEvent(Function<List<T>, T> factory) {
            this.invoker = factory.apply(listeners);
        }

        public void register(T listener) {
            listeners.add(listener);
        }

        public T invoker() {
            return invoker;
        }
    }

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
        /** @return true to allow, false to cancel */
        boolean onPreUse(ServerPlayer player, ActiveAbility ability);
    }

    @FunctionalInterface
    public interface ClassLevelUp {
        void onLevelUp(ServerPlayer player, int newLevel);
    }
}
