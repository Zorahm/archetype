package com.mod.archetype.ability;

import com.mod.archetype.core.PlayerClass;

@FunctionalInterface
public interface ActiveAbilityFactory {
    ActiveAbility create(PlayerClass.ActiveAbilityEntry entry);
}
