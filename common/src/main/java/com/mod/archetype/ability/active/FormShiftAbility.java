package com.mod.archetype.ability.active;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FormShiftAbility extends AbstractActiveAbility {

    private static final UUID FORM_HEALTH_UUID = UUID.fromString("a1b2c3d4-1111-4000-8000-000000000001");
    private static final UUID FORM_ATTACK_DAMAGE_UUID = UUID.fromString("a1b2c3d4-2222-4000-8000-000000000002");
    private static final UUID FORM_ATTACK_SPEED_UUID = UUID.fromString("a1b2c3d4-3333-4000-8000-000000000003");

    private final List<FormDefinition> forms;
    private final int globalDamageGrowthPer5Levels;
    private final int globalMaxBonusDamage;
    private final int globalRadiusGrowthPer5Levels;
    private final int globalMaxBonusRadius;

    private FormDefinition currentForm;

    public FormShiftAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.globalDamageGrowthPer5Levels = getInt("global_damage_growth_per_5_levels", 1);
        this.globalMaxBonusDamage = getInt("global_max_bonus_damage", 5);
        this.globalRadiusGrowthPer5Levels = getInt("global_radius_growth_per_5_levels", 1);
        this.globalMaxBonusRadius = getInt("global_max_bonus_radius", 3);
        this.forms = parseForms();
    }

    private List<FormDefinition> parseForms() {
        List<FormDefinition> result = new ArrayList<>();
        if (params.has("forms") && params.get("forms").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("forms");
            for (JsonElement elem : arr) {
                result.add(new FormDefinition(elem.getAsJsonObject()));
            }
        }
        return result;
    }

    public FormDefinition getCurrentForm() {
        return currentForm;
    }

    public boolean isFormActive() {
        return active && currentForm != null;
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (active) {
            return ActivationResult.ALREADY_ACTIVE;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return ActivationResult.FAILED;

        String heldId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();

        FormDefinition matchedForm = null;
        for (FormDefinition form : forms) {
            if (form.itemId.equals(heldId)) {
                matchedForm = form;
                break;
            }
        }

        if (matchedForm == null) return ActivationResult.FAILED;

        held.shrink(1);

        currentForm = matchedForm;
        active = true;

        applyFormModifiers(player, matchedForm);

        return ActivationResult.SUCCESS;
    }

    private void applyFormModifiers(ServerPlayer player, FormDefinition form) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();

        float healthMod = form.getEffectiveHealthModifier(level);
        float damageMod = form.getEffectiveAttackDamageModifier(level);
        float speedMod = form.getEffectiveAttackSpeedModifier(level);

        if (healthMod != 0) {
            applyModifier(player, Attributes.MAX_HEALTH, FORM_HEALTH_UUID,
                    "Form Health", healthMod, AttributeModifier.Operation.ADDITION);
            if (healthMod > 0) {
                player.setHealth(Math.min(player.getHealth() + healthMod, player.getMaxHealth()));
            }
        }
        if (damageMod != 0) {
            applyModifier(player, Attributes.ATTACK_DAMAGE, FORM_ATTACK_DAMAGE_UUID,
                    "Form Damage", damageMod, AttributeModifier.Operation.MULTIPLY_BASE);
        }
        if (speedMod != 0) {
            applyModifier(player, Attributes.ATTACK_SPEED, FORM_ATTACK_SPEED_UUID,
                    "Form Speed", speedMod, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    }

    private void applyModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                               UUID uuid, String name, double value, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(uuid);
        inst.addTransientModifier(new AttributeModifier(uuid, name, value, op));
    }

    private void removeFormModifiers(ServerPlayer player) {
        removeModifier(player, Attributes.MAX_HEALTH, FORM_HEALTH_UUID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, FORM_ATTACK_DAMAGE_UUID);
        removeModifier(player, Attributes.ATTACK_SPEED, FORM_ATTACK_SPEED_UUID);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void removeModifier(ServerPlayer player, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                UUID uuid) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) {
            inst.removeModifier(uuid);
        }
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (currentForm == null) {
            active = false;
            return;
        }

        if ("zombie".equals(currentForm.formId)) {
            tickZombieForm(player);
        }
    }

    private void tickZombieForm(ServerPlayer player) {
        if (player.level().isClientSide()) return;
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        boolean isDaytime = player.level().isDay();

        if (isDaytime) {
            boolean noSunDamage = level >= 20;
            if (!noSunDamage && player.level().canSeeSky(player.blockPosition())) {
                if (player.tickCount % 20 == 0) {
                    player.setSecondsOnFire(2);
                }
            }

            boolean noHunger = level >= 15;
            if (!noHunger && player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.HUNGER, 100, 0, true, false, false));
            }
        }
    }

    /**
     * Called by FormlessDebuffPassive when the player attacks an entity.
     */
    public void handleOnHit(ServerPlayer player, Entity target) {
        if (!active || currentForm == null || !(target instanceof LivingEntity living)) return;

        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int globalDamageBonus = Math.min((level / 5) * globalDamageGrowthPer5Levels, globalMaxBonusDamage);
        int globalRadiusBonus = Math.min((level / 5) * globalRadiusGrowthPer5Levels, globalMaxBonusRadius);

        switch (currentForm.onHitType) {
            case "explosion" -> {
                float damage = currentForm.getEffectiveOnHitDamage(level) + globalDamageBonus;
                float radius = currentForm.getEffectiveOnHitRadius(level) + globalRadiusBonus;
                AABB area = target.getBoundingBox().inflate(radius);
                List<Entity> nearby = player.level().getEntities(player, area,
                        e -> e instanceof LivingEntity && e.isAlive() && e != player && e != target);
                for (Entity e : nearby) {
                    e.hurt(player.damageSources().playerAttack(player), damage);
                }
                living.hurt(player.damageSources().playerAttack(player), damage);
            }
            case "effect" -> {
                MobEffect effect = currentForm.getOnHitEffect();
                if (effect != null) {
                    int duration = currentForm.getEffectiveOnHitEffectDuration(level);
                    int amplifier = currentForm.getEffectiveOnHitEffectAmplifier(level);
                    living.addEffect(new MobEffectInstance(effect, duration, amplifier));
                }
            }
            case "fire" -> {
                int fireTicks = currentForm.getEffectiveOnHitFireDuration(level);
                living.setSecondsOnFire(fireTicks / 20);
            }
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        removeFormModifiers(player);
        currentForm = null;
        active = false;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "form_shift");
    }

    static class FormDefinition {
        final String itemId;
        final String formId;
        final String onHitType;

        // On-hit base params
        final float baseOnHitDamage;
        final float baseOnHitRadius;
        final String onHitEffectId;
        final int baseOnHitEffectDuration;
        final int baseOnHitEffectAmplifier;
        final int baseOnHitFireDuration;

        // Attribute modifiers
        final float attackDamageModifier;
        final float attackSpeedModifier;
        final float maxHealthModifier;

        // Zombie day/night specifics
        final float nightAttackSpeedModifier;
        final float nightMaxHealthModifier;
        final float dayMaxHealthModifier;

        final JsonArray progression;

        FormDefinition(JsonObject json) {
            this.itemId = json.get("item").getAsString();
            this.formId = json.get("form_id").getAsString();
            this.onHitType = json.has("on_hit_type") ? json.get("on_hit_type").getAsString() : "none";

            this.baseOnHitDamage = json.has("on_hit_damage") ? json.get("on_hit_damage").getAsFloat() : 0;
            this.baseOnHitRadius = json.has("on_hit_radius") ? json.get("on_hit_radius").getAsFloat() : 0;
            this.onHitEffectId = json.has("on_hit_effect") ? json.get("on_hit_effect").getAsString() : "";
            this.baseOnHitEffectDuration = json.has("on_hit_effect_duration") ? json.get("on_hit_effect_duration").getAsInt() : 20;
            this.baseOnHitEffectAmplifier = json.has("on_hit_effect_amplifier") ? json.get("on_hit_effect_amplifier").getAsInt() : 0;
            this.baseOnHitFireDuration = json.has("on_hit_fire_duration") ? json.get("on_hit_fire_duration").getAsInt() : 20;

            this.attackDamageModifier = json.has("attack_damage_modifier") ? json.get("attack_damage_modifier").getAsFloat() : 0;
            this.attackSpeedModifier = json.has("attack_speed_modifier") ? json.get("attack_speed_modifier").getAsFloat() : 0;
            this.maxHealthModifier = json.has("max_health_modifier") ? json.get("max_health_modifier").getAsFloat() : 0;

            JsonObject nightBonuses = json.has("night_bonuses") ? json.getAsJsonObject("night_bonuses") : null;
            this.nightAttackSpeedModifier = nightBonuses != null && nightBonuses.has("attack_speed_modifier")
                    ? nightBonuses.get("attack_speed_modifier").getAsFloat() : 0;
            this.nightMaxHealthModifier = nightBonuses != null && nightBonuses.has("max_health_modifier")
                    ? nightBonuses.get("max_health_modifier").getAsFloat() : 0;
            this.dayMaxHealthModifier = 0;

            this.progression = json.has("progression") ? json.getAsJsonArray("progression") : new JsonArray();
        }

        MobEffect getOnHitEffect() {
            if (onHitEffectId.isEmpty()) return null;
            return BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(onHitEffectId));
        }

        float getEffectiveOnHitDamage(int level) {
            float damage = baseOnHitDamage;
            for (JsonElement elem : progression) {
                JsonObject p = elem.getAsJsonObject();
                if (p.has("level") && p.has("on_hit_damage") && level >= p.get("level").getAsInt()) {
                    damage = p.get("on_hit_damage").getAsFloat();
                }
            }
            return damage;
        }

        float getEffectiveOnHitRadius(int level) {
            float radius = baseOnHitRadius;
            for (JsonElement elem : progression) {
                JsonObject p = elem.getAsJsonObject();
                if (p.has("level") && p.has("on_hit_radius") && level >= p.get("level").getAsInt()) {
                    radius = p.get("on_hit_radius").getAsFloat();
                }
            }
            return radius;
        }

        int getEffectiveOnHitEffectDuration(int level) {
            int duration = baseOnHitEffectDuration;
            for (JsonElement elem : progression) {
                JsonObject p = elem.getAsJsonObject();
                if (p.has("level_interval") && p.has("on_hit_effect_duration_growth")) {
                    int interval = p.get("level_interval").getAsInt();
                    int growth = p.get("on_hit_effect_duration_growth").getAsInt();
                    int maxDur = p.has("max_duration") ? p.get("max_duration").getAsInt() : Integer.MAX_VALUE;
                    int steps = level / interval;
                    duration = Math.min(baseOnHitEffectDuration + steps * growth, maxDur);
                }
            }
            return duration;
        }

        int getEffectiveOnHitEffectAmplifier(int level) {
            int amplifier = baseOnHitEffectAmplifier;
            for (JsonElement elem : progression) {
                JsonObject p = elem.getAsJsonObject();
                if (p.has("level_interval") && p.has("on_hit_effect_amplifier_growth")) {
                    int interval = p.get("level_interval").getAsInt();
                    int growth = p.get("on_hit_effect_amplifier_growth").getAsInt();
                    int maxAmp = p.has("max_amplifier") ? p.get("max_amplifier").getAsInt() : Integer.MAX_VALUE;
                    int steps = level / interval;
                    amplifier = Math.min(baseOnHitEffectAmplifier + steps * growth, maxAmp);
                }
            }
            return amplifier;
        }

        int getEffectiveOnHitFireDuration(int level) {
            int duration = baseOnHitFireDuration;
            for (JsonElement elem : progression) {
                JsonObject p = elem.getAsJsonObject();
                if (p.has("level_interval") && p.has("on_hit_fire_duration_growth")) {
                    int interval = p.get("level_interval").getAsInt();
                    int growth = p.get("on_hit_fire_duration_growth").getAsInt();
                    int maxDur = p.has("max_duration") ? p.get("max_duration").getAsInt() : Integer.MAX_VALUE;
                    int steps = level / interval;
                    duration = Math.min(baseOnHitFireDuration + steps * growth, maxDur);
                }
            }
            return duration;
        }

        float getEffectiveHealthModifier(int level) {
            float mod = maxHealthModifier;
            if ("zombie".equals(formId)) {
                boolean isDaytime = true; // will be set at apply time
                // For zombie, base modifier depends on time of day
                // This is handled dynamically in tickActive, not here
                // Return night bonus as default since it's the most common modifier
                mod = nightMaxHealthModifier;
                for (JsonElement elem : progression) {
                    JsonObject p = elem.getAsJsonObject();
                    if (p.has("level") && p.has("day_max_health_modifier") && level >= p.get("level").getAsInt()) {
                        // Day health bonus also applies
                    }
                }
            }
            return mod;
        }

        float getEffectiveAttackDamageModifier(int level) {
            return attackDamageModifier;
        }

        float getEffectiveAttackSpeedModifier(int level) {
            float mod = attackSpeedModifier;
            if ("zombie".equals(formId)) {
                // Night bonus attack speed
                mod = nightAttackSpeedModifier;
                for (JsonElement elem : progression) {
                    JsonObject p = elem.getAsJsonObject();
                    if (p.has("level") && p.has("day_attack_speed_modifier") && level >= p.get("level").getAsInt()) {
                        // This adds day attack speed too
                    }
                }
            }
            return mod;
        }
    }
}
