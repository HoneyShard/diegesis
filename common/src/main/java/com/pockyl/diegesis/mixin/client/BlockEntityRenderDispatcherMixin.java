package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.render.ClearBlocks;

/**
 * A block left out of the picture for the camera at a station is left out whole: its chunk mesh goes, and so does what its block
 * entity draws (the food on a board, a chest's lid), which would otherwise hang in the air before the camera.
 */
@Mixin(BlockEntityRenderDispatcher.class)
abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void diegesis$clearBlockEntity(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            CallbackInfo callback) {
        if (ClearBlocks.hides(blockEntity.getBlockPos())) {
            callback.cancel();
        }
    }
}
