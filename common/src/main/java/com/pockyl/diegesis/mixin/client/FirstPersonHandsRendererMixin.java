package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.focus.FocusController;

/**
 * No hands at all once they have gone down ({@code FirstPersonHandsMixin}). {@code GameRendererMixin} stops the game drawing them,
 * but a shader pack under Iris draws them itself, straight through this method, and a sliver of the held item stayed in view at
 * the station.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class FirstPersonHandsRendererMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$noHandsAtStation(float partialTicks, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
            PlayerRenderState playerState, FirstPersonHandsAndItemsRenderState state,
            CallbackInfo ci) {
        if (FocusController.get().isHandGone()) {
            ci.cancel();
        }
    }
}
