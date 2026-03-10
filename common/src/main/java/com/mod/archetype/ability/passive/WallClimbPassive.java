package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class WallClimbPassive extends AbstractPassiveAbility {

    private final float climbSpeed;

    public WallClimbPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.climbSpeed = getFloat("climb_speed", 0.2f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        if (player.horizontalCollision && player.isShiftKeyDown()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, climbSpeed, motion.z);
            player.fallDistance = 0.0f;
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "wall_climb");
    }
}
