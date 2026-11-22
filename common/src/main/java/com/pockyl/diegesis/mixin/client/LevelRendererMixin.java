package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.focus.FocusController;

/**
 * Lets the game turn a scene's outline into a line. The scene draws the silhouette of what the cursor is over into the game's outline
 * target, and Minecraft 26.1 blits that target onto the frame as it is, a solid fill, unless the level has a glowing entity: only then
 * does the post chain that finds the edge run. That is decided while the frame is planned, before the scenes are submitted, so the
 * flag is set here, at the end of extraction, for as long as the player works at a station.
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Shadow
    @org.spongepowered.asm.mixin.Final
    private LevelRenderState levelRenderState;

    @Inject(method = "extractLevel", at = @At("TAIL"))
    private void diegesis$outlineChain(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        if (FocusController.get().isActive()) {
            levelRenderState.haveGlowingEntities = true;
        }
    }
}
