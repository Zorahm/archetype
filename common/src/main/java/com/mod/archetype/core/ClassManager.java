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
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import com.mod.archetype.config.ConfigManager;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassManager {

    private static final ClassManager INSTANCE = new ClassManager();

    private final Map<UUID, ActiveClassInstance> activeInstances = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> playerUsingItem = new ConcurrentHashMap<>();

    public static ClassManager getInstance() {
        return INSTANCE;
    }

    // --- Registry (delegates to ClassRegistry) ---

    @Nullable
    public PlayerClass getClassDefinition(Identifier id) {
        return ClassRegistry.getInstance().get(id).orElse(null);
    }

    public Collection<PlayerClass> getAllClasses() {
        return ClassRegistry.getInstance().getAll();
    }

    // --- Class Assignment ---

    public AssignResult assignClass(ServerPlayer player, Identifier classId) {
        assert !player.level().isClientSide() : "assignClass must be called on server";

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
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));

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

        // Track tried classes
        data.addTriedClass(classId);

        // Trigger advancements
        ClassActionTrigger.INSTANCE.trigger(player, "choose_class", data.getClassLevel());

        // "Осознанный выбор" — tried all registered classes
        int totalClasses = ClassRegistry.getInstance().getAll().size();
        if (totalClasses > 0 && data.getTriedClasses().size() >= totalClasses) {
            ClassActionTrigger.INSTANCE.trigger(player, "all_classes_tried", data.getClassLevel());
        }

        // Execute on_assign commands
        executeCommands(player, classDef, "on_assign");

        Archetype.LOGGER.info("Player {} assigned class {}", player.getName().getString(), classId);
        return AssignResult.success();
    }

    public AssignResult removeClass(ServerPlayer player) {
        assert !player.level().isClientSide() : "removeClass must be called on server";

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

        // Execute on_remove commands (before clearing class id)
        if (classDef != null) {
            executeCommands(player, classDef, "on_remove");
        }

        Identifier oldClassId = data.getCurrentClassId();
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

        // Class level = player XP level (1:1) — updated first so tickActive reads current level
        int xpLevel = player.experienceLevel;
        int oldLevel = data.getClassLevel();
        if (xpLevel != oldLevel) {
            data.setClassLevel(xpLevel);
            if (xpLevel > oldLevel) {
                ArchetypeEvents.CLASS_LEVEL_UP.invoker().onLevelUp(player, xpLevel);
                ClassActionTrigger.INSTANCE.trigger(player, "level_up", xpLevel);
                executeCommands(player, instance.getClassDefinition(), "on_level_up", xpLevel);
            }
        }

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

        // Detect when player finishes eating/drinking
        trackItemUseFinished(player, instance);

        // Every 10 ticks: update resource drain/regen
        if (tick % 10 == 0) {
            updateResource(player, instance, data);
        }

        // Every 100 ticks: check End City entry for "Кровное предательство"
        if (tick % 100 == 0) {
            checkEndCityEntry(player, data);
        }

        // on_tick commands
        executeTickCommands(player, instance.getClassDefinition(), tick);

        // Every 20 ticks: sync to client
        if (tick % 20 == 0) {
            syncToClient(player);
        }
    }

    public void grantClassExperience(ServerPlayer player, int amount) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        int maxLevel = ConfigManager.server().maxClassLevel;
        int[] expTable = new int[maxLevel + 1];
        for (int i = 1; i <= maxLevel; i++) {
            expTable[i] = (int) (100 * Math.pow(i, 1.5));
        }

        int oldLevel = data.getClassLevel();
        data.addExperience(amount, maxLevel, expTable);
        int newLevel = data.getClassLevel();

        if (newLevel > oldLevel) {
            ArchetypeEvents.CLASS_LEVEL_UP.invoker().onLevelUp(player, newLevel);
            ClassActionTrigger.INSTANCE.trigger(player, "level_up", newLevel);
        }

        syncToClient(player);
    }

    private void trackItemUseFinished(ServerPlayer player, ActiveClassInstance instance) {
        UUID uuid = player.getUUID();
        if (player.isUsingItem()) {
            ItemStack using = player.getUseItem().copy();
            ItemUseAnimation anim = using.getUseAnimation();
            if (anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK) {
                playerUsingItem.put(uuid, using);
            }
        } else {
            ItemStack wasUsing = playerUsingItem.remove(uuid);
            if (wasUsing != null && !wasUsing.isEmpty()) {
                onPlayerEat(player, wasUsing);
            }
        }
    }

    // --- Event Handlers ---

    public void onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        if (!data.hasClass()) return;

        PlayerClass classDef = ClassRegistry.getInstance().get(data.getCurrentClassId()).orElse(null);
        if (classDef != null && classDef.getResource() != null) {
            data.setResourceCurrent(classDef.getResource().startValue());
        }
        if (classDef != null) {
            executeCommands(player, classDef, "on_death");
        }
        // Cooldowns are preserved across death

        // PvP kill advancements — trigger on the killer
        Entity killer = damageSource.getEntity();
        if (killer instanceof ServerPlayer killerPlayer) {
            PlayerClassData killerData = PlayerDataAccess.INSTANCE.getClassData(killerPlayer);
            if (killerData.hasClass()) {
                // "Разница навыков" — kill another player while having a class
                ClassActionTrigger.INSTANCE.trigger(killerPlayer, "ability_kill_player", killerData.getClassLevel());

                // "Спасение энда" / "Свержение энда" — kill max-level player of specific class
                String victimClassId = data.getCurrentClassId().toString();
                int victimLevel = data.getClassLevel();
                ClassActionTrigger.INSTANCE.trigger(killerPlayer, "kill_class_player", victimLevel, victimClassId);
            }
        }
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
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));

        // Rebuild instance (passives need to be re-created)
        rebuildInstance(player, data);

        // Execute on_respawn commands
        executeCommands(player, classDef, "on_respawn");

        syncToClient(player);
    }

    public void onPlayerJoin(ServerPlayer player) {
        // Sync class definitions to client (needed for non-host players in multiplayer)
        NetworkHandler.INSTANCE.sendToPlayer(player,
                new com.mod.archetype.network.SyncClassDefinitionsPacket(
                        ClassRegistry.getInstance().getRawJsonData()));

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
        playerUsingItem.remove(player.getUUID());
    }

    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return;

        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.onPlayerAttack(player, target, source);
        }
    }

    public boolean shouldCancelDamage(ServerPlayer player, DamageSource source) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return false;

        for (PassiveAbility passive : instance.getActivePassives()) {
            if (passive.shouldCancelDamage(player, source)) return true;
        }
        return false;
    }

    public float getFallDamageMultiplier(ServerPlayer player) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return 1.0f;

        for (ActiveAbility ability : instance.getActiveAbilities().values()) {
            if (ability instanceof com.mod.archetype.ability.active.FormShiftAbility formShift) {
                if (formShift.isFormActive()) {
                    return formShift.getFallDamageMultiplier();
                }
            }
        }
        return 1.0f;
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

    public boolean shouldCancelItemUse(ServerPlayer player, ItemStack item) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return false;

        for (PassiveAbility passive : instance.getActivePassives()) {
            if (passive.shouldCancelItemUse(player, item)) return true;
        }
        return false;
    }

    public boolean onEntityInteract(ServerPlayer player, Entity entity) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return false;

        for (PassiveAbility passive : instance.getActivePassives()) {
            if (passive.onEntityInteract(player, entity)) return true;
        }
        return false;
    }

    public void onBlockBreak(ServerPlayer player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        ActiveClassInstance instance = activeInstances.get(player.getUUID());
        if (instance == null) return;

        for (PassiveAbility passive : instance.getActivePassives()) {
            passive.onBlockBreak(player, pos, state);
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
            var attributeHolder = player.level().registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                    .get(entry.attribute());

            if (attributeHolder.isEmpty()) {
                Archetype.LOGGER.warn("Unknown attribute: {}", entry.attribute());
                continue;
            }

            AttributeInstance attrInstance = player.getAttribute(attributeHolder.get());
            if (attrInstance == null) continue;

            Identifier modifierId = generateModifierId(entry.attribute());
            // Remove existing modifier if present
            attrInstance.removeModifier(modifierId);

            AttributeModifier modifier = new AttributeModifier(
                    modifierId,
                    entry.value(),
                    entry.operation()
            );
            attrInstance.addPermanentModifier(modifier);
        }
    }

    private void removeAttributes(ServerPlayer player, PlayerClass classDef) {
        for (PlayerClass.AttributeModifierEntry entry : classDef.getAttributes()) {
            var attributeHolder = player.level().registryAccess()
                    .lookupOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                    .get(entry.attribute());

            if (attributeHolder.isEmpty()) continue;

            AttributeInstance attrInstance = player.getAttribute(attributeHolder.get());
            if (attrInstance == null) continue;

            Identifier modifierId = generateModifierId(entry.attribute());
            attrInstance.removeModifier(modifierId);
        }

        // Also remove conditional attribute modifiers
        for (int i = 0; i < classDef.getConditionalAttributes().size(); i++) {
            PlayerClass.ConditionalAttributeEntry cond = classDef.getConditionalAttributes().get(i);
            for (PlayerClass.AttributeModifierEntry entry : cond.modifiers()) {
                var attributeHolder = player.level().registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                        .get(entry.attribute());

                if (attributeHolder.isEmpty()) continue;

                AttributeInstance attrInstance = player.getAttribute(attributeHolder.get());
                if (attrInstance == null) continue;

                Identifier modifierId = generateConditionalModifierId(entry.attribute(), i);
                attrInstance.removeModifier(modifierId);
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
        var attributeHolder = player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                .get(entry.attribute());

        if (attributeHolder.isEmpty()) return;

        AttributeInstance attrInstance = player.getAttribute(attributeHolder.get());
        if (attrInstance == null) return;

        Identifier modifierId = generateConditionalModifierId(entry.attribute(), condIndex);
        attrInstance.removeModifier(modifierId);

        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                entry.value(),
                entry.operation()
        );
        attrInstance.addTransientModifier(modifier);
    }

    private void removeConditionalModifier(ServerPlayer player, PlayerClass.AttributeModifierEntry entry, int condIndex) {
        var attributeHolder = player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ATTRIBUTE)
                .get(entry.attribute());

        if (attributeHolder.isEmpty()) return;

        AttributeInstance attrInstance = player.getAttribute(attributeHolder.get());
        if (attrInstance == null) return;

        Identifier modifierId = generateConditionalModifierId(entry.attribute(), condIndex);
        attrInstance.removeModifier(modifierId);
    }

    private void updateResource(ServerPlayer player, ActiveClassInstance instance, PlayerClassData data) {
        PlayerClass.ResourceDefinition resource = instance.getClassDefinition().getResource();
        if (resource == null) return;

        float current = data.getResourceCurrent();
        float drain = resource.drainPerSecond() * 0.5f; // 10 ticks = 0.5 sec

        float regen = resource.regenPerSecond() * 0.5f;

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

    private void checkEndCityEntry(ServerPlayer player, PlayerClassData data) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        // Only in The End dimension
        if (!serverLevel.dimension().equals(net.minecraft.world.level.Level.END)) return;

        Structure endCity = serverLevel.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOptional(Identifier.fromNamespaceAndPath("minecraft", "end_city"))
                .orElse(null);
        if (endCity == null) return;

        StructureStart start = serverLevel.structureManager().getStructureAt(player.blockPosition(), endCity);
        if (start.isValid()) {
            ClassActionTrigger.INSTANCE.trigger(player, "enter_end_city", data.getClassLevel(),
                    data.getCurrentClassId().toString());
        }
    }

    public void syncToClient(ServerPlayer player) {
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
        Map<Identifier, com.mod.archetype.network.SyncClassDataPacket.CooldownEntry> cooldownEntries = new HashMap<>();
        for (var entry : data.getCooldowns().entrySet()) {
            cooldownEntries.put(entry.getKey(),
                    new com.mod.archetype.network.SyncClassDataPacket.CooldownEntry(entry.getValue(), entry.getValue()));
        }

        // Build charge entries from abilities that manage their own charges
        Map<Identifier, com.mod.archetype.network.SyncClassDataPacket.ChargeEntry> chargeEntries = new HashMap<>();
        ActiveClassInstance inst = activeInstances.get(player.getUUID());
        if (inst != null) {
            for (var abilityEntry : inst.getActiveAbilities().entrySet()) {
                ActiveAbility ability = abilityEntry.getValue();
                if (ability.managesCooldown()) {
                    int charges = ability.getCharges(player);
                    int maxCharges = ability.getMaxCharges(player);
                    if (charges >= 0 && maxCharges >= 0) {
                        Identifier abilityId = Identifier.fromNamespaceAndPath(ability.getType().getNamespace(), abilityEntry.getKey());
                        chargeEntries.put(abilityId, new com.mod.archetype.network.SyncClassDataPacket.ChargeEntry(charges, maxCharges));
                    }
                }
            }
        }

        NetworkHandler.INSTANCE.sendToPlayer(player,
                new com.mod.archetype.network.SyncClassDataPacket(
                        true, data.getCurrentClassId(),
                        data.getClassLevel(), data.getClassExperience(),
                        data.getResourceCurrent(), resourceMax,
                        cooldownEntries, new HashMap<>(data.getToggleStates()),
                        chargeEntries));
    }

    private static Identifier generateModifierId(Identifier attribute) {
        return Identifier.fromNamespaceAndPath("archetype", "class_" + attribute.getNamespace() + "_" + attribute.getPath());
    }

    private static Identifier generateConditionalModifierId(Identifier attribute, int condIndex) {
        return Identifier.fromNamespaceAndPath("archetype", "cond_" + condIndex + "_" + attribute.getNamespace() + "_" + attribute.getPath());
    }

    // --- Command triggers ---

    private void executeCommands(ServerPlayer player, PlayerClass classDef, String trigger) {
        for (PlayerClass.CommandTrigger cmd : classDef.getCommands()) {
            if (!cmd.trigger().equals(trigger)) continue;
            runCommand(player, cmd.command(), -1);
        }
    }

    private void executeCommands(ServerPlayer player, PlayerClass classDef, String trigger, int level) {
        for (PlayerClass.CommandTrigger cmd : classDef.getCommands()) {
            if (!cmd.trigger().equals(trigger)) continue;
            runCommand(player, cmd.command(), level);
        }
    }

    private void executeTickCommands(ServerPlayer player, PlayerClass classDef, int tick) {
        for (PlayerClass.CommandTrigger cmd : classDef.getCommands()) {
            if (!"on_tick".equals(cmd.trigger())) continue;
            if (tick % cmd.interval() != 0) continue;
            runCommand(player, cmd.command(), -1);
        }
    }

    private void runCommand(ServerPlayer player, String rawCommand, int level) {
        String command = rawCommand
                .replace("{player}", player.getName().getString())
                .replace("{level}", level >= 0 ? String.valueOf(level) : String.valueOf(player.experienceLevel));
        try {
            // Run with the player as @s, but with OP-level permissions
            var source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.ADMIN).withSuppressedOutput();
            player.level().getServer().getCommands().performPrefixedCommand(source, command);
        } catch (Exception e) {
            Archetype.LOGGER.error("Command trigger failed for class {}: '{}' — {}", player.getName().getString(), command, e.getMessage());
        }
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
