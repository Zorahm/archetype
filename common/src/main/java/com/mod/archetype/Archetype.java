package com.mod.archetype;

import com.mod.archetype.ability.AbilityRegistry;
import com.mod.archetype.advancement.ClassActionTrigger;
import com.mod.archetype.condition.ConditionRegistry;
import com.mod.archetype.network.NetworkInit;
import com.mod.archetype.platform.NetworkHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Archetype {

    public static final String MOD_ID = "archetype";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Archetype initializing...");

        // Register built-in condition types
        ConditionRegistry.getInstance().registerBuiltins();

        // Register built-in ability types
        AbilityRegistry.getInstance().registerBuiltins();

        // Register advancement trigger
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, ClassActionTrigger.ID, ClassActionTrigger.INSTANCE);

        // Register network packets
        NetworkHandler network = NetworkHandler.INSTANCE;
        network.init();
        NetworkInit.register(network);

        LOGGER.info("Archetype initialized.");
    }

    public static void initClient() {
        LOGGER.info("Archetype client initializing...");
    }
}
