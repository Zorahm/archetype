package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EvokerFangsAbility extends AbstractActiveAbility {

    private final int mode; // 1=line, 2=targeted, 3=around self
    private final int baseFangs;

    public EvokerFangsAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.mode = getInt("mode", 1);
        this.baseFangs = getInt("base_fangs", 3);
    }

    private int getFangCount(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int extra = level / 10;
        int total = baseFangs + extra;
        int maxFangs = getInt("max_fangs", mode == 3 ? 12 : 9);
        return Math.min(total, maxFangs);
    }

    // Resistance I–IV on Circle activation, duration grows every 20 levels
    private void applyCircleResistance(ServerPlayer player) {
        int level = PlayerDataAccess.INSTANCE.getClassData(player).getClassLevel();
        int amplifier = 0;
        if (level >= 10) amplifier = 1;
        if (level >= 30) amplifier = 2;
        if (level >= 50) amplifier = 3;
        int durationTicks = 60; // 3s
        if (level >= 20) durationTicks += 20;
        if (level >= 40) durationTicks += 20;
        if (level >= 60) durationTicks += 20;
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, durationTicks, amplifier, false, true, true));
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        int count = getFangCount(player);
        switch (mode) {
            case 1 -> spawnFangsLine(player, count);
            case 2 -> spawnFangsTargeted(player, count);
            case 3 -> {
                spawnFangsAround(player, count);
                applyCircleResistance(player);
            }
            default -> spawnFangsLine(player, count);
        }
        return ActivationResult.SUCCESS;
    }

    private void spawnFangsLine(ServerPlayer player, int count) {
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
        Vec3 start = player.position().add(flatLook.scale(1.5));
        for (int i = 0; i < count; i++) {
            Vec3 pos = start.add(flatLook.scale(i * 1.2));
            BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(
                        new EvokerFangs(player.level(), pos.x, ground.getY(), pos.z, 0, i * 2, player));
            }
        }
    }

    private void spawnFangsTargeted(ServerPlayer player, int count) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(20));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return;
        BlockPos targetPos = hit.getBlockPos().relative(hit.getDirection());
        double cx = targetPos.getX() + 0.5;
        double cy = targetPos.getY();
        double cz = targetPos.getZ() + 0.5;
        BlockPos centerGround = findGround(player, targetPos);
        if (centerGround != null) {
            player.level().addFreshEntity(
                    new EvokerFangs(player.level(), cx, centerGround.getY(), cz, 0, 0, player));
        }
        if (count <= 1) return;
        int circleCount = count - 1;
        double angleStep = 2 * Math.PI / circleCount;
        double radius = 1.5;
        for (int i = 0; i < circleCount; i++) {
            double angle = angleStep * i;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            BlockPos blockPos = BlockPos.containing(x, cy, z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(
                        new EvokerFangs(player.level(), x, ground.getY(), z, (float) angle, i + 1, player));
            }
        }
    }

    private void spawnFangsAround(ServerPlayer player, int count) {
        double angleStep = 2 * Math.PI / count;
        double radius = 2.0;
        for (int i = 0; i < count; i++) {
            double angle = angleStep * i;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            BlockPos blockPos = BlockPos.containing(x, player.getY(), z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                player.level().addFreshEntity(
                        new EvokerFangs(player.level(), x, ground.getY(), z, (float) angle, i, player));
            }
        }
    }

    private BlockPos findGround(ServerPlayer player, BlockPos pos) {
        for (int dy = 2; dy >= -2; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            if (!player.level().getBlockState(check).isAir()
                    && player.level().getBlockState(check.above()).isAir()) {
                return check.above();
            }
        }
        return pos;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "evoker_fangs");
    }
}
