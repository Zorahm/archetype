package com.mod.archetype.core;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbilityRegistry;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.PassiveAbility;
import com.mod.archetype.advancement.ClassActionTrigger;
import com.mod.archetype.condition.Condition;
import com.mod.archetype.condition.ConditionRegistry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.NetworkHandler;
import com.mod.archetype.platform.PlayerDataAccess;
import com.mod.archetype.ability.active.RageDashAbility;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassManager {

    private static final ClassManager INSTANCE = new ClassManager();

    private final Map<UUID, ActiveClassInstance> activeInstances = new ConcurrentHashMap<>();

    public static ClassManager getInstance() {
        return INSTANCE;
    }

    // --- Registry (delegates to ClassRegistry) ---

    @Nullable
    public PlayerClass getClassDefinition(ResourceLocation id) {
        return ClassRegistry.getInstance().get(id).orElse(null);
    }

    public Collection<PlayerClass> getAllClasses() {
        return ClassRegistry.getInstance().getAll();
    }

    // --- Class Assignment ---

    public AssignResult assignClass(ServerPlayer player, ResourceLocation classId) {
        assert !player.level().isClientSide : "assignClass must be called on server";

        PlayerClass classDef = ClassRegistry.getInstance().get(classId).orElse(null);
        if (classDef == null) {
            return AssignResult.failure("archetype.error.class_not_found");
        }

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);

        // If player has an existing class, remove it first
        if (data.hasClass()) {
            removeClass(player);
        }

        // Apply attribute modifiers
        applyAttributes(player, classDef);

        // Create passive ability instances
        List<PassiveAbility> passives = new ArrayList<>();
        for (PlayerClass.PassiveAbilityEntry entry : classDef.getPassiveAbilities()) {
            PassiveAbility passive = AbilityRegistry.getInstance().createPassive(entry);
            if (passive != null) {
                passive.onApply(player);
                passives.add(passive);
            }
        }

        // Create active ability instances
        Map<String, ActiveAbility> actives = new LinkedHashMap<>();
        for (PlayerClass.ActiveAbilityEntry entry : classDef.getActiveAbilities()) {
            ActiveAbility ability = AbilityRegistry.getInstance().createActive(entry);
            if (ability != null) {
                actives.put(entry.slot(), ability);
            }
        }

        // Initialize resource
        if (classDef.getResource() != null) {
            data.setResourceCurrent(classDef.getResource().startValue());
        }

        // Update data
        data.setCurrentClassId(classId);
        data.setClassAssignedTime(player.level().getGameTime());
        data.getCooldowns().clear();
        data.getToggleStates().clear();

        // Cache the active instance
        activeInstances.put(player.getUUID(), new ActiveClassInstance(classDef, passives, actives));

        // Sync with client
        syncToClient(player);

        // Publish event
        ArchetypeEvents.CLASS_ASSIGNED.invoker().onAssigned(player, classDef);

        // Trigger advancements
        ClassActionTrigger.INSTANCE.trigger(player, "choose_class", data.getClassLevel());

        Archetype.LOGGER.info("Player {} assigned class {}", player.getName().getString(), classId);
        return AssignResult.success();
    }

    public AssignResult removeClass(ServerPlayer player) {
        assert !player.level().isClientSide : "removeClass must be called on server";

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) {
            return AssignResult.failure("archetype.error.no_class");
        }

        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        ActiveClassInstance instance = activeInstances.get(player.getUUID());

        // Remove attribute modifiers
        if (classDef != null) {
            removeAttributes(player, classDef);
        }

        // Remove passives
        if (instance != null) {
            for (PassiveAbility passive : instance.getActivePassives()) {
                passive.onRemove(player);
            }

            // Force deactivate actives
            for (ActiveAbility ability : instance.getActiveAbilities().values()) {
                if (ability.isActive()) {
                    ability.forceDeactivate(player);
                }
            }
        }

        // Clear data
        data.getCooldowns().clear();
        data.getToggleStates().clear();
        data.setResourceCurrent(0);
        data.getActiveConditionalSets().clear();

        ResourceLocation oldClassId = data.getCurrentClassId();
        data.setCurrentClassId(null);

        // Remove cached instance
        activeInstances.remove(player.getUUID());

        // Sync with client
        syncToClient(player);

        // Publish event
        if (classDef != null) {
            ArchetypeEvents.CLASS_REMOVED.invoker().onRemoved(player, classDef);
        }

        Archetype.LOGGER.info("Player {} class removed (was {})", player.getName().getString(), oldClassId);
        return AssignResult.success();
    }

    // --- Server Tick ---

    public void tickPlayer(ServerPlayer player) {
        if (player.isDeadOrDying()) return;

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) {
            // Instance lost (e.g. after reload) — rebuild it
            rebuildInstance(player, data);
            instance = activeInstances.get(player.getUUID());
            if (instance == null) return;
        }

        instance.incrementTickCounter();
        int tick = instance.getTickCounter();

        // Every tick: decrease cooldowns
        data.getCooldowns().replaceAll((k, v) -> Math.max(0, v - 1));
        data.getCooldowns().values().removeIf(v -> v <= 0);

        // Every tick: tick active/duration abilities
        for (ActiveAbility ability : instance.getActiveAbilities().values()) {
            if (ability.isActive()) {
                ability.tickActive(player);
            }
        }

        // Every 20 ticks: check conditional attributes
        if (tick % 20 == 0) {
            checkConditionalAttributes(player, instance, data);
        }

        // Every tick: tick passives (each passive manages its own frequency internally)
        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.tick(player);
        }

        // Every 10 ticks: update resource drain/regen
        if (tick % 10 == 0) {
            updateResource(player, instance, data);
        }

        // Every 20 ticks: sync to client
        if (tick % 20 == 0) {
            syncToClient(player);
        }
    }

    // --- Event Handlers ---

    public void onPlayerDeath(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        if (classDef != null && classDef.getResource() != null) {
            data.setResourceCurrent(classDef.getResource().startValue());
        }
        // Cooldowns are preserved across death
    }

    public void onPlayerRespawn(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        if (classDef == null) {
            // Class was removed from registry
            Archetype.LOGGER.warn("Player {} has class {} which no longer exists, removing",
                    player.getName().getString(), data.getCurrentClassId());
            data.setCurrentClassId(null);
            activeInstances.remove(player.getUUID());
            syncToClient(player);
            return;
        }

        // Re-apply attributes (Minecraft resets them on death)
        applyAttributes(player, classDef);

        // Rebuild instance (passives need to be re-created)
        rebuildInstance(player, data);

        syncToClient(player);
    }

    public void onPlayerJoin(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (data.hasClass()) {
            PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
            if (classDef == null) {
                Archetype.LOGGER.warn("Player {} has class {} which no longer exists, removing",
                        player.getName().getString(), data.getCurrentClassId());
                data.setCurrentClassId(null);
                syncToClient(player);
                return;
            }

            applyAttributes(player, classDef);
            rebuildInstance(player, data);
            syncToClient(player);
        } else {
            // No class — open selection screen
            NetworkHandler.INSTANCE.sendToPlayer(player,
                    new com.mod.archetype.network.OpenClassSelectionPacket(
                            com.mod.archetype.network.OpenClassSelectionPacket.MODE_FIRST_SELECT));
        }
    }

    public void onPlayerLeave(ServerPlayer player) {
        activeInstances.remove(player.getUUID());
    }

    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return;

        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.onPlayerAttack(player, target, source);
        }
    }

    public void onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return;

        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.onPlayerHurt(player, source, amount);
        }
    }

    public void onPlayerEat(ServerPlayer player, ItemStack food) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return;

        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.onPlayerEat(player, food);
        }
    }

    // --- Getters ---

    @Nullable
    public ActiveClassInstance getInstance(ServerPlayer player) {
        return activeInstances.get(player.getUUID());
    }

    @Nullable
    public ActiveClassInstance getInstance(UUID playerId) {
        return activeInstances.get(playerId);
    }

    // --- Internal ---

    private void applyAttributes(ServerPlayer player, PlayerClass classDef) {
        for (PlayerClass.AttributeModifierEntry entry : classDef.getAttributes()) {
            var attribute = player.level().registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                    .getOptional(entry.attribute());

            if (attribute.isEmpty()) {
                Archetype.LOGGER.warn("Unknown attribute: {}", entry.attribute());
                continue;
            }

            AttributeInstance attrInstance = player.getAttribute(attribute.get());
            if (attrInstance == null) continue;

            UUID modifierUUID = generateModifierUUID(entry.attribute());
            // Remove existing modifier if present
            attrInstance.removeModifier(modifierUUID);

            AttributeModifier modifier = new AttributeModifier(
                    modifierUUID,
                    "archetype:class_" + entry.attribute(),
                    entry.value(),
                    entry.operation()
            );
            attrInstance.addPermanentModifier(modifier);
        }
    }

    private void removeAttributes(ServerPlayer player, PlayerClass classDef) {
        for (PlayerClass.AttributeModifierEntry entry : classDef.getAttributes()) {
            var attribute = player.level().registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                    .getOptional(entry.attribute());

            if (attribute.isEmpty()) continue;

            AttributeInstance attrInstance = player.getAttribute(attribute.get());
            if (attrInstance == null) continue;

            UUID modifierUUID = generateModifierUUID(entry.attribute());
            attrInstance.removeModifier(modifierUUID);
        }

        // Also remove conditional attribute modifiers
        for (int i = 0; i < classDef.getConditionalAttributes().size(); i++) {
            PlayerClass.ConditionalAttributeEntry cond = classDef.getConditionalAttributes().get(i);
            for (PlayerClass.AttributeModifierEntry entry : cond.modifiers()) {
                var attribute = player.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                        .getOptional(entry.attribute());

                if (attribute.isEmpty()) continue;

                AttributeInstance attrInstance = player.getAttribute(attribute.get());
                if (attrInstance == null) continue;

                UUID modifierUUID = generateConditionalModifierUUID(entry.attribute(), i);
                attrInstance.removeModifier(modifierUUID);
            }
        }
    }

    private void checkConditionalAttributes(ServerPlayer player, ActiveClassInstance instance, PlayerClassData data) {
        PlayerClass classDef = instance.getClassDefinition();
        List<PlayerClass.ConditionalAttributeEntry> conditionals = classDef.getConditionalAttributes();

        for (int i = 0; i < conditionals.size(); i++) {
            PlayerClass.ConditionalAttributeEntry condEntry = conditionals.get(i);
            Condition condition = ConditionRegistry.getInstance().create(condEntry.condition());
            boolean shouldBeActive = condition.test(player);
            boolean isActive = data.getActiveConditionalSets().contains(i);

            if (shouldBeActive && !isActive) {
                // Apply modifiers
                for (PlayerClass.AttributeModifierEntry mod : condEntry.modifiers()) {
                    applyConditionalModifier(player, mod, i);
                }
                data.getActiveConditionalSets().add(i);
            } else if (!shouldBeActive && isActive) {
                // Remove modifiers
                for (PlayerClass.AttributeModifierEntry mod : condEntry.modifiers()) {
                    removeConditionalModifier(player, mod, i);
                }
                data.getActiveConditionalSets().remove(i);
            }
        }
    }

    private void applyConditionalModifier(ServerPlayer player, PlayerClass.AttributeModifierEntry entry, int condIndex) {
        var attribute = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                .getOptional(entry.attribute());

        if (attribute.isEmpty()) return;

        AttributeInstance attrInstance = player.getAttribute(attribute.get());
        if (attrInstance == null) return;

        UUID modifierUUID = generateConditionalModifierUUID(entry.attribute(), condIndex);
        attrInstance.removeModifier(modifierUUID);

        AttributeModifier modifier = new AttributeModifier(
                modifierUUID,
                "archetype:cond_" + condIndex + "_" + entry.attribute(),
                entry.value(),
                entry.operation()
        );
        attrInstance.addTransientModifier(modifier);
    }

    private void removeConditionalModifier(ServerPlayer player, PlayerClass.AttributeModifierEntry entry, int condIndex) {
        var attribute = player.level().registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                .getOptional(entry.attribute());

        if (attribute.isEmpty()) return;

        AttributeInstance attrInstance = player.getAttribute(attribute.get());
        if (attrInstance == null) return;

        UUID modifierUUID = generateConditionalModifierUUID(entry.attribute(), condIndex);
        attrInstance.removeModifier(modifierUUID);
    }

    private void updateResource(ServerPlayer player, ActiveClassInstance instance, PlayerClassData data) {
        PlayerClass.ResourceDefinition resource = instance.getClassDefinition().getResource();
        if (resource == null) return;

        float current = data.getResourceCurrent();
        float drain = resource.drainPerSecond() * 0.5f; // 10 ticks = 0.5 sec

        // Check if regen is blocked (e.g. by Ram's obsidian dome)
        float regen = RageDashAbility.isRegenBlocked(player)
                ? 0 : resource.regenPerSecond() * 0.5f;

        current = current - drain + regen;
        current = Math.max(0, Math.min(resource.maxValue(), current));
        data.setResourceCurrent(current);
    }

    private void rebuildInstance(ServerPlayer player, PlayerClassData data) {
        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        if (classDef == null) return;

        List<PassiveAbility> passives = new ArrayList<>();
        for (PlayerClass.PassiveAbilityEntry entry : classDef.getPassiveAbilities()) {
            PassiveAbility passive = AbilityRegistry.getInstance().createPassive(entry);
            if (passive != null) {
                passive.onApply(player);
                passives.add(passive);
            }
        }

        Map<String, ActiveAbility> actives = new LinkedHashMap<>();
        for (PlayerClass.ActiveAbilityEntry entry : classDef.getActiveAbilities()) {
            ActiveAbility ability = AbilityRegistry.getInstance().createActive(entry);
            if (ability != null) {
                actives.put(entry.slot(), ability);
            }
        }

        activeInstances.put(player.getUUID(), new ActiveClassInstance(classDef, passives, actives));
    }

    private void syncToClient(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) {
            NetworkHandler.INSTANCE.sendToPlayer(player,
                    new com.mod.archetype.network.SyncClassDataPacket(
                            false, null, 0, 0, 0, 0,
                            java.util.Map.of(), java.util.Map.of()));
            return;
        }

        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        float resourceMax = classDef != null && classDef.getResource() != null
                ? classDef.getResource().maxValue() : 0;

        // Build cooldown entries
        Map<ResourceLocation, com.mod.archetype.network.SyncClassDataPacket.CooldownEntry> cooldownEntries = new HashMap<>();
        for (var entry : data.getCooldowns().entrySet()) {
            cooldownEntries.put(entry.getKey(),
                    new com.mod.archetype.network.SyncClassDataPacket.CooldownEntry(entry.getValue(), entry.getValue()));
        }

        NetworkHandler.INSTANCE.sendToPlayer(player,
                new com.mod.archetype.network.SyncClassDataPacket(
                        true, data.getCurrentClassId(),
                        data.getClassLevel(), data.getClassExperience(),
                        data.getResourceCurrent(), resourceMax,
                        cooldownEntries, new HashMap<>(data.getToggleStates())));
    }

    private static UUID generateModifierUUID(ResourceLocation attribute) {
        return UUID.nameUUIDFromBytes(("archetype:class_" + attribute).getBytes());
    }

    private static UUID generateConditionalModifierUUID(ResourceLocation attribute, int condIndex) {
        return UUID.nameUUIDFromBytes(("archetype:cond_" + condIndex + "_" + attribute).getBytes());
    }

    // --- Result ---

    public record AssignResult(boolean succeeded, @Nullable String failReasonKey) {
        public static AssignResult success() {
            return new AssignResult(true, null);
        }

        public static AssignResult failure(String reasonKey) {
            return new AssignResult(false, reasonKey);
        }
    }
}
