package com.mod.archetype.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerFangs.class)
public class EvokerFangsMixin {

    @Inject(method = "dealDamage", at = @At("HEAD"))
    private void archetype_applyResistance(LivingEntity target, CallbackInfo ci) {
        EvokerFangs self = (EvokerFangs) (Object) this;
        for (String tag : self.getTags()) {
            if (tag.startsWith("archetype_resistance:")) {
                int amplifier = Integer.parseInt(tag.substring("archetype_resistance:".length()));
                target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1, amplifier, false, false));
                return;
            }
        }
    }
}
