package com.mod.archetype.ability;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface PassiveAbilityFactory {
    PassiveAbility create(JsonObject params);
}
