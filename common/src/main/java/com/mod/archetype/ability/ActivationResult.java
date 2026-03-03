package com.mod.archetype.ability;

public enum ActivationResult {
    SUCCESS,
    ON_COOLDOWN,
    NOT_ENOUGH_RESOURCE,
    LEVEL_TOO_LOW,
    CONDITION_NOT_MET,
    ALREADY_ACTIVE,
    FAILED
}
