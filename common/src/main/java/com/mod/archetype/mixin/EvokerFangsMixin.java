package com.mod.archetype.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EvokerFangs.class)
public class EvokerFangsMixin {

    @Redirect(
        method = "dealDamage",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z")
    )
    private boolean archetype_redirectDamage(LivingEntity target, DamageSource source, float originalDamage) {
        EvokerFangs self = (EvokerFangs) (Object) this;
        for (String tag : self.getTags()) {
            if (tag.startsWith("archetype_dmg:")) {
                float damage = Float.parseFloat(tag.substring("archetype_dmg:".length()));
                return target.hurt(source, damage);
            }
        }
        return target.hurt(source, originalDamage);
    }
}
