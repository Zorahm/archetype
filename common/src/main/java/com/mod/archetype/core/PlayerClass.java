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

    public PlayerClass(ResourceLocation id, String nameKey, String descriptionKey,
                       ResourceLocation icon, int color, ClassCategory category,
                       List<String> loreKeys,
                       List<AttributeModifierEntry> attributes,
                       List<ConditionalAttributeEntry> conditionalAttributes,
                       List<PassiveAbilityEntry> passiveAbilities,
                       List<ActiveAbilityEntry> activeAbilities,
                       @Nullable ResourceDefinition resource,
                       @Nullable Float sizeModifier,
                       List<ResourceLocation> incompatibleWith) {
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
            ResourceLocation icon
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
