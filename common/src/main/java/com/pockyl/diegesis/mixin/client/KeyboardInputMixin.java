package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
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
abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void diegesis$standAtStation(CallbackInfo ci) {
        FocusController focus = FocusController.get();
        if (!focus.isActive() || focus.isExiting()) {
            return;
        }
        keyPresses = Input.EMPTY;
        moveVector = Vec2.ZERO;
    }
}
