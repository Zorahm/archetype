package com.mod.archetype.mixin;

import com.mod.archetype.network.client.ClientClassData;
import com.mod.archetype.registry.ClassRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts KeyboardInput.tick() AFTER vanilla reads keyboard state
 * but BEFORE LocalPlayer.aiStep() uses it for movement/edge-sneak.
 *
 * This is the only reliable injection point to fully suppress sneaking,
 * including the "can't fall off block edges" mechanic.
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void archetype_suppressSneak(boolean isSneaking, float sneakSpeedModifier, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ClientClassData data = ClientClassData.getInstance();
        if (!data.hasClass() || data.getClassId() == null) return;

        var cls = ClassRegistry.getInstance().get(data.getClassId()).orElse(null);
        if (cls == null) return;

        boolean hasNoSneak = cls.getPassiveAbilities().stream()
                .anyMatch(p -> p.type().getPath().equals("no_sneak"));

        if (hasNoSneak) {
            // Clear the sneak input BEFORE aiStep() reads it
            // This prevents both the crouching animation AND the block-edge sneak mechanic
            ClientInput input = (ClientInput) (Object) this;
            input.keyPresses = new net.minecraft.world.entity.player.Input(
                    input.keyPresses.forward(), input.keyPresses.backward(),
                    input.keyPresses.left(), input.keyPresses.right(),
                    input.keyPresses.jump(), false, // shift = false
                    input.keyPresses.sprint());
        }
    }
}
