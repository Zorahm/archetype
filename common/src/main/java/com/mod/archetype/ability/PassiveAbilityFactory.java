package com.mod.archetype.ability;

import com.mod.archetype.core.PlayerClass;

@FunctionalInterface
public interface PassiveAbilityFactory {
    PassiveAbility create(PlayerClass.PassiveAbilityEntry entry);
}
