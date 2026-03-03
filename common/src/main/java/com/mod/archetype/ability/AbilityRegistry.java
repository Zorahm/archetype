package com.mod.archetype.ability;

import com.mod.archetype.Archetype;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AbilityRegistry {

    private static final AbilityRegistry INSTANCE = new AbilityRegistry();

    private final Map<ResourceLocation, PassiveAbilityFactory> passiveFactories = new HashMap<>();
    private final Map<ResourceLocation, ActiveAbilityFactory> activeFactories = new HashMap<>();

    public static AbilityRegistry getInstance() {
        return INSTANCE;
    }

    public void registerPassive(ResourceLocation type, PassiveAbilityFactory factory) {
        passiveFactories.put(type, factory);
    }

    public void registerActive(ResourceLocation type, ActiveAbilityFactory factory) {
        activeFactories.put(type, factory);
    }

    public PassiveAbility createPassive(PassiveAbilityEntry entry) {
        PassiveAbilityFactory factory = passiveFactories.get(entry.type());
        if (factory == null) {
            Archetype.LOGGER.error("Unknown passive ability type: {}", entry.type());
            return null;
        }
        return factory.create(entry.params());
    }

    public ActiveAbility createActive(ActiveAbilityEntry entry) {
        ActiveAbilityFactory factory = activeFactories.get(entry.type());
        if (factory == null) {
            Archetype.LOGGER.error("Unknown active ability type: {}", entry.type());
            return null;
        }
        return factory.create(entry);
    }

    public boolean hasPassiveFactory(ResourceLocation type) {
        return passiveFactories.containsKey(type);
    }

    public boolean hasActiveFactory(ResourceLocation type) {
        return activeFactories.containsKey(type);
    }

    public void registerBuiltins() {
        // Built-in ability types will be registered here by future prompts
        // e.g.:
        // registerPassive(new ResourceLocation(Archetype.MOD_ID, "sun_damage"), SunDamagePassive::new);
        // registerActive(new ResourceLocation(Archetype.MOD_ID, "timed_buff"), TimedBuffAbility::new);
    }
}
