package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.entity.ArchetypeProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ProjectileAbility extends AbstractActiveAbility {

    private final float speed;
    private final float damage;
    private final boolean gravity;
    private final float explosionRadius;
    private final int pierce;

    public ProjectileAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.speed = getFloat("speed", 1.5f);
        this.damage = getFloat("damage", 8.0f);
        this.gravity = getBool("gravity", false);
        this.explosionRadius = getFloat("explosion_radius", 0f);
        this.pierce = getInt("pierce", 0);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Vec3 look = player.getLookAngle();
        ArchetypeProjectile projectile = new ArchetypeProjectile(player.level(), player);
        projectile.setDamage(damage);
        projectile.setExplosionRadius(explosionRadius);
        projectile.setPierce(pierce);
        projectile.setNoGravity(!gravity);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        projectile.shoot(look.x, look.y, look.z, speed, 0f);
        player.level().addFreshEntity(projectile);
        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "projectile");
    }
}
