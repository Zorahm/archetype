package com.mod.archetype.entity;

import net.minecraft.nbt.CompoundTag;
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
        super((EntityType<? extends ThrowableProjectile>) EntityType.SNOWBALL, level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target) {
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
        if (!level().isClientSide) {
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
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putFloat("ExplosionRadius", explosionRadius);
        tag.putInt("Pierce", pierce);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        explosionRadius = tag.getFloat("ExplosionRadius");
        pierce = tag.getInt("Pierce");
    }
}
