package com.mod.archetype.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mod.archetype.core.ClassCategory;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.core.PlayerClass.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ClassJsonParser {

    public static PlayerClass parse(ResourceLocation fileId, JsonObject json) throws ClassParseException {
        // Required fields
        String name = requireString(json, "name", fileId);
        String description = requireString(json, "description", fileId);
        String iconStr = requireString(json, "icon", fileId);
        String colorStr = requireString(json, "color", fileId);

        ResourceLocation icon = parseResourceLocation(iconStr, fileId, "icon");
        int color = parseColor(colorStr, fileId);

        // Optional fields
        ClassCategory category = ClassCategory.DAMAGE;
        if (json.has("category") && json.get("category").isJsonPrimitive()) {
            String catStr = json.get("category").getAsString().toUpperCase(Locale.ROOT);
            try {
                category = ClassCategory.valueOf(catStr);
            } catch (IllegalArgumentException e) {
                throw new ClassParseException(fileId, "category", "Invalid category: " + catStr);
            }
        }

        List<String> loreKeys = new ArrayList<>();
        if (json.has("lore") && json.get("lore").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("lore");
            for (int i = 0; i < arr.size(); i++) {
                loreKeys.add(arr.get(i).getAsString());
            }
        }

        // Attributes
        List<AttributeModifierEntry> attributes = new ArrayList<>();
        if (json.has("attributes") && json.get("attributes").isJsonArray()) {
            attributes = parseAttributes(json.getAsJsonArray("attributes"), fileId);
        }

        // Conditional attributes
        List<ConditionalAttributeEntry> conditionalAttributes = new ArrayList<>();
        if (json.has("conditional_attributes") && json.get("conditional_attributes").isJsonArray()) {
            conditionalAttributes = parseConditionalAttributes(json.getAsJsonArray("conditional_attributes"), fileId);
        }

        // Passive abilities
        List<PassiveAbilityEntry> passiveAbilities = new ArrayList<>();
        if (json.has("passive_abilities") && json.get("passive_abilities").isJsonArray()) {
            passiveAbilities = parsePassives(json.getAsJsonArray("passive_abilities"), fileId);
        }

        // Active abilities
        List<ActiveAbilityEntry> activeAbilities = new ArrayList<>();
        if (json.has("active_abilities") && json.get("active_abilities").isJsonArray()) {
            activeAbilities = parseActives(json.getAsJsonArray("active_abilities"), fileId);
        }

        // Resource
        ResourceDefinition resource = null;
        if (json.has("resource") && json.get("resource").isJsonObject()) {
            resource = parseResource(json.getAsJsonObject("resource"), fileId);
        }

        // Size modifier
        Float sizeModifier = null;
        if (json.has("size_modifier") && json.get("size_modifier").isJsonPrimitive()) {
            sizeModifier = json.get("size_modifier").getAsFloat();
        }

        // Incompatible with
        List<ResourceLocation> incompatibleWith = new ArrayList<>();
        if (json.has("incompatible_with") && json.get("incompatible_with").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("incompatible_with");
            for (int i = 0; i < arr.size(); i++) {
                incompatibleWith.add(parseResourceLocation(arr.get(i).getAsString(), fileId, "incompatible_with[" + i + "]"));
            }
        }

        // Level progression milestones
        List<PlayerClass.LevelMilestone> progression = new ArrayList<>();
        if (json.has("progression") && json.get("progression").isJsonArray()) {
            JsonArray progArr = json.getAsJsonArray("progression");
            for (int i = 0; i < progArr.size(); i++) {
                JsonObject obj = progArr.get(i).getAsJsonObject();
                int level = obj.get("level").getAsInt();
                String key = obj.get("key").getAsString();
                progression.add(new PlayerClass.LevelMilestone(level, key));
            }
        }

        // Extra ability sections
        List<PlayerClass.ExtraAbilitySection> extraAbilitySections = new ArrayList<>();
        if (json.has("extra_ability_sections") && json.get("extra_ability_sections").isJsonArray()) {
            extraAbilitySections = parseExtraAbilitySections(json.getAsJsonArray("extra_ability_sections"), fileId);
        }

        // Ability stats
        List<PlayerClass.AbilityStatEntry> abilityStats = new ArrayList<>();
        if (json.has("ability_stats") && json.get("ability_stats").isJsonArray()) {
            abilityStats = parseAbilityStats(json.getAsJsonArray("ability_stats"), fileId);
        }

        // Command triggers
        List<PlayerClass.CommandTrigger> commands = new ArrayList<>();
        if (json.has("commands") && json.get("commands").isJsonArray()) {
            commands = parseCommands(json.getAsJsonArray("commands"), fileId);
        }

        return new PlayerClass(fileId, name, description, icon, color, category, loreKeys,
                attributes, conditionalAttributes, passiveAbilities, activeAbilities,
                resource, sizeModifier, incompatibleWith, progression,
                extraAbilitySections, abilityStats, commands);
    }

    private static List<AttributeModifierEntry> parseAttributes(JsonArray arr, ResourceLocation fileId) throws ClassParseException {
        List<AttributeModifierEntry> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String attrStr = requireString(obj, "attribute", fileId, "attributes[" + i + "]");
            ResourceLocation attribute = parseResourceLocation(attrStr, fileId, "attributes[" + i + "].attribute");
            AttributeModifier.Operation operation = parseOperation(
                    requireString(obj, "operation", fileId, "attributes[" + i + "]"), fileId, "attributes[" + i + "].operation");
            double value = obj.get("value").getAsDouble();
            result.add(new AttributeModifierEntry(attribute, operation, value));
        }
        return result;
    }

    private static List<ConditionalAttributeEntry> parseConditionalAttributes(JsonArray arr, ResourceLocation fileId) throws ClassParseException {
        List<ConditionalAttributeEntry> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            if (!obj.has("condition") || !obj.has("modifiers")) {
                throw new ClassParseException(fileId, "conditional_attributes[" + i + "]", "Missing 'condition' or 'modifiers'");
            }
            ConditionDefinition condition = parseCondition(obj.getAsJsonObject("condition"), fileId, "conditional_attributes[" + i + "].condition");
            List<AttributeModifierEntry> modifiers = parseAttributes(obj.getAsJsonArray("modifiers"), fileId);
            result.add(new ConditionalAttributeEntry(condition, modifiers));
        }
        return result;
    }

    private static List<PassiveAbilityEntry> parsePassives(JsonArray arr, ResourceLocation fileId) throws ClassParseException {
        List<PassiveAbilityEntry> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String typeStr = requireString(obj, "type", fileId, "passive_abilities[" + i + "]");
            ResourceLocation type = parseResourceLocation(typeStr, fileId, "passive_abilities[" + i + "].type");
            boolean positive = obj.has("positive") && obj.get("positive").getAsBoolean();
            String nameKey = obj.has("name") ? obj.get("name").getAsString() : "";
            String descKey = obj.has("description") ? obj.get("description").getAsString() : "";
            JsonObject params = obj.has("params") && obj.get("params").isJsonObject() ? obj.getAsJsonObject("params") : new JsonObject();

            ConditionDefinition activationCondition = null;
            if (obj.has("condition") && obj.get("condition").isJsonObject()) {
                activationCondition = parseCondition(obj.getAsJsonObject("condition"), fileId, "passive_abilities[" + i + "].condition");
            }

            boolean hidden = obj.has("hidden") && obj.get("hidden").getAsBoolean();
            result.add(new PassiveAbilityEntry(type, params, activationCondition, positive, hidden, nameKey, descKey));
        }
        return result;
    }

    private static List<ActiveAbilityEntry> parseActives(JsonArray arr, ResourceLocation fileId) throws ClassParseException {
        List<ActiveAbilityEntry> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String typeStr = requireString(obj, "type", fileId, "active_abilities[" + i + "]");
            ResourceLocation type = parseResourceLocation(typeStr, fileId, "active_abilities[" + i + "].type");

            String slot = requireString(obj, "slot", fileId, "active_abilities[" + i + "]");
            if (!slot.equals("ability_1") && !slot.equals("ability_2") && !slot.equals("ability_3")) {
                throw new ClassParseException(fileId, "active_abilities[" + i + "].slot", "Invalid slot: " + slot + ". Must be ability_1, ability_2, or ability_3");
            }

            int cooldown = obj.has("cooldown") ? obj.get("cooldown").getAsInt() : 0;
            if (cooldown < 0) {
                throw new ClassParseException(fileId, "active_abilities[" + i + "].cooldown", "Cooldown must be >= 0");
            }

            int resourceCost = obj.has("resource_cost") ? obj.get("resource_cost").getAsInt() : 0;
            int unlockLevel = obj.has("unlock_level") ? obj.get("unlock_level").getAsInt() : 0;
            if (unlockLevel < 0) {
                throw new ClassParseException(fileId, "active_abilities[" + i + "].unlock_level", "Unlock level must be >= 0");
            }

            String nameKey = obj.has("name") ? obj.get("name").getAsString() : "";
            String descKey = obj.has("description") ? obj.get("description").getAsString() : "";
            String iconStr = obj.has("icon") ? obj.get("icon").getAsString() : "archetype:textures/gui/abilities/default.png";
            ResourceLocation icon = parseResourceLocation(iconStr, fileId, "active_abilities[" + i + "].icon");

            String item = obj.has("item") ? obj.get("item").getAsString() : null;

            JsonObject params = obj.has("params") && obj.get("params").isJsonObject() ? obj.getAsJsonObject("params") : new JsonObject();

            result.add(new ActiveAbilityEntry(type, slot, cooldown, resourceCost, unlockLevel, params, nameKey, descKey, icon, item));
        }
        return result;
    }

    private static ResourceDefinition parseResource(JsonObject obj, ResourceLocation fileId) throws ClassParseException {
        String typeKey = obj.has("type") ? obj.get("type").getAsString() : "resource.archetype.generic";
        int max = obj.has("max") ? obj.get("max").getAsInt() : 100;
        if (max <= 0) {
            throw new ClassParseException(fileId, "resource.max", "Resource max must be > 0");
        }
        int start = obj.has("start") ? obj.get("start").getAsInt() : max;
        float drain = obj.has("drain_per_second") ? obj.get("drain_per_second").getAsFloat() : 0f;
        float regen = obj.has("regen_per_second") ? obj.get("regen_per_second").getAsFloat() : 0f;

        int color = 0xFFFFFF;
        if (obj.has("color")) {
            color = parseColor(obj.get("color").getAsString(), fileId);
        }

        String iconStr = obj.has("icon") ? obj.get("icon").getAsString() : "archetype:textures/gui/resource/default.png";
        ResourceLocation icon = parseResourceLocation(iconStr, fileId, "resource.icon");

        return new ResourceDefinition(typeKey, max, start, drain, regen, color, icon);
    }

    static ConditionDefinition parseCondition(JsonObject obj, ResourceLocation fileId, String fieldPath) throws ClassParseException {
        if (!obj.has("type")) {
            throw new ClassParseException(fileId, fieldPath, "Condition missing 'type' field");
        }
        String type = obj.get("type").getAsString();

        // Handle compound conditions (and/or/not)
        if ("and".equals(type) || "or".equals(type)) {
            if (!obj.has("conditions") || !obj.get("conditions").isJsonArray()) {
                throw new ClassParseException(fileId, fieldPath, "Compound condition '" + type + "' requires 'conditions' array");
            }
            JsonArray condArr = obj.getAsJsonArray("conditions");
            List<ConditionDefinition> children = new ArrayList<>();
            for (int i = 0; i < condArr.size(); i++) {
                children.add(parseCondition(condArr.get(i).getAsJsonObject(), fileId, fieldPath + ".conditions[" + i + "]"));
            }
            return new ConditionDefinition(type, null, children);
        }

        if ("not".equals(type)) {
            if (!obj.has("condition") || !obj.get("condition").isJsonObject()) {
                throw new ClassParseException(fileId, fieldPath, "'not' condition requires 'condition' object");
            }
            List<ConditionDefinition> children = new ArrayList<>();
            children.add(parseCondition(obj.getAsJsonObject("condition"), fileId, fieldPath + ".condition"));
            return new ConditionDefinition(type, null, children);
        }

        // Extract params (everything except "type")
        JsonObject params = new JsonObject();
        for (var entry : obj.entrySet()) {
            if (!"type".equals(entry.getKey())) {
                params.add(entry.getKey(), entry.getValue());
            }
        }

        return new ConditionDefinition(type, params);
    }

    private static List<PlayerClass.ExtraAbilitySection> parseExtraAbilitySections(JsonArray arr, ResourceLocation fileId) {
        List<PlayerClass.ExtraAbilitySection> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String parentSlot = obj.has("parent_slot") ? obj.get("parent_slot").getAsString() : "ability_1";
            String nameKey = obj.has("name") ? obj.get("name").getAsString() : "";
            int unlockLevel = obj.has("unlock_level") ? obj.get("unlock_level").getAsInt() : 0;

            List<PlayerClass.ExtraAbilityEntry> entries = new ArrayList<>();
            if (obj.has("entries") && obj.get("entries").isJsonArray()) {
                JsonArray entryArr = obj.getAsJsonArray("entries");
                for (int j = 0; j < entryArr.size(); j++) {
                    JsonObject entryObj = entryArr.get(j).getAsJsonObject();
                    String eName = entryObj.has("name") ? entryObj.get("name").getAsString() : "";
                    String eDesc = entryObj.has("description") ? entryObj.get("description").getAsString() : "";
                    entries.add(new PlayerClass.ExtraAbilityEntry(eName, eDesc));
                }
            }
            result.add(new PlayerClass.ExtraAbilitySection(parentSlot, nameKey, unlockLevel, entries));
        }
        return result;
    }

    private static List<PlayerClass.AbilityStatEntry> parseAbilityStats(JsonArray arr, ResourceLocation fileId) {
        List<PlayerClass.AbilityStatEntry> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String nameKey = obj.has("name") ? obj.get("name").getAsString() : "";
            float base = obj.has("base") ? obj.get("base").getAsFloat() : 0;
            String format = obj.has("format") ? obj.get("format").getAsString() : "int";

            List<PlayerClass.LevelBonus> bonuses = new ArrayList<>();
            if (obj.has("bonuses") && obj.get("bonuses").isJsonArray()) {
                JsonArray bArr = obj.getAsJsonArray("bonuses");
                for (int j = 0; j < bArr.size(); j++) {
                    JsonObject bObj = bArr.get(j).getAsJsonObject();
                    int level = bObj.get("level").getAsInt();
                    float value = bObj.get("value").getAsFloat();
                    bonuses.add(new PlayerClass.LevelBonus(level, value));
                }
            }
            result.add(new PlayerClass.AbilityStatEntry(nameKey, base, bonuses, format));
        }
        return result;
    }

    private static final java.util.Set<String> VALID_TRIGGERS = java.util.Set.of(
            "on_assign", "on_remove", "on_tick", "on_death", "on_respawn", "on_level_up");

    private static List<PlayerClass.CommandTrigger> parseCommands(JsonArray arr, ResourceLocation fileId) throws ClassParseException {
        List<PlayerClass.CommandTrigger> result = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String trigger = requireString(obj, "trigger", fileId, "commands[" + i + "]");
            if (!VALID_TRIGGERS.contains(trigger)) {
                throw new ClassParseException(fileId, "commands[" + i + "].trigger",
                        "Invalid trigger '" + trigger + "'. Valid: " + VALID_TRIGGERS);
            }
            String command = requireString(obj, "command", fileId, "commands[" + i + "]");
            int interval = obj.has("interval") ? obj.get("interval").getAsInt() : 20;
            if (interval <= 0) {
                throw new ClassParseException(fileId, "commands[" + i + "].interval", "Interval must be > 0");
            }
            result.add(new PlayerClass.CommandTrigger(trigger, command, interval));
        }
        return result;
    }

    // --- Utility methods ---

    private static String requireString(JsonObject obj, String key, ResourceLocation fileId) throws ClassParseException {
        return requireString(obj, key, fileId, null);
    }

    private static String requireString(JsonObject obj, String key, ResourceLocation fileId, @Nullable String parentField) throws ClassParseException {
        String field = parentField != null ? parentField + "." + key : key;
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) {
            throw new ClassParseException(fileId, field, "Required field '" + key + "' is missing or not a string");
        }
        return obj.get(key).getAsString();
    }

    private static ResourceLocation parseResourceLocation(String value, ResourceLocation fileId, String field) throws ClassParseException {
        try {
            return new ResourceLocation(value);
        } catch (Exception e) {
            throw new ClassParseException(fileId, field, "Invalid ResourceLocation: " + value, e);
        }
    }

    private static int parseColor(String hex, ResourceLocation fileId) throws ClassParseException {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            throw new ClassParseException(fileId, "color", "Invalid hex color: " + hex, e);
        }
    }

    private static AttributeModifier.Operation parseOperation(String op, ResourceLocation fileId, String field) throws ClassParseException {
        return switch (op.toLowerCase(Locale.ROOT)) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> throw new ClassParseException(fileId, field, "Invalid operation: " + op + ". Must be addition, multiply_base, or multiply_total");
        };
    }
}
