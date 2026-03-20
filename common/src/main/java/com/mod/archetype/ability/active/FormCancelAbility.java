package com.mod.archetype.ability.active;

import com.mod.archetype.ability.AbstractActiveAbility;
import com.mod.archetype.ability.ActiveAbility;
import com.mod.archetype.ability.ActivationResult;
import com.mod.archetype.core.ActiveClassInstance;
import com.mod.archetype.core.ClassManager;
import com.mod.archetype.core.PlayerClass.ActiveAbilityEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

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

        formShift.forceDeactivate(player);
        return ActivationResult.SUCCESS;
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
    public ResourceLocation getType() {
        return new ResourceLocation("archetype", "form_cancel");
    }
}
