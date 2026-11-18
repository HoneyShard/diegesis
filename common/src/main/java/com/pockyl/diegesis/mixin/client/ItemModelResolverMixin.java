package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.mesh.ItemShapes;

/**
 * Items whose look is a shape made from their data ({@code ItemShapes}): wherever the game builds an item's look (inventories,
 * frames, hands, shelves), the layers its JSON model added keep their display transforms and get the shape's quads.
 */
@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverMixin {
    /** How many layers the item's look had before its model added its own. */
    @Unique
    private int diegesis$layersBefore;

    @Inject(method = "appendItemLayers", at = @At("HEAD"))
    private void diegesis$before(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Level level,
            ItemOwner owner, int seed, CallbackInfo ci) {
        diegesis$layersBefore = ((ItemStackRenderStateAccessor) output).diegesis$layerCount();
    }

    @Inject(method = "appendItemLayers", at = @At("TAIL"))
    private void diegesis$shapedItem(ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext, Level level,
            ItemOwner owner, int seed, CallbackInfo ci) {
        ItemShapes.apply(item, (ItemStackRenderStateAccessor) output, diegesis$layersBefore);
    }
}
