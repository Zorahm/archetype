package com.mod.archetype.config;

import java.util.ArrayList;
import java.util.List;

public class ServerConfig {

    public boolean showSelectionOnFirstJoin = true;
    public boolean allowFreeClassChange = false;
    public int classChangeCooldownTicks = 72000;
    public boolean resetLevelOnClassChange = false;
    public int maxClassLevel = 20;

    public float globalCooldownMultiplier = 1.0f;
    public float globalAbilityDamageMultiplier = 1.0f;
    public float classExpFromMobKill = 1.0f;
    public float classExpFromOreBreak = 1.0f;

    public List<String> disabledClasses = new ArrayList<>();
    public List<String> disabledDimensions = new ArrayList<>();
    public List<String> disabledAbilityTypes = new ArrayList<>();

    public int opLevelForCommands = 2;
    public boolean allowPlayerSelfSelect = true;
}
