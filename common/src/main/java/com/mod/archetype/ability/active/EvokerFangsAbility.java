package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        int extra = level / 10; // +1 per 10 levels
        int total = baseFangs + extra;
        int maxFangs = getInt("max_fangs", mode == 3 ? 12 : 9);
        return Math.min(total, maxFangs);
    }

    private float getDamage(ServerPlayer player) {
        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        float base = getFloat("damage", 2.0f);
        int bonus = (level >= 20 ? 1 : 0) + (level >= 40 ? 1 : 0);
        return base + bonus;
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        int count = getFangCount(player);
        float damage = getDamage(player);

        switch (mode) {
            case 1 -> spawnFangsLine(player, count, damage);
            case 2 -> spawnFangsTargeted(player, count, damage);
            case 3 -> spawnFangsAround(player, count, damage);
            default -> spawnFangsLine(player, count, damage);
        }

        return ActivationResult.SUCCESS;
    }

    private void spawnFangsLine(ServerPlayer player, int count, float damage) {
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0, look.z).normalize();
        Vec3 start = player.position().add(flatLook.scale(1.5));

        for (int i = 0; i < count; i++) {
            Vec3 pos = start.add(flatLook.scale(i * 1.2));
            BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);
            // Find ground level
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                EvokerFangs fangs = new EvokerFangs(player.level(), pos.x, ground.getY(),
                        pos.z, 0, i * 2, player);
                FangsDamageRegistry.DAMAGE.put(fangs, damage);
                player.level().addFreshEntity(fangs);
            }
        }
    }

    private void spawnFangsTargeted(ServerPlayer player, int count, float damage) {
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

        // Always spawn one fang at center
        BlockPos centerGround = findGround(player, targetPos);
        if (centerGround != null) {
            EvokerFangs centerFangs = new EvokerFangs(player.level(), cx, centerGround.getY(), cz, 0, 0, player);
            FangsDamageRegistry.DAMAGE.put(centerFangs, damage);
            player.level().addFreshEntity(centerFangs);
        }

        if (count <= 1) return;

        // Remaining fangs in a circle around center
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
                EvokerFangs fangs = new EvokerFangs(player.level(), x, ground.getY(), z,
                        (float) angle, i + 1, player);
                FangsDamageRegistry.DAMAGE.put(fangs, damage);
                player.level().addFreshEntity(fangs);
            }
        }
    }

    private void spawnFangsAround(ServerPlayer player, int count, float damage) {
        double angleStep = 2 * Math.PI / count;
        double radius = 2.0;

        for (int i = 0; i < count; i++) {
            double angle = angleStep * i;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            BlockPos blockPos = BlockPos.containing(x, player.getY(), z);
            BlockPos ground = findGround(player, blockPos);
            if (ground != null) {
                EvokerFangs fangs = new EvokerFangs(player.level(), x, ground.getY(),
                        z, (float) angle, i, player);
                FangsDamageRegistry.DAMAGE.put(fangs, damage);
                player.level().addFreshEntity(fangs);
            }
        }
    }

    private BlockPos findGround(ServerPlayer player, BlockPos pos) {
        // Search 3 blocks up and down for ground
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
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "evoker_fangs");
    }
}
