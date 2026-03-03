package com.mod.archetype.condition;

import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass.ConditionDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConditionRegistry {

    private static final ConditionRegistry INSTANCE = new ConditionRegistry();

    private final Map<ResourceLocation, ConditionFactory> factories = new HashMap<>();

    public static ConditionRegistry getInstance() {
        return INSTANCE;
    }

    public void register(ResourceLocation type, ConditionFactory factory) {
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
                return player -> false;
            }
            Condition child = create(definition.children().get(0));
            return new NotCondition(child);
        }

        ResourceLocation typeId = new ResourceLocation(type);
        ConditionFactory factory = factories.get(typeId);
        if (factory == null) {
            Archetype.LOGGER.error("Unknown condition type: {}", type);
            return player -> false;
        }

        return factory.create(definition.params() != null ? definition.params() : new com.google.gson.JsonObject());
    }

    public void registerBuiltins() {
        // Built-in condition types will be registered here by future prompts
        // e.g. register(new ResourceLocation(Archetype.MOD_ID, "time_of_day"), TimeOfDayCondition::new);
    }

    public boolean hasFactory(ResourceLocation type) {
        return factories.containsKey(type);
    }
}
