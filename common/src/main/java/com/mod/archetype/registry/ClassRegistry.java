package com.mod.archetype.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.platform.PlatformHelper;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.util.*;

public class ClassRegistry extends SimplePreparableReloadListener<Map<Identifier, JsonObject>> {

    private static final Gson GSON = new GsonBuilder().create();
    private static final ClassRegistry INSTANCE = new ClassRegistry();
    private static final FileToIdConverter FILE_TO_ID = FileToIdConverter.json("archetype_classes");

    private Map<Identifier, PlayerClass> classes = Map.of();
    private Map<Identifier, String> rawJsonData = Map.of();

    // Datapacks and config are stored separately so reload() can re-merge without a full datapack reload
    private Map<Identifier, PlayerClass> datpackClasses = Map.of();
    private Map<Identifier, String> datpackRawJson = Map.of();

    private ClassRegistry() {
    }

    public static ClassRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    protected Map<Identifier, JsonObject> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonObject> result = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : FILE_TO_ID.listMatchingResources(manager).entrySet()) {
            Identifier fileId = FILE_TO_ID.fileToId(entry.getKey());
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    result.put(fileId, json);
                }
            } catch (Exception e) {
                Archetype.LOGGER.error("Failed to read archetype class '{}': {}", fileId, e.getMessage());
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, JsonObject> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, PlayerClass> newClasses = new HashMap<>();
        Map<Identifier, String> newRawJson = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<Identifier, JsonObject> entry : map.entrySet()) {
            Identifier fileId = entry.getKey();
            try {
                JsonObject json = entry.getValue();
                PlayerClass playerClass = ClassJsonParser.parse(fileId, json);
                newClasses.put(fileId, playerClass);
                newRawJson.put(fileId, GSON.toJson(json));
            } catch (ClassParseException e) {
                Archetype.LOGGER.error(e.getMessage());
                errorCount++;
            } catch (Exception e) {
                Archetype.LOGGER.error("Unexpected error parsing class '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.datpackClasses = Collections.unmodifiableMap(newClasses);
        this.datpackRawJson = Collections.unmodifiableMap(newRawJson);
        Archetype.LOGGER.info("Loaded {} archetype classes from datapacks ({} failed)", newClasses.size(), errorCount);

        mergeWithConfig();
    }

    public Map<Identifier, String> getRawJsonData() {
        return rawJsonData;
    }

    public void loadFromJsonStrings(Map<Identifier, String> jsonMap) {
        Map<Identifier, PlayerClass> newClasses = new HashMap<>();
        for (var entry : jsonMap.entrySet()) {
            try {
                JsonObject json = GSON.fromJson(entry.getValue(), JsonObject.class);
                PlayerClass playerClass = ClassJsonParser.parse(entry.getKey(), json);
                newClasses.put(entry.getKey(), playerClass);
            } catch (Exception e) {
                Archetype.LOGGER.error("Failed to parse synced class '{}': {}", entry.getKey(), e.getMessage());
            }
        }
        this.classes = Collections.unmodifiableMap(newClasses);
        this.rawJsonData = Collections.unmodifiableMap(jsonMap);
        Archetype.LOGGER.info("Synced {} archetype classes from server", newClasses.size());
    }

    public Optional<PlayerClass> get(Identifier id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Collection<PlayerClass> getAll() {
        return classes.values();
    }

    public boolean exists(Identifier id) {
        return classes.containsKey(id);
    }

    public Set<Identifier> getAllIds() {
        return classes.keySet();
    }

    public int getClassCount() {
        return classes.size();
    }

    public void reload(ResourceManager manager) {
        Archetype.LOGGER.info("Reloading config classes...");
        mergeWithConfig();
        Archetype.LOGGER.info("Config reload done. Total classes: {}", classes.size());
    }

    private void mergeWithConfig() {
        ConfigClassLoader.LoadResult cfg = ConfigClassLoader.load(PlatformHelper.INSTANCE.getConfigDir());

        Map<Identifier, PlayerClass> merged = new HashMap<>(datpackClasses);
        merged.putAll(cfg.classes()); // config wins on conflict

        Map<Identifier, String> mergedRaw = new HashMap<>(datpackRawJson);
        mergedRaw.putAll(cfg.rawJson());

        this.classes = Collections.unmodifiableMap(merged);
        this.rawJsonData = Collections.unmodifiableMap(mergedRaw);
        Archetype.LOGGER.info("Total archetype classes: {} ({} from config, {} from datapacks)",
            merged.size(), cfg.classes().size(), datpackClasses.size());
    }
}
