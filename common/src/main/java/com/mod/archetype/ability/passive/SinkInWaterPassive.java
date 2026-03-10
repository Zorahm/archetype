package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SinkInWaterPassive extends AbstractPassiveAbility {

    private final boolean cannotSwim;
    private final float walkSpeedUnderwater;

    public SinkInWaterPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.cannotSwim = getBool("cannot_swim", true);
        this.walkSpeedUnderwater = getFloat("walk_speed_underwater", 0.8f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        if (player.isInWater() && cannotSwim) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(
                    motion.x * walkSpeedUnderwater,
                    -0.04,
                    motion.z * walkSpeedUnderwater
            );
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "sink_in_water");
    }
}
