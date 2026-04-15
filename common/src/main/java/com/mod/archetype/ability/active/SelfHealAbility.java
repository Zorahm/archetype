package com.mod.archetype.ability.active;

import com.google.gson.JsonArray;
import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

public class SelfHealAbility extends AbstractActiveAbility {

    private final float amount;
    private final float percent;
    private final List<Identifier> removeEffects;

    public SelfHealAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.amount = getFloat("amount", 0f);
        this.percent = getFloat("percent", 0.3f);
        this.removeEffects = new ArrayList<>();
        if (params.has("remove_effects") && params.get("remove_effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("remove_effects");
            for (int i = 0; i < arr.size(); i++) {
                removeEffects.add(Identifier.parse(arr.get(i).getAsString()));
            }
        }
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        float healAmount = amount > 0 ? amount : player.getMaxHealth() * percent;
        player.heal(healAmount);
        for (Identifier effectId : removeEffects) {
            BuiltInRegistries.MOB_EFFECT.get(effectId).ifPresent(player::removeEffect);
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "self_heal");
    }
}
