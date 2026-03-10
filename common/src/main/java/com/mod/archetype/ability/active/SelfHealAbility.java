package com.mod.archetype.ability.active;

import com.google.gson.JsonArray;
import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

public class SelfHealAbility extends AbstractActiveAbility {

    private final float amount;
    private final float percent;
    private final List<ResourceLocation> removeEffects;

    public SelfHealAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.amount = getFloat("amount", 0f);
        this.percent = getFloat("percent", 0.3f);
        this.removeEffects = new ArrayList<>();
        if (params.has("remove_effects") && params.get("remove_effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("remove_effects");
            for (int i = 0; i < arr.size(); i++) {
                removeEffects.add(new ResourceLocation(arr.get(i).getAsString()));
            }
        }
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        float healAmount = amount > 0 ? amount : player.getMaxHealth() * percent;
        player.heal(healAmount);
        for (ResourceLocation effectId : removeEffects) {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectId);
            if (effect != null) player.removeEffect(effect);
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "self_heal");
    }
}
