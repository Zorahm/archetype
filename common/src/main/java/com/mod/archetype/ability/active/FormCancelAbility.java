package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.ActiveClassInstance;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class FormCancelAbility extends AbstractActiveAbility {

    public FormCancelAbility(ActiveAbilityEntry entry) {
        super(entry);
    }

    @Override
    public boolean canActivate(ServerPlayer player) {
        FormShiftAbility formShift = getFormShiftAbility(player);
        return formShift != null && formShift.isActive();
    }

    @Override
    public ActivationResult activate(ServerPlayer player) {
        FormShiftAbility formShift = getFormShiftAbility(player);
        if (formShift == null || !formShift.isActive()) {
            return ActivationResult.FAILED;
        }

        playFormDeathSound(player, formShift);
        formShift.forceDeactivate(player);
        return ActivationResult.SUCCESS;
    }

    private void playFormDeathSound(ServerPlayer player, FormShiftAbility formShift) {
        var sound = switch (formShift.getCurrentForm().formId) {
            case "zombie"          -> SoundEvents.ZOMBIE_DEATH;
            case "creeper"         -> SoundEvents.CREEPER_DEATH;
            case "snowman"         -> SoundEvents.SNOW_GOLEM_DEATH;
            case "blaze"           -> SoundEvents.BLAZE_DEATH;
            case "wither_skeleton" -> SoundEvents.WITHER_SKELETON_DEATH;
            default                -> null;
        };
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private FormShiftAbility getFormShiftAbility(ServerPlayer player) {
        ActiveClassInstance instance = ClassManager.getInstance().getInstance(player);
        if (instance == null) return null;

        for (ActiveAbility ability : instance.getActiveAbilities().values()) {
            if (ability instanceof FormShiftAbility formShift) {
                return formShift;
            }
        }
        return null;
    }

    @Override
    public Identifier getType() {
        return Identifier.fromNamespaceAndPath("archetype", "form_cancel");
    }
}
