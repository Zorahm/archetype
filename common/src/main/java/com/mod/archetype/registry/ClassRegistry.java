package com.mod.archetype.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass;
import com.mod.archetype.platform.PlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public class ClassRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final ClassRegistry INSTANCE = new ClassRegistry();

    private Map<ResourceLocation, PlayerClass> classes = Map.of();
    private Map<ResourceLocation, String> rawJsonData = Map.of();

    // Datapacks and config are stored separately so reload() can re-merge without a full datapack reload
    private Map<ResourceLocation, PlayerClass> datpackClasses = Map.of();
    private Map<ResourceLocation, String> datpackRawJson = Map.of();

    private ClassRegistry() {
        super(GSON, "archetype_classes");
    }

    public static ClassRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PlayerClass> newClasses = new HashMap<>();
        Map<ResourceLocation, String> newRawJson = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
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

    public Map<ResourceLocation, String> getRawJsonData() {
        return rawJsonData;
    }

    public void loadFromJsonStrings(Map<ResourceLocation, String> jsonMap) {
        Map<ResourceLocation, PlayerClass> newClasses = new HashMap<>();
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

    public Optional<PlayerClass> get(ResourceLocation id) {
        return Optional.ofNullable(classes.get(id));
    }

    public Collection<PlayerClass> getAll() {
        return classes.values();
    }

    public boolean exists(ResourceLocation id) {
        return classes.containsKey(id);
    }

    public Set<ResourceLocation> getAllIds() {
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

        Map<ResourceLocation, PlayerClass> merged = new HashMap<>(datpackClasses);
        merged.putAll(cfg.classes()); // config wins on conflict

        Map<ResourceLocation, String> mergedRaw = new HashMap<>(datpackRawJson);
        mergedRaw.putAll(cfg.rawJson());

        this.classes = Collections.unmodifiableMap(merged);
        this.rawJsonData = Collections.unmodifiableMap(mergedRaw);
        Archetype.LOGGER.info("Total archetype classes: {} ({} from config, {} from datapacks)",
            merged.size(), cfg.classes().size(), datpackClasses.size());
    }
}
