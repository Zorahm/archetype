package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RandomTeleportAbility extends AbstractActiveAbility {
    private final Random random = new Random();
    private final float range;

    public RandomTeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.range = getFloat("range", 40.0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;

        PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
        int level = data.getClassLevel();
        int levelTier = level / 10;

        int snowballChance = Math.max(0, 70 - levelTier * 10);
        int pearlChance = 25 + levelTier * 10;
        int eggChance = 5;
        int total = snowballChance + pearlChance + eggChance;

        int roll = random.nextInt(total);
        Vec3 look = player.getLookAngle();

        if (roll < snowballChance) {
            // Fail: snowball
            Snowball snowball = new Snowball(player.level(), player);
            snowball.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            snowball.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(snowball);
        } else if (roll < snowballChance + pearlChance) {
            // Success: teleport
            Vec3 eyePos = player.getEyePosition();
            Vec3 endPos = eyePos.add(look.scale(range));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                BlockPos targetPos = hit.getBlockPos().relative(hit.getDirection());
                // Safe landing check
                if (player.level().getBlockState(targetPos).isAir()
                        && player.level().getBlockState(targetPos.above()).isAir()) {
                    player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
                }
            }
        } else {
            // Fail: egg
            ThrownEgg egg = new ThrownEgg(player.level(), player);
            egg.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            egg.shoot(look.x, look.y, look.z, 1.5f, 0f);
            player.level().addFreshEntity(egg);
        }

        return ActivationResult.SUCCESS;
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "random_teleport");
    }
}
