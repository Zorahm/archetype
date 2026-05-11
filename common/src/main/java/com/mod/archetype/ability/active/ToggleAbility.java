package com.mod.archetype.ability.active;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class ToggleAbility extends AbstractActiveAbility {

    private final List<EffectDef> effects;
    private final float drainPerSecond;

    public ToggleAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.drainPerSecond = getFloat("drain_per_second", 0f);
        this.effects = parseEffects();
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (active) {
            forceDeactivate(player);
            return ActivationResult.SUCCESS;
        }
        if (!canActivate(player)) return ActivationResult.FAILED;
        active = true;
        applyEffects(player);
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (player.tickCount % 40 == 0) {
            applyEffects(player);
        }
        if (drainPerSecond > 0) {
            PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
            float drain = drainPerSecond / 20f;
            data.setResourceCurrent(data.getResourceCurrent() - drain);
            if (data.getResourceCurrent() <= 0) {
                data.setResourceCurrent(0);
                forceDeactivate(player);
            }
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        for (EffectDef e : effects) {
            player.removeEffect(e.effect);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "toggle");
    }

    private void applyEffects(ServerPlayer player) {
        for (EffectDef e : effects) {
            player.addEffect(new MobEffectInstance(e.effect, 100, e.amplifier, true, false, true));
        }
    }

    private List<EffectDef> parseEffects() {
        List<EffectDef> result = new ArrayList<>();
        if (params.has("effects") && params.get("effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("effects");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                var effectHolder = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(obj.get("effect").getAsString()));
                int amp = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                effectHolder.ifPresent(effect -> result.add(new EffectDef(effect, amp)));
            }
        }
        return result;
    }

    private record EffectDef(Holder<MobEffect> effect, int amplifier) {}
}
