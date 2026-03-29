package com.mod.archetype.mixin;

import com.mod.archetype.ability.active.FangsDamageRegistry;
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
        Float customDamage = FangsDamageRegistry.DAMAGE.get(self);
        float dmg = customDamage != null ? customDamage : originalDamage;
        return target.hurt(source, dmg);
    }
}
