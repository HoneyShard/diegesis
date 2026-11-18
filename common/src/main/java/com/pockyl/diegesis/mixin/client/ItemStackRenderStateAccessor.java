package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An item's layers, for {@code ItemShapes} to give the layers of a shaped item its shape. */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int diegesis$layerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] diegesis$layers();
}
