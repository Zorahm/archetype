package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import com.mod.archetype.condition.types.*;
import com.mod.archetype.core.PlayerClass.ConditionDefinition;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConditionRegistry {

    private static final ConditionRegistry INSTANCE = new ConditionRegistry();

    private final Map<Identifier, ConditionFactory> factories = new HashMap<>();

    public static ConditionRegistry getInstance() {
        return INSTANCE;
    }

    public void register(Identifier type, ConditionFactory factory) {
        factories.put(type, factory);
    }

    public Condition create(ConditionDefinition definition) {
        String type = definition.type();

        if ("and".equals(type)) {
            List<Condition> children = new ArrayList<>();
            for (ConditionDefinition child : definition.children()) {
                children.add(create(child));
            }
            return new AndCondition(children);
        }

        if ("or".equals(type)) {
            List<Condition> children = new ArrayList<>();
            for (ConditionDefinition child : definition.children()) {
                children.add(create(child));
            }
            return new OrCondition(children);
        }

        if ("not".equals(type)) {
            if (definition.children().isEmpty()) {
                Archetype.LOGGER.error("'not' condition requires exactly one child");
                return new Condition() {
                    @Override public boolean test(net.minecraft.world.entity.player.Player player) { return false; }
                    @Override public Identifier getType() { return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "false"); }
                };
            }
            Condition child = create(definition.children().get(0));
            return new NotCondition(child);
        }

        Identifier typeId = Identifier.parse(type);
        ConditionFactory factory = factories.get(typeId);
        if (factory == null) {
            Archetype.LOGGER.error("Unknown condition type: {}", type);
            return new Condition() {
                @Override public boolean test(net.minecraft.world.entity.player.Player player) { return false; }
                @Override public Identifier getType() { return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "false"); }
            };
        }

        return factory.create(definition.params() != null ? definition.params() : new com.google.gson.JsonObject());
    }

    public void registerBuiltins() {
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "time_of_day"), TimeOfDayCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "health_below_percent"), HealthBelowPercentCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "health_above_percent"), HealthAbovePercentCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "in_water"), InWaterCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "underwater"), UnderwaterCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "under_open_sky"), UnderOpenSkyCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "in_dimension"), InDimensionCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "in_biome_tag"), InBiomeTagCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "is_sneaking"), IsSneakingCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "is_sprinting"), IsSprintingCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "on_fire"), OnFireCondition::new);
        register(Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "has_item"), HasItemCondition::new);
    }

    public boolean hasFactory(Identifier type) {
        return factories.containsKey(type);
    }
}
