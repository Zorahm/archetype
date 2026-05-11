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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TargetedEffectAbility extends AbstractActiveAbility {

    private final float range;
    private final float damage;
    private final List<EffectDef> effects;

    public TargetedEffectAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.range = getFloat("range", 10.0f);
        this.damage = getFloat("damage", 0f);
        this.effects = parseEffects();
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player, eyePos, endPos, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != player, range * range);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return ActivationResult.FAILED;
        }
        if (damage > 0) {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }
        for (EffectDef e : effects) {
            target.addEffect(new MobEffectInstance(e.effect, e.duration, e.amplifier));
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "targeted_effect");
    }

    private List<EffectDef> parseEffects() {
        List<EffectDef> result = new ArrayList<>();
        if (params.has("effects") && params.get("effects").isJsonArray()) {
            JsonArray arr = params.getAsJsonArray("effects");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject obj = arr.get(i).getAsJsonObject();
                var effectHolder = BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(obj.get("effect").getAsString()));
                int dur = obj.has("duration") ? obj.get("duration").getAsInt() : 200;
                int amp = obj.has("amplifier") ? obj.get("amplifier").getAsInt() : 0;
                effectHolder.ifPresent(effect -> result.add(new EffectDef(effect, dur, amp)));
            }
        }
        return result;
    }

    private record EffectDef(Holder<MobEffect> effect, int duration, int amplifier) {}
}
