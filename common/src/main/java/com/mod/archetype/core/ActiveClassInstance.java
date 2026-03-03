package com.mod.archetype.core;

import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.PassiveAbility;

import java.util.List;
import java.util.Map;

public class ActiveClassInstance {

    private final PlayerClass classDefinition;
    private final List<PassiveAbility> activePassives;
    private final Map<String, ActiveAbility> activeAbilities; // slot -> ability
    private int tickCounter;

    public ActiveClassInstance(PlayerClass classDefinition,
                                List<PassiveAbility> activePassives,
                                Map<String, ActiveAbility> activeAbilities) {
        this.classDefinition = classDefinition;
        this.activePassives = activePassives;
        this.activeAbilities = activeAbilities;
        this.tickCounter = 0;
    }

    public PlayerClass getClassDefinition() {
        return classDefinition;
    }

    public List<PassiveAbility> getActivePassives() {
        return activePassives;
    }

    public Map<String, ActiveAbility> getActiveAbilities() {
        return activeAbilities;
    }

    public ActiveAbility getAbilityBySlot(String slot) {
        return activeAbilities.get(slot);
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void incrementTickCounter() {
        tickCounter++;
    }
}
