package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TeleportAbility extends AbstractActiveAbility {

    private final float range;
    private final boolean requireSafeLanding;
    private final boolean enderpearlDamage;

    public TeleportAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.range = getFloat("range", 30.0f);
        this.requireSafeLanding = getBool("require_safe_landing", true);
        this.enderpearlDamage = getBool("enderpearl_damage", false);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (!canActivate(player)) return ActivationResult.FAILED;
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eyePos, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return ActivationResult.FAILED;
        }
        BlockPos targetPos = hit.getBlockPos().relative(hit.getDirection());
        if (requireSafeLanding) {
            if (!player.level().getBlockState(targetPos).isAir()
                    || !player.level().getBlockState(targetPos.above()).isAir()) {
                return ActivationResult.FAILED;
            }
        }
        player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        if (enderpearlDamage) {
            player.hurt(player.damageSources().fall(), 1.0f);
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "teleport");
    }
}
