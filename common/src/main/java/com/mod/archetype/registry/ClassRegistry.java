package com.mod.archetype.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public class ClassRegistry extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final ClassRegistry INSTANCE = new ClassRegistry();

    private Map<ResourceLocation, PlayerClass> classes = Map.of();

    private ClassRegistry() {
        super(GSON, "archetype_classes");
    }

    public static ClassRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, PlayerClass> newClasses = new HashMap<>();
        int errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                PlayerClass playerClass = ClassJsonParser.parse(fileId, json);
                newClasses.put(fileId, playerClass);
            } catch (ClassParseException e) {
                Archetype.LOGGER.error(e.getMessage());
                errorCount++;
            } catch (Exception e) {
                Archetype.LOGGER.error("Unexpected error parsing class '{}': {}", fileId, e.getMessage());
                errorCount++;
            }
        }

        this.classes = Collections.unmodifiableMap(newClasses);
        Archetype.LOGGER.info("Loaded {} archetype classes ({} failed)", newClasses.size(), errorCount);
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
        // Force a manual reload by delegating to the resource manager
        // The actual reload happens through the datapack reload system
        Archetype.LOGGER.info("Class registry reload requested");
    }
}
