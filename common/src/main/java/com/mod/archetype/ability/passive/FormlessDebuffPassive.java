package com.mod.archetype.ability.passive;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.active.FormShiftAbility;
import com.mod.archetype.core.ActiveClassInstance;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class FormlessDebuffPassive extends AbstractPassiveAbility {

    private final List<EffectDef> debuffEffects;

    public FormlessDebuffPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.debuffEffects = parseEffects();
    }

    private List<EffectDef> parseEffects() {
        List<EffectDef> result = new ArrayList<>();
        if (params.has("effects") && params.get("effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("effects");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                var effectHolder = BuiltInRegistries.MOB_EFFECT.get(
                        Identifier.parse(obj.get("effect").getAsString()));
                int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                effectHolder.ifPresent(effect -> result.add(new EffectDef(effect, amplifier)));
            }
        }
        return result;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        FormShiftAbility formShift = getFormShiftAbility(player);
        boolean hasForm = formShift != null && formShift.isFormActive();

        if (!hasForm && player.tickCount % 40 == 0) {
            for (EffectDef e : debuffEffects) {
                player.addEffect(new MobEffectInstance(
                        e.effect, 100, e.amplifier, true, false, false));
            }
        }
    }

    @Override
    public void onPlayerAttack(ServerPlayer player, Entity target, DamageSource source) {
        FormShiftAbility formShift = getFormShiftAbility(player);
        if (formShift != null && formShift.isFormActive()) {
            formShift.handleOnHit(player, target);
        }
    }

    @Override
    public void onRemove(ServerPlayer player) {
        for (EffectDef e : debuffEffects) {
            player.removeEffect(e.effect);
        }
    }

    private FormShiftAbility getFormShiftAbility(ServerPlayer player) {
        ActiveClassInstance instance = ClassManager.getInstance().getInstance(player);
        if (instance == null) return null;

        for (ActiveAbility ability : instance.getActiveAbilities().values()) {
            if (ability instanceof FormShiftAbility formShift) {
                return formShift;
            }
        }
        return null;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "formless_debuff");
    }

    private record EffectDef(Holder<MobEffect> effect, int amplifier) {}
}
