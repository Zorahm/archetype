package com.mod.archetype.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass;
import net.minecraft.resources.Identifier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConfigClassLoader {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String CONFIG_SUBDIR = "archetype/classes";

    public record LoadResult(
        Map<Identifier, PlayerClass> classes,
        Map<Identifier, String> rawJson
    ) {}

    public static LoadResult load(Path configDir) {
        Path classesDir = configDir.resolve(CONFIG_SUBDIR);
        if (!Files.isDirectory(classesDir)) {
            return new LoadResult(Map.of(), Map.of());
        }

        Map<Identifier, PlayerClass> classes = new HashMap<>();
        Map<Identifier, String> rawJson = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(classesDir, "*.json")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String id = fileName.substring(0, fileName.length() - 5);
                Identifier loc = Identifier.fromNamespaceAndPath(Archetype.MOD_ID, id);
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    PlayerClass cls = ClassJsonParser.parse(loc, json);
                    classes.put(loc, cls);
                    rawJson.put(loc, GSON.toJson(json));
                } catch (ClassParseException e) {
                    Archetype.LOGGER.error("Config class '{}': {}", fileName, e.getMessage());
                } catch (Exception e) {
                    Archetype.LOGGER.error("Failed to read config class '{}': {}", fileName, e.getMessage());
                }
            }
        } catch (IOException e) {
            Archetype.LOGGER.error("Failed to read config classes directory: {}", e.getMessage());
        }

        if (!classes.isEmpty()) {
            Archetype.LOGGER.info("Loaded {} custom class(es) from config/archetype/classes/", classes.size());
        }
        return new LoadResult(
            Collections.unmodifiableMap(classes),
            Collections.unmodifiableMap(rawJson)
        );
    }
}
