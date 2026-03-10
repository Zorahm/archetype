package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AreaAttackAbility extends AbstractActiveAbility {

    private final float damage;
    private final float radius;
    private final float knockbackStrength;

    public AreaAttackAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.damage = getFloat("damage", 6.0f);
        this.radius = getFloat("radius", 5.0f);
        this.knockbackStrength = getFloat("knockback_strength", 1.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(knockbackStrength);
            target.push(knockback.x, 0.3, knockback.z);
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "area_attack");
    }
}
