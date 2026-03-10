package com.mod.archetype.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mod.archetype.Archetype;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static ServerConfig serverConfig = new ServerConfig();
    private static ClientConfig clientConfig = new ClientConfig();

    public static ServerConfig server() {
        return serverConfig;
    }

    public static ClientConfig client() {
        return clientConfig;
    }

    public static void loadServerConfig(Path configDir) {
        Path file = configDir.resolve("archetype-server.json");
        serverConfig = loadConfig(file, ServerConfig.class, new ServerConfig());
    }

    public static void loadClientConfig(Path configDir) {
        Path file = configDir.resolve("archetype-client.json");
        clientConfig = loadConfig(file, ClientConfig.class, new ClientConfig());
    }

    public static void saveServerConfig(Path configDir) {
        saveConfig(configDir.resolve("archetype-server.json"), serverConfig);
    }

    public static void saveClientConfig(Path configDir) {
        saveConfig(configDir.resolve("archetype-client.json"), clientConfig);
    }

    public static void reloadServer(Path configDir) {
        loadServerConfig(configDir);
        Archetype.LOGGER.info("Server config reloaded");
    }

    private static <T> T loadConfig(Path file, Class<T> type, T defaultConfig) {
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                T config = GSON.fromJson(json, type);
                if (config != null) {
                    return config;
                }
            } catch (Exception e) {
                Archetype.LOGGER.error("Failed to load config {}: {}. Using defaults (file not overwritten).",
                        file.getFileName(), e.getMessage());
                return defaultConfig;
            }
        }
        // File doesn't exist, create default
        saveConfig(file, defaultConfig);
        return defaultConfig;
    }

    private static <T> void saveConfig(Path file, T config) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(config));
        } catch (IOException e) {
            Archetype.LOGGER.error("Failed to save config {}: {}", file.getFileName(), e.getMessage());
        }
    }
}
