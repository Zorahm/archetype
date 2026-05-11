package com.mod.archetype.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class ArchetypeProjectile extends ThrowableProjectile {

    private float damage = 8.0f;
    private float explosionRadius = 0f;
    private int pierce = 0;
    private int pierceCount = 0;

    public ArchetypeProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("unchecked")
    public ArchetypeProjectile(Level level, LivingEntity owner) {
        super((EntityType<? extends ThrowableProjectile>) (EntityType<?>) EntityType.SNOWBALL, level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide() && result.getEntity() instanceof LivingEntity target) {
            LivingEntity owner = getOwner() instanceof LivingEntity le ? le : null;
            target.hurt(
                    owner != null ? damageSources().mobProjectile(this, owner) : damageSources().generic(),
                    damage);
        }
        pierceCount++;
        if (pierceCount > pierce) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide()) {
            if (explosionRadius > 0) {
                level().explode(this, getX(), getY(), getZ(), explosionRadius, Level.ExplosionInteraction.NONE);
            }
            discard();
        }
    }

    public void setDamage(float damage) { this.damage = damage; }
    public float getDamage() { return damage; }
    public void setExplosionRadius(float radius) { this.explosionRadius = radius; }
    public void setPierce(int pierce) { this.pierce = pierce; }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("Damage", damage);
        output.putFloat("ExplosionRadius", explosionRadius);
        output.putInt("Pierce", pierce);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        damage = input.getFloatOr("Damage", 8.0f);
        explosionRadius = input.getFloatOr("ExplosionRadius", 0f);
        pierce = input.getIntOr("Pierce", 0);
    }
}
