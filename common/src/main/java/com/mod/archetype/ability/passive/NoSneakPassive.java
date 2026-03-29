package com.mod.archetype.ability.passive;

import com.mod.archetype.Archetype;
import com.mod.archetype.ability.AbstractPassiveAbility;
import com.mod.archetype.core.PlayerClass.PassiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class NoSneakPassive extends AbstractPassiveAbility {
    public NoSneakPassive(PassiveAbilityEntry entry) { super(entry); }

    @Override
    public void tick(ServerPlayer player) {
        if (player.isShiftKeyDown() || player.isCrouching()) {
            player.setShiftKeyDown(false);
            // Force pose out of crouching — setShiftKeyDown alone is not enough
            // because the client re-sends input state every tick
            if (player.getPose() == net.minecraft.world.entity.Pose.CROUCHING) {
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation(Archetype.MOD_ID, "no_sneak");
    }
}
