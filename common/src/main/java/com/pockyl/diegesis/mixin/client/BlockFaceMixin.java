package com.pockyl.diegesis.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.diegesis.impl.client.HiddenBlocks;

/**
 * A block a scene stands in for hides no face of its neighbours in the game's chunk meshes. Its shape still says it fills the
 * block, while the scene's copy may leave part of it open: a door on its way shut over a block whose state already says shut
 * showed the caves through the floor under it.
 */
@Mixin(Block.class)
abstract class BlockFaceMixin {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void diegesis$showFaceByHiddenBlock(BlockState state, BlockGetter level, BlockPos pos, Direction face,
                                                        BlockPos neighbour, CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.isHidden(neighbour)) {
            cir.setReturnValue(true);
        }
    }
}
