package com.mod.archetype.core;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.List;

public final class PlayerClass {

    private final ResourceLocation id;
    private final String nameKey;
    private final String descriptionKey;
    private final ResourceLocation icon;
    private final int color;
    private final ClassCategory category;
    private final List<String> loreKeys;

    private final List<AttributeModifierEntry> attributes;
    private final List<ConditionalAttributeEntry> conditionalAttributes;

    private final List<PassiveAbilityEntry> passiveAbilities;
    private final List<ActiveAbilityEntry> activeAbilities;

    @Nullable
    private final ResourceDefinition resource;
    @Nullable
    private final Float sizeModifier;
    private final List<ResourceLocation> incompatibleWith;
    private final List<LevelMilestone> progression;
    private final List<ExtraAbilitySection> extraAbilitySections;
    private final List<AbilityStatEntry> abilityStats;
    private final List<CommandTrigger> commands;

    public PlayerClass(ResourceLocation id, String nameKey, String descriptionKey,
                       ResourceLocation icon, int color, ClassCategory category,
                       List<String> loreKeys,
                       List<AttributeModifierEntry> attributes,
                       List<ConditionalAttributeEntry> conditionalAttributes,
                       List<PassiveAbilityEntry> passiveAbilities,
                       List<ActiveAbilityEntry> activeAbilities,
                       @Nullable ResourceDefinition resource,
                       @Nullable Float sizeModifier,
                       List<ResourceLocation> incompatibleWith,
                       List<LevelMilestone> progression,
                       List<ExtraAbilitySection> extraAbilitySections,
                       List<AbilityStatEntry> abilityStats,
                       List<CommandTrigger> commands) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.icon = icon;
        this.color = color;
        this.category = category;
        this.loreKeys = List.copyOf(loreKeys);
        this.attributes = List.copyOf(attributes);
        this.conditionalAttributes = List.copyOf(conditionalAttributes);
        this.passiveAbilities = List.copyOf(passiveAbilities);
        this.activeAbilities = List.copyOf(activeAbilities);
        this.resource = resource;
        this.sizeModifier = sizeModifier;
        this.incompatibleWith = List.copyOf(incompatibleWith);
        this.progression = List.copyOf(progression);
        this.extraAbilitySections = List.copyOf(extraAbilitySections);
        this.abilityStats = List.copyOf(abilityStats);
        this.commands = List.copyOf(commands);
    }

    public ResourceLocation getId() { return id; }
    public String getNameKey() { return nameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public ResourceLocation getIcon() { return icon; }
    public int getColor() { return color; }
    public ClassCategory getCategory() { return category; }
    public List<String> getLoreKeys() { return loreKeys; }
    public List<AttributeModifierEntry> getAttributes() { return attributes; }
    public List<ConditionalAttributeEntry> getConditionalAttributes() { return conditionalAttributes; }
    public List<PassiveAbilityEntry> getPassiveAbilities() { return passiveAbilities; }
    public List<ActiveAbilityEntry> getActiveAbilities() { return activeAbilities; }
    @Nullable public ResourceDefinition getResource() { return resource; }
    @Nullable public Float getSizeModifier() { return sizeModifier; }
    public List<ResourceLocation> getIncompatibleWith() { return incompatibleWith; }
    public List<LevelMilestone> getProgression() { return progression; }
    public List<ExtraAbilitySection> getExtraAbilitySections() { return extraAbilitySections; }
    public List<AbilityStatEntry> getAbilityStats() { return abilityStats; }
    public List<CommandTrigger> getCommands() { return commands; }

    // --- Nested records ---

    public record AttributeModifierEntry(
            ResourceLocation attribute,
            AttributeModifier.Operation operation,
            double value
    ) {}

    public record ConditionalAttributeEntry(
            ConditionDefinition condition,
            List<AttributeModifierEntry> modifiers
    ) {}

    public record PassiveAbilityEntry(
            ResourceLocation type,
            JsonObject params,
            @Nullable ConditionDefinition activationCondition,
            boolean positive,
            boolean hidden,
            String nameKey,
            String descriptionKey
    ) {}

    public record ActiveAbilityEntry(
            ResourceLocation type,
            String slot,
            int cooldownTicks,
            int resourceCost,
            int unlockLevel,
            JsonObject params,
            String nameKey,
            String descriptionKey,
            ResourceLocation icon,
            @Nullable String item
    ) {}

    public record ResourceDefinition(
            String typeKey,
            int maxValue,
            int startValue,
            float drainPerSecond,
            float regenPerSecond,
            int color,
            ResourceLocation icon
    ) {}

    public record LevelMilestone(int level, String descriptionKey) {}

    public record ExtraAbilitySection(
            String parentSlot,
            String nameKey,
            int unlockLevel,
            List<ExtraAbilityEntry> entries) {}

    public record ExtraAbilityEntry(
            String nameKey,
            String descriptionKey) {}

    public record AbilityStatEntry(
            String nameKey,
            float baseValue,
            List<LevelBonus> bonuses,
            String format) {

        public float computeValue(int classLevel) {
            float value = baseValue;
            for (LevelBonus bonus : bonuses) {
                if (classLevel >= bonus.level()) {
                    value += bonus.value();
                }
            }
            return value;
        }

        public String formatValue(int classLevel) {
            float value = computeValue(classLevel);
            return switch (format) {
                case "seconds" -> String.format("%.0fs", value);
                case "float" -> String.format("%.1f", value);
                case "boolean" -> value > 0 ? "\u2714" : "\u2718";
                case "percent" -> String.format("%.0f%%", value);
                case "header" -> "";
                default -> String.format("%.0f", value);
            };
        }
    }

    public record LevelBonus(int level, float value) {}

    /**
     * Vanilla command executed at a specific lifecycle point.
     * Supported triggers: on_assign, on_remove, on_tick, on_death, on_respawn, on_level_up
     * Placeholders: {player} → player name, {level} → current class level (on_level_up only)
     * Commands run with player's source at permission level 4 (@s = the player).
     */
    public record CommandTrigger(String trigger, String command, int interval) {
        /** @param trigger  lifecycle trigger id
         *  @param command  command string without leading /
         *  @param interval ticks between executions, only for "on_tick" (default 20) */
        public CommandTrigger {
            if (interval <= 0) interval = 20;
        }
    }

    public record ConditionDefinition(
            String type,
            @Nullable JsonObject params,
            List<ConditionDefinition> children
    ) {
        public ConditionDefinition(String type, @Nullable JsonObject params) {
            this(type, params, Collections.emptyList());
        }
    }
}
