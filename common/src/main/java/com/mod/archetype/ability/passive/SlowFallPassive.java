package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SlowFallPassive extends AbstractPassiveAbility {

    private final float fallSpeed;

    public SlowFallPassive(PassiveAbilityEntry entry) {
        super(entry);
        this.fallSpeed = getFloat("fall_speed", 0.05f);
    }

    @Override
    public void tick(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        Vec3 motion = player.getDeltaMovement();
        if (motion.y < -fallSpeed) {
            player.setDeltaMovement(motion.x, -fallSpeed, motion.z);
            player.fallDistance = 0.0f;
        }
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath(Archetype.MOD_ID, "slow_fall");
    }
}
