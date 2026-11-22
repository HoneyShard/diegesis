package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.focus.FocusController;

/**
 * No hands at all once they have gone down ({@code FirstPersonHandsMixin}). This is the method the game and a shader pack
 * under Iris both draw them through.
 */
@Mixin(ItemInHandRenderer.class)
abstract class FirstPersonHandsRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void diegesis$noHandsAtStation(float frameInterp, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            LocalPlayer player, int lightCoords, CallbackInfo ci) {
        if (FocusController.get().isHandGone()) {
            ci.cancel();
        }
    }
}
