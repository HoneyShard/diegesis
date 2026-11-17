package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.render.ClearView;

/**
 * Puts back the last particle faded in the clear view once the particles are drawn, and before they tick: a renderer that draws
 * the particle itself and cancels the vanilla method skips anything after it.
 * <p>
 * AsyncParticles replaces render and tick with an {@code @Overwrite}. Mixin lets an injector reach a method another mixin
 * merged only from a higher priority, so this one runs above it and lands in AsyncParticles' versions. {@code require = 0}: a
 * later change there cannot stop the game from starting, and the next {@link ClearView#particleFade} call still puts the faded
 * particle back.
 */
@Mixin(value = ParticleEngine.class, priority = 1100)
abstract class ParticleEngineMixin {
    // Every overload: NeoForge draws through its own, which takes the frustum and a filter of render types. The vanilla one is
    // named in full as well: a wildcard skips a method another mixin merged, such as AsyncParticles' @Overwrite.
    @Inject(method = {"render*", "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"},
            at = @At("RETURN"), require = 0)
    private void diegesis$restoreDrawn(CallbackInfo info) {
        ClearView.restoreParticle();
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void diegesis$restoreBeforeTick(CallbackInfo info) {
        ClearView.restoreParticle();
    }
}
