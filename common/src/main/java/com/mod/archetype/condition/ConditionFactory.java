package com.mod.archetype.condition;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface ConditionFactory {
    Condition create(JsonObject params);
}
