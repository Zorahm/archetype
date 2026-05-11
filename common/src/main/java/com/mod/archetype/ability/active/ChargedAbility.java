package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ChargedAbility extends AbstractActiveAbility {

    private final int maxChargeTicks;
    private final float baseDamage;
    private final float damageMultiplier;
    private final float radius;
    private final float range;
    private int chargeTicks;

    public ChargedAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.maxChargeTicks = getInt("max_charge_ticks", 60);
        this.baseDamage = getFloat("base_damage", 4.0f);
        this.damageMultiplier = getFloat("damage_multiplier", 3.0f);
        this.radius = getFloat("radius", 0f);
        this.range = getFloat("range", 5.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        active = true;
        chargeTicks = 0;
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        if (chargeTicks < maxChargeTicks) {
            chargeTicks++;
        }
    }

    @Override
    public void onRelease(ServerPlayer player, int chargeLevel) {
        float chargePercent = (float) chargeTicks / maxChargeTicks;
        float totalDamage = baseDamage * (1 + chargePercent * damageMultiplier);

        if (radius > 0) {
            AABB area = player.getBoundingBox().inflate(radius);
            List<LivingEntity> targets = player.level().getEntitiesOfClass(
                    LivingEntity.class, area, e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), totalDamage);
            }
        } else {
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            Vec3 endPos = eyePos.add(lookVec.scale(range));
            AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    player, eyePos, endPos, searchBox,
                    e -> e instanceof LivingEntity && e.isAlive() && e != player, range * range);
            if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                target.hurt(player.damageSources().playerAttack(player), totalDamage);
            }
        }
        active = false;
        chargeTicks = 0;
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        chargeTicks = 0;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "charged");
    }
}
