package com.mod.archetype.ability;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.passive.BreathUnderwaterPassive;
import com.mod.archetype.ability.passive.CustomDietPassive;
import com.mod.archetype.ability.passive.EffectImmunityPassive;
import com.mod.archetype.ability.passive.FireImmunityPassive;
import com.mod.archetype.ability.passive.FoodRestrictionPassive;
import com.mod.archetype.ability.passive.JumpBoostPassive;
import com.mod.archetype.ability.passive.LifestealPassive;
import com.mod.archetype.ability.passive.MagneticPullPassive;
import com.mod.archetype.ability.passive.MobNeutralPassive;
import com.mod.archetype.ability.passive.NaturalRegenDisabledPassive;
import com.mod.archetype.ability.passive.NightVisionPassive;
import com.mod.archetype.ability.passive.NoFallDamagePassive;
import com.mod.archetype.ability.passive.SinkInWaterPassive;
import com.mod.archetype.ability.passive.SlowFallPassive;
import com.mod.archetype.ability.passive.SunDamagePassive;
import com.mod.archetype.ability.passive.ThornsPassive;
import com.mod.archetype.ability.passive.UndeadTypePassive;
import com.mod.archetype.ability.passive.WallClimbPassive;
import com.mod.archetype.ability.passive.WaterVulnerabilityPassive;
import com.mod.archetype.ability.passive.NoSneakPassive;
import com.mod.archetype.ability.passive.FireAttackPassive;
import com.mod.archetype.ability.passive.DestroyHolyItemsPassive;
import com.mod.archetype.ability.passive.ShieldBlockPassive;
import com.mod.archetype.ability.passive.ToolFragilityPassive;
import com.mod.archetype.ability.passive.RandomEnchantPassive;
import com.mod.archetype.ability.passive.ArrowTransmutePassive;
import com.mod.archetype.ability.passive.PotionCreatePassive;
import com.mod.archetype.ability.active.AreaAttackAbility;
import com.mod.archetype.ability.active.BloodDrainAbility;
import com.mod.archetype.ability.active.ChargedAbility;
import com.mod.archetype.ability.active.DashAbility;
import com.mod.archetype.ability.active.MorphAbility;
import com.mod.archetype.ability.active.ProjectileAbility;
import com.mod.archetype.ability.active.SelfHealAbility;
import com.mod.archetype.ability.active.SummonAbility;
import com.mod.archetype.ability.active.TargetedEffectAbility;
import com.mod.archetype.ability.active.TeleportAbility;
import com.mod.archetype.ability.active.TimedBuffAbility;
import com.mod.archetype.ability.active.ToggleAbility;
import com.mod.archetype.ability.active.ViDashAbility;
import com.mod.archetype.ability.active.RageDashAbility;
import com.mod.archetype.ability.active.RandomProjectileAbility;
import com.mod.archetype.ability.active.RandomTeleportAbility;
import com.mod.archetype.ability.active.SpaceRiftAbility;
import com.mod.archetype.ability.active.EvokerFangsAbility;
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
        return factory.create(entry);
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
        // Passive abilities
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "sun_damage"), SunDamagePassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "night_vision"), NightVisionPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "food_restriction"), FoodRestrictionPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "natural_regeneration_disabled"), NaturalRegenDisabledPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "undead_type"), UndeadTypePassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "water_vulnerability"), WaterVulnerabilityPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "sink_in_water"), SinkInWaterPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "no_fall_damage"), NoFallDamagePassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "fire_immunity"), FireImmunityPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "custom_diet"), CustomDietPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "mob_neutral"), MobNeutralPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "magnetic_pull"), MagneticPullPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "thorns_passive"), ThornsPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "lifesteal"), LifestealPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "wall_climb"), WallClimbPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "slow_fall"), SlowFallPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "jump_boost"), JumpBoostPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "breath_underwater"), BreathUnderwaterPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "effect_immunity"), EffectImmunityPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "no_sneak"), NoSneakPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "fire_attack"), FireAttackPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "destroy_holy_items"), DestroyHolyItemsPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "shield_block"), ShieldBlockPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "tool_fragility"), ToolFragilityPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "random_enchant"), RandomEnchantPassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "arrow_transmute"), ArrowTransmutePassive::new);
        registerPassive(new ResourceLocation(Archetype.MOD_ID, "potion_create"), PotionCreatePassive::new);

        // Active abilities
        registerActive(new ResourceLocation(Archetype.MOD_ID, "timed_buff"), TimedBuffAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "area_attack"), AreaAttackAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "targeted_effect"), TargetedEffectAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "projectile"), ProjectileAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "dash"), DashAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "teleport"), TeleportAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "morph"), MorphAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "toggle"), ToggleAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "summon"), SummonAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "self_heal"), SelfHealAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "charged"), ChargedAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "blood_drain"), BloodDrainAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "vi_dash"), ViDashAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "rage_dash"), RageDashAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "random_projectile"), RandomProjectileAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "random_teleport"), RandomTeleportAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "space_rift"), SpaceRiftAbility::new);
        registerActive(new ResourceLocation(Archetype.MOD_ID, "evoker_fangs"), EvokerFangsAbility::new);
    }
}
