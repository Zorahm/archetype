package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import com.mod.archetype.data.PlayerClassData;
import com.mod.archetype.platform.PlayerDataAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MorphAbility extends AbstractActiveAbility {

    private final int durationTicks;
    private final boolean canFly;
    private final float flightSpeed;
    private final float drainPerSecond;
    private int remainingTicks;
    private boolean wasFlying;

    public MorphAbility(ActiveAbilityEntry entry) {
        super(entry);
        this.durationTicks = getInt("duration_ticks", 400);
        this.canFly = getBool("can_fly", false);
        this.flightSpeed = getFloat("flight_speed", 0.05f);
        this.drainPerSecond = getFloat("drain_per_second", 0f);
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        if (active) {
            forceDeactivate(player);
            return ActivationResult.SUCCESS;
        }
        if (!canActivate(player)) return ActivationResult.FAILED;
        active = true;
        remainingTicks = durationTicks;
        wasFlying = player.getAbilities().mayfly;
        if (canFly) {
            player.getAbilities().mayfly = true;
            player.getAbilities().setFlyingSpeed(flightSpeed);
            player.onUpdateAbilities();
        }
        return ActivationResult.SUCCESS;
    }

    @Override
    public void tickActive(ServerPlayer player) {
        remainingTicks--;
        if (drainPerSecond > 0) {
            PlayerClassData data = PlayerDataAccess.INSTANCE.getClassData(player);
            float drain = drainPerSecond / 20f;
            data.setResourceCurrent(data.getResourceCurrent() - drain);
            if (data.getResourceCurrent() <= 0) {
                data.setResourceCurrent(0);
                forceDeactivate(player);
                return;
            }
        }
        if (remainingTicks <= 0) {
            forceDeactivate(player);
        }
    }

    @Override
    public void forceDeactivate(ServerPlayer player) {
        active = false;
        if (canFly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = wasFlying;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    @Override
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "morph");
    }
}
