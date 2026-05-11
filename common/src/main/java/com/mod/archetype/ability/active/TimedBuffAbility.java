package com.mod.archetype.ability.active;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class TimedBuffAbility extends AbstractActiveAbility {

    private final int durationTicks;
    private final List<EffectEntry> effects;
    private int remainingTicks;

    public TimedBuffAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.durationTicks = getInt("duration_ticks", 200);
        this.effects = parseEffects();
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        active = true;
        remainingTicks = durationTicks;
        for (EffectEntry e : effects) {
            player.addEffect(new MobEffectInstance(e.effect, durationTicks, e.amplifier, false, true, true));
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        remainingTicks--;
        if (remainingTicks <= 0) {
            forceDeactivate(player);
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        for (EffectEntry e : effects) {
            player.removeEffect(e.effect);
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "timed_buff");
    }

    private List<EffectEntry> parseEffects() {
        List<EffectEntry> result = new ArrayList<>();
        if (params.has("effects") && params.get("effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("effects");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                String effectId = obj.get("effect").getAsString();
                int amplifier = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                var effectHolder = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(effectId));
                effectHolder.ifPresent(effect -> result.add(new EffectEntry(effect, amplifier)));
            }
        }
        return result;
    }

    private record EffectEntry(Holder<MobEffect> effect, int amplifier) {}
}
