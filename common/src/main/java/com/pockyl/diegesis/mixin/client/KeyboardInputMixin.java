package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.focus.FocusController;

/**
 * Keeps the player standing while they work at a station. Vanilla reads no movement keys behind a screen, but mods that let
 * players walk with a screen open (InvMove) would move them with the keys that go over to a neighbouring station.
 */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void diegesis$standAtStation(boolean slow, float slowFactor, CallbackInfo ci) {
        FocusController focus = FocusController.get();
        if (!focus.isActive() || focus.isExiting()) {
            return;
        }
        KeyboardInput input = (KeyboardInput) (Object) this;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
    }
}
