package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** {@code RenderType.create}, not public in plain Minecraft: the library's own render types are made with it. */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Invoker("create")
    static RenderType diegesis$create(String name, RenderSetup setup) {
        throw new AssertionError();
    }
}
