package com.mod.archetype.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mod.archetype.core.ClassCategory;
import com.mod.archetype.core.PlayerClass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ClassJsonParserTest {

    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath("testpack", "test_class");

    // --- Helpers ---

    private JsonObject json(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    private PlayerClass parseJson(String raw) throws ClassParseException {
        return ClassJsonParser.parse(TEST_ID, json(raw));
    }

    private String minimal() {
        return """
                {
                  "name": "class.test.name",
                  "description": "class.test.desc",
                  "icon": "testpack:textures/gui/class/test.png",
                  "color": "FF0000"
                }""";
    }

    // --- Required fields ---

    @Test
    void parseMinimalClass_succeeds() throws ClassParseException {
        PlayerClass pc = parseJson(minimal());
        assertEquals(TEST_ID, pc.getId());
        assertEquals("class.test.name", pc.getNameKey());
        assertEquals("class.test.desc", pc.getDescriptionKey());
        assertEquals(Identifier.fromNamespaceAndPath("testpack", "textures/gui/class/test.png"), pc.getIcon());
        assertEquals(0xFF0000, pc.getColor());
        assertEquals(ClassCategory.DAMAGE, pc.getCategory()); // default
        assertTrue(pc.getPassiveAbilities().isEmpty());
        assertTrue(pc.getActiveAbilities().isEmpty());
        assertTrue(pc.getAttributes().isEmpty());
        assertNull(pc.getResource());
        assertNull(pc.getSizeModifier());
    }

    @Test
    void missingName_throwsException() {
        String raw = """
                {
                  "description": "desc",
                  "icon": "testpack:textures/test.png",
                  "color": "FFFFFF"
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void missingDescription_throwsException() {
        String raw = """
                {
                  "name": "name",
                  "icon": "testpack:textures/test.png",
                  "color": "FFFFFF"
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void missingIcon_throwsException() {
        String raw = """
                {
                  "name": "name",
                  "description": "desc",
                  "color": "FFFFFF"
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void missingColor_throwsException() {
        String raw = """
                {
                  "name": "name",
                  "description": "desc",
                  "icon": "testpack:textures/test.png"
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Color parsing ---

    @Test
    void colorWithHash_parsed() throws ClassParseException {
        PlayerClass pc = parseJson(minimal().replace("\"FF0000\"", "\"#FF0000\""));
        assertEquals(0xFF0000, pc.getColor());
    }

    @Test
    void colorBlack_parsed() throws ClassParseException {
        PlayerClass pc = parseJson(minimal().replace("\"FF0000\"", "\"000000\""));
        assertEquals(0x000000, pc.getColor());
    }

    @Test
    void invalidColor_throwsException() {
        String raw = minimal().replace("\"FF0000\"", "\"ZZZZZZ\"");
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Category ---

    @ParameterizedTest
    @ValueSource(strings = {"DAMAGE", "TANK", "MOBILITY", "UTILITY"})
    void validCategory_parsed(String category) throws ClassParseException {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"category\": \"" + category + "\"");
        PlayerClass pc = parseJson(raw);
        assertEquals(ClassCategory.valueOf(category), pc.getCategory());
    }

    @Test
    void invalidCategory_throwsException() {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"category\": \"HEALER\"");
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Attributes ---

    @Test
    void attributes_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "attributes": [
                    { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 4.0 },
                    { "attribute": "minecraft:generic.attack_damage", "operation": "MULTIPLY_BASE", "value": 0.2 },
                    { "attribute": "minecraft:generic.movement_speed", "operation": "MULTIPLY_TOTAL", "value": 0.1 }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        assertEquals(3, pc.getAttributes().size());
        assertEquals(AttributeModifier.Operation.ADD_VALUE, pc.getAttributes().get(0).operation());
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_BASE, pc.getAttributes().get(1).operation());
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, pc.getAttributes().get(2).operation());
        assertEquals(4.0, pc.getAttributes().get(0).value());
    }

    @Test
    void invalidAttributeOperation_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "attributes": [
                    { "attribute": "minecraft:generic.max_health", "operation": "DIVIDE", "value": 1.0 }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Passive abilities ---

    @Test
    void passiveAbilities_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "passive_abilities": [
                    {
                      "type": "archetype:speed_boost",
                      "params": { "amplifier": 1 },
                      "positive": true,
                      "hidden": false,
                      "name": "passive.test.name",
                      "description": "passive.test.desc"
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        assertEquals(1, pc.getPassiveAbilities().size());
        PlayerClass.PassiveAbilityEntry p = pc.getPassiveAbilities().get(0);
        assertEquals(Identifier.fromNamespaceAndPath("archetype", "speed_boost"), p.type());
        assertTrue(p.positive());
        assertFalse(p.hidden());
        assertNull(p.activationCondition());
        assertEquals("passive.test.name", p.nameKey());
    }

    @Test
    void passiveAbilityWithCondition_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "passive_abilities": [
                    {
                      "type": "archetype:health_regen",
                      "condition": {
                        "type": "archetype:health_below_percent",
                        "threshold": 0.3
                      },
                      "positive": true,
                      "name": "p.name",
                      "description": "p.desc"
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        PlayerClass.PassiveAbilityEntry p = pc.getPassiveAbilities().get(0);
        assertNotNull(p.activationCondition());
        assertEquals("archetype:health_below_percent", p.activationCondition().type());
    }

    // --- Active abilities ---

    @Test
    void activeAbilities_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "active_abilities": [
                    {
                      "type": "archetype:blink",
                      "slot": "ability_1",
                      "cooldown": 100,
                      "resource_cost": 20,
                      "unlock_level": 5,
                      "params": { "range": 8.0 },
                      "name": "a.name",
                      "description": "a.desc",
                      "icon": "testpack:textures/gui/a.png",
                      "item": "minecraft:ender_pearl"
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        assertEquals(1, pc.getActiveAbilities().size());
        PlayerClass.ActiveAbilityEntry a = pc.getActiveAbilities().get(0);
        assertEquals("ability_1", a.slot());
        assertEquals(100, a.cooldownTicks());
        assertEquals(20, a.resourceCost());
        assertEquals(5, a.unlockLevel());
        assertEquals("minecraft:ender_pearl", a.item());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ability_1", "ability_2", "ability_3"})
    void validActiveSlots_parsed(String slot) throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "active_abilities": [
                    { "type": "archetype:blink", "slot": "%s", "name": "n", "description": "d" }
                  ]
                }""".formatted(slot);
        PlayerClass pc = parseJson(raw);
        assertEquals(slot, pc.getActiveAbilities().get(0).slot());
    }

    @Test
    void invalidActiveSlot_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "active_abilities": [
                    { "type": "archetype:blink", "slot": "ability_4", "name": "n", "description": "d" }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void negativeCooldown_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "active_abilities": [
                    { "type": "archetype:blink", "slot": "ability_1", "cooldown": -1, "name": "n", "description": "d" }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void negativeUnlockLevel_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "active_abilities": [
                    { "type": "archetype:blink", "slot": "ability_1", "unlock_level": -1, "name": "n", "description": "d" }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Resource ---

    @Test
    void resource_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "resource": {
                    "type": "resource.testpack.mana",
                    "max": 100,
                    "start": 50,
                    "drain_per_second": 2.0,
                    "regen_per_second": 5.0,
                    "color": "0000FF",
                    "icon": "testpack:textures/gui/resource/mana.png"
                  }
                }""";
        PlayerClass pc = parseJson(raw);
        PlayerClass.ResourceDefinition res = pc.getResource();
        assertNotNull(res);
        assertEquals("resource.testpack.mana", res.typeKey());
        assertEquals(100, res.maxValue());
        assertEquals(50, res.startValue());
        assertEquals(2.0f, res.drainPerSecond());
        assertEquals(5.0f, res.regenPerSecond());
        assertEquals(0x0000FF, res.color());
    }

    @Test
    void resourceMaxZero_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "resource": { "max": 0 }
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void resourceMaxNegative_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "resource": { "max": -10 }
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Conditions ---

    @Test
    void conditionAnd_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": {
                        "type": "and",
                        "conditions": [
                          { "type": "archetype:is_sneaking" },
                          { "type": "archetype:in_water" }
                        ]
                      },
                      "modifiers": [
                        { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 2.0 }
                      ]
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        assertEquals(1, pc.getConditionalAttributes().size());
        PlayerClass.ConditionDefinition cond = pc.getConditionalAttributes().get(0).condition();
        assertEquals("and", cond.type());
        assertEquals(2, cond.children().size());
        assertEquals("archetype:is_sneaking", cond.children().get(0).type());
        assertEquals("archetype:in_water", cond.children().get(1).type());
    }

    @Test
    void conditionOr_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": {
                        "type": "or",
                        "conditions": [
                          { "type": "archetype:time_of_day", "phase": "night" },
                          { "type": "archetype:is_sneaking" }
                        ]
                      },
                      "modifiers": [
                        { "attribute": "minecraft:generic.attack_damage", "operation": "MULTIPLY_BASE", "value": 0.1 }
                      ]
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        PlayerClass.ConditionDefinition cond = pc.getConditionalAttributes().get(0).condition();
        assertEquals("or", cond.type());
        assertEquals(2, cond.children().size());
    }

    @Test
    void conditionNot_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": {
                        "type": "not",
                        "condition": { "type": "archetype:in_water" }
                      },
                      "modifiers": [
                        { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 2.0 }
                      ]
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        PlayerClass.ConditionDefinition cond = pc.getConditionalAttributes().get(0).condition();
        assertEquals("not", cond.type());
        assertEquals(1, cond.children().size());
        assertEquals("archetype:in_water", cond.children().get(0).type());
    }

    @Test
    void conditionMissingType_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": { "threshold": 0.5 },
                      "modifiers": [
                        { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 1.0 }
                      ]
                    }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void conditionAndMissingConditionsArray_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": { "type": "and" },
                      "modifiers": [
                        { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 1.0 }
                      ]
                    }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void conditionNotMissingConditionObject_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "conditional_attributes": [
                    {
                      "condition": { "type": "not" },
                      "modifiers": [
                        { "attribute": "minecraft:generic.max_health", "operation": "ADDITION", "value": 1.0 }
                      ]
                    }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Command triggers ---

    @ParameterizedTest
    @ValueSource(strings = {"on_assign", "on_remove", "on_tick", "on_death", "on_respawn", "on_level_up"})
    void validCommandTriggers_parsed(String trigger) throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "commands": [
                    { "trigger": "%s", "command": "say test" }
                  ]
                }""".formatted(trigger);
        PlayerClass pc = parseJson(raw);
        assertEquals(1, pc.getCommands().size());
        assertEquals(trigger, pc.getCommands().get(0).trigger());
        assertEquals("say test", pc.getCommands().get(0).command());
        assertEquals(20, pc.getCommands().get(0).interval()); // default
    }

    @Test
    void invalidCommandTrigger_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "commands": [
                    { "trigger": "on_jump", "command": "say test" }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void commandIntervalZero_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "commands": [
                    { "trigger": "on_tick", "command": "say test", "interval": 0 }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    @Test
    void commandIntervalNegative_throwsException() {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "commands": [
                    { "trigger": "on_tick", "command": "say test", "interval": -5 }
                  ]
                }""";
        assertThrows(ClassParseException.class, () -> parseJson(raw));
    }

    // --- Optional fields ---

    @Test
    void sizeModifier_parsed() throws ClassParseException {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"size_modifier\": 0.8");
        PlayerClass pc = parseJson(raw);
        assertNotNull(pc.getSizeModifier());
        assertEquals(0.8f, pc.getSizeModifier(), 0.001f);
    }

    @Test
    void incompatibleWith_parsed() throws ClassParseException {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"incompatible_with\": [\"archetype:ram\", \"archetype:vi\"]");
        PlayerClass pc = parseJson(raw);
        assertEquals(2, pc.getIncompatibleWith().size());
        assertEquals(Identifier.fromNamespaceAndPath("archetype", "ram"), pc.getIncompatibleWith().get(0));
        assertEquals(Identifier.fromNamespaceAndPath("archetype", "vi"), pc.getIncompatibleWith().get(1));
    }

    @Test
    void progression_parsed() throws ClassParseException {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"progression\": [{\"level\": 10, \"key\": \"prog.key.10\"}, {\"level\": 20, \"key\": \"prog.key.20\"}]");
        PlayerClass pc = parseJson(raw);
        assertEquals(2, pc.getProgression().size());
        assertEquals(10, pc.getProgression().get(0).level());
        assertEquals("prog.key.10", pc.getProgression().get(0).descriptionKey());
    }

    @Test
    void lore_parsed() throws ClassParseException {
        String raw = minimal().replace("\"color\": \"FF0000\"",
                "\"color\": \"FF0000\", \"lore\": [\"lore.key.1\", \"lore.key.2\"]");
        PlayerClass pc = parseJson(raw);
        assertEquals(2, pc.getLoreKeys().size());
        assertEquals("lore.key.1", pc.getLoreKeys().get(0));
    }

    @Test
    void abilityStats_parsed() throws ClassParseException {
        String raw = """
                {
                  "name": "n", "description": "d",
                  "icon": "testpack:textures/t.png", "color": "FFFFFF",
                  "ability_stats": [
                    {
                      "name": "gui.stat.damage",
                      "base": 5.0,
                      "format": "float",
                      "bonuses": [
                        { "level": 10, "value": 1.0 },
                        { "level": 20, "value": 2.0 }
                      ]
                    }
                  ]
                }""";
        PlayerClass pc = parseJson(raw);
        assertEquals(1, pc.getAbilityStats().size());
        PlayerClass.AbilityStatEntry stat = pc.getAbilityStats().get(0);
        assertEquals("gui.stat.damage", stat.nameKey());
        assertEquals(5.0f, stat.baseValue());
        assertEquals("float", stat.format());
        assertEquals(2, stat.bonuses().size());
        assertEquals(10, stat.bonuses().get(0).level());
        assertEquals(1.0f, stat.bonuses().get(0).value());
    }

    // --- Integration: parse actual builtin class files ---

    @ParameterizedTest
    @ValueSource(strings = {"vi", "ram", "lin_qi", "ru_yi", "morph", "summoner"})
    void parseBuiltinClass_succeeds(String classId) throws ClassParseException, IOException {
        String path = "/data/archetype/archetype_classes/" + classId + ".json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, "Builtin class file not found: " + path);
            JsonObject jsonObj = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            Identifier id = Identifier.fromNamespaceAndPath("archetype", classId);
            PlayerClass pc = ClassJsonParser.parse(id, jsonObj);
            assertEquals(id, pc.getId());
            assertNotNull(pc.getNameKey());
            assertFalse(pc.getNameKey().isBlank());
        }
    }

    // --- Integration: parse custom datapack class ---

    @Test
    void parseCustomDatapackClass_succeeds() throws ClassParseException, IOException {
        String path = "/data/testpack/archetype_classes/custom_class.json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            assertNotNull(is, "Custom class file not found: " + path);
            JsonObject jsonObj = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            Identifier id = Identifier.fromNamespaceAndPath("testpack", "custom_class");
            PlayerClass pc = ClassJsonParser.parse(id, jsonObj);

            assertEquals(id, pc.getId());
            assertEquals("class.testpack.custom.name", pc.getNameKey());
            assertEquals(ClassCategory.MOBILITY, pc.getCategory());
            assertEquals(0xFF4400, pc.getColor());
            assertEquals(2, pc.getLoreKeys().size());
            assertEquals(2, pc.getAttributes().size());
            assertEquals(2, pc.getPassiveAbilities().size());
            assertEquals(2, pc.getActiveAbilities().size());
            assertNotNull(pc.getResource());
            assertEquals(100, pc.getResource().maxValue());
            assertEquals(5.0f, pc.getResource().regenPerSecond());
            assertNotNull(pc.getSizeModifier());
            assertEquals(0.95f, pc.getSizeModifier(), 0.001f);
            assertEquals(1, pc.getIncompatibleWith().size());
            assertEquals(Identifier.fromNamespaceAndPath("archetype", "ram"), pc.getIncompatibleWith().get(0));
            assertEquals(2, pc.getProgression().size());
            assertEquals(1, pc.getAbilityStats().size());
            assertEquals(1, pc.getCommands().size());
            assertEquals("on_assign", pc.getCommands().get(0).trigger());
            assertEquals(1, pc.getConditionalAttributes().size());

            // passive with condition
            PlayerClass.PassiveAbilityEntry regenPassive = pc.getPassiveAbilities().get(1);
            assertNotNull(regenPassive.activationCondition());
            assertEquals("archetype:health_below_percent", regenPassive.activationCondition().type());

            // active ability slot
            assertEquals("ability_1", pc.getActiveAbilities().get(0).slot());
            assertEquals("ability_2", pc.getActiveAbilities().get(1).slot());
            assertEquals(10, pc.getActiveAbilities().get(1).unlockLevel());
        }
    }
}
